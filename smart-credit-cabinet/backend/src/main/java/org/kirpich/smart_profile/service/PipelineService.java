package org.kirpich.smart_profile.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import org.kirpich.smart_profile.config.AppProperties;
import org.kirpich.smart_profile.config.JsonCodec;
import org.kirpich.smart_profile.domain.Actor;
import org.kirpich.smart_profile.domain.ApplicationStatus;
import org.kirpich.smart_profile.domain.CheckStatus;
import org.kirpich.smart_profile.domain.Finance;
import org.kirpich.smart_profile.domain.PipelineStep;
import org.kirpich.smart_profile.domain.decision.DecisionEngine;
import org.kirpich.smart_profile.domain.decision.DecisionEngine.DecisionResult;
import org.kirpich.smart_profile.domain.policy.CreditPolicy;
import org.kirpich.smart_profile.domain.policy.Product;
import org.kirpich.smart_profile.domain.scoring.BorrowerProfile;
import org.kirpich.smart_profile.domain.scoring.ProfileBuilder;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine.ScoreResult;
import org.kirpich.smart_profile.domain.sources.BkiReport;
import org.kirpich.smart_profile.domain.sources.EgrulCompany;
import org.kirpich.smart_profile.domain.sources.FnsBusiness;
import org.kirpich.smart_profile.integration.RegistryGateway;
import org.kirpich.smart_profile.integration.SourceNotFoundException;
import org.kirpich.smart_profile.integration.SourceUnavailableException;
import org.kirpich.smart_profile.model.ApplicationCheck;
import org.kirpich.smart_profile.model.CreditApplication;
import org.kirpich.smart_profile.model.Decision;
import org.kirpich.smart_profile.model.InfoRequest;
import org.kirpich.smart_profile.repository.CreditApplicationRepository;
import org.kirpich.smart_profile.repository.DecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Конвейер обработки заявки. Один тик продвигает каждую заявку в статусе CHECKING на один шаг:
 * проверка заявки → ЕГРЮЛ → ФНС → БКИ → скоринг и решение. Каждая заявка — в своей транзакции.
 */
@Service
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);
    private static final int BATCH_SIZE = 20;

    private final CreditApplicationRepository applications;
    private final DecisionRepository decisions;
    private final ApplicationWorkflow workflow;
    private final RegistryGateway registry;
    private final CreditPolicy policy;
    private final JsonCodec json;
    private final AppProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;

    public PipelineService(
            CreditApplicationRepository applications,
            DecisionRepository decisions,
            ApplicationWorkflow workflow,
            RegistryGateway registry,
            CreditPolicy policy,
            JsonCodec json,
            AppProperties props,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.applications = applications;
        this.decisions = decisions;
        this.workflow = workflow;
        this.registry = registry;
        this.policy = policy;
        this.json = json;
        this.props = props;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** @return сколько заявок продвинулось */
    public int processTick() {
        List<Long> ids = applications.findIdsByStatus(ApplicationStatus.CHECKING, PageRequest.of(0, BATCH_SIZE));
        int processed = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(tx.execute(status -> advance(id)))) {
                    processed++;
                }
            } catch (RuntimeException e) {
                log.error("Сбой конвейера на заявке {}", id, e);
            }
        }
        return processed;
    }

    private boolean advance(Long id) {
        CreditApplication app = applications.findById(id).orElse(null);
        if (app == null || app.getStatus() != ApplicationStatus.CHECKING) {
            return false;
        }
        ApplicationCheck check = app.getChecks().stream()
                .filter(c -> c.getStatus() == CheckStatus.PENDING || c.getStatus() == CheckStatus.RUNNING)
                .findFirst()
                .orElse(null);
        if (check == null) {
            return false;
        }
        String inn = app.getCompany().getInn();
        switch (check.getStep()) {
            case VALIDATION -> validate(app, check);
            case EGRUL -> external(app, check, () -> registry.egrulCompany(inn));
            case FNS -> external(app, check, () -> registry.fnsBusiness(inn));
            case BKI -> external(app, check, () -> registry.bkiReport(inn, "consent-" + app.getId()));
            case DECISION -> decide(app, check);
        }
        return true;
    }

    /** Проверка полноты: для крупных сумм продукт требует документы — без них просим клиента дозагрузить. */
    private void validate(CreditApplication app, ApplicationCheck check) {
        check.setAttempts(check.getAttempts() + 1);
        Product product = policy.product(app.getProductCode());
        List<String> uploaded = app.getDocuments().stream().map(d -> d.getDocType()).toList();
        List<String> missing = product.documentsNeeded(app.getAmount()).stream().filter(d -> !uploaded.contains(d)).toList();
        check.setStatus(CheckStatus.OK);
        check.setResult(json.write(new ValidationResult(missing)));
        if (missing.isEmpty()) {
            startNext(app, check);
            return;
        }
        List<InfoItem> items = missing.stream()
                .map(code -> new InfoItem(code, policy.documentLabel(code).orElse(code), InfoItem.FILE))
                .toList();
        InfoRequest request = new InfoRequest();
        request.setApplication(app);
        request.setRequestedBy(Actor.SYSTEM);
        request.setReturnStatus(ApplicationStatus.CHECKING);
        request.setItems(json.write(items));
        request.setNote("Для суммы больше %s нужны документы".formatted(Finance.formatRub(product.documentsRequiredAbove())));
        app.getInfoRequests().add(request);
        String labels = String.join(", ", items.stream().map(InfoItem::label).toList());
        workflow.transition(app, ApplicationStatus.NEEDS_INFO, Actor.SYSTEM, null, "Нужны документы: " + labels);
    }

    record ValidationResult(List<String> missingDocuments) {
    }

    private void external(CreditApplication app, ApplicationCheck check, Supplier<Object> call) {
        check.setAttempts(check.getAttempts() + 1);
        String label = check.getStep().label();
        try {
            check.setResult(json.write(call.get()));
            check.setStatus(CheckStatus.OK);
            check.setError(null);
        } catch (SourceNotFoundException e) {
            check.setStatus(CheckStatus.FAILED);
            check.setError("Сведения не найдены");
            workflow.log(app, "check_failed", label + ": сведения не найдены", Actor.SYSTEM, null);
        } catch (SourceUnavailableException e) {
            check.setError(e.getMessage());
            int maxAttempts = props.registry().maxAttempts();
            if (check.getAttempts() < maxAttempts) {
                workflow.log(app, "check_retry", "%s: нет ответа (%s), повтор %d из %d"
                        .formatted(label, e.getMessage(), check.getAttempts() + 1, maxAttempts), Actor.SYSTEM, null);
                return;
            }
            check.setStatus(CheckStatus.FAILED);
            workflow.log(app, "check_failed", "%s: нет ответа после %d попыток — заявку проверит специалист"
                    .formatted(label, maxAttempts), Actor.SYSTEM, null);
        }
        startNext(app, check);
    }

    private void decide(CreditApplication app, ApplicationCheck check) {
        check.setAttempts(check.getAttempts() + 1);
        BorrowerProfile profile = ProfileBuilder.build(
                app.getRevenue12m() == null ? 0 : app.getRevenue12m(),
                app.getExistingMonthlyPayments(),
                resultOf(app, PipelineStep.EGRUL, EgrulCompany.class),
                resultOf(app, PipelineStep.FNS, FnsBusiness.class),
                resultOf(app, PipelineStep.BKI, BkiReport.class),
                LocalDate.now(clock));
        Product product = policy.product(app.getProductCode());
        ScoreResult score = ScoringEngine.score(profile, app.getAmount(), app.getTermMonths(), product, policy);
        DecisionResult result = DecisionEngine.decide(
                product, app.getAmount(), app.getTermMonths(), app.getPurpose(), profile, score, policy);

        Decision decision = new Decision();
        decision.setApplication(app);
        decision.setSource(Decision.ENGINE);
        decision.setOutcome(result.outcome());
        decision.setScore(score.score());
        decision.setGrade(score.grade());
        decision.setFactors(json.write(score.factors()));
        decision.setStopFactors(json.write(score.stopFactors()));
        decision.setReasons(json.write(result.reasons()));
        decision.setProposal(json.write(result.proposal()));
        decision.setPolicyVersion(policy.version());
        app.getDecisions().add(decision);
        decisions.save(decision);

        check.setStatus(CheckStatus.OK);
        check.setResult(json.write(new DecisionSummary(score.score(), score.grade(), result.outcome().name())));

        switch (result.outcome()) {
            case APPROVE, COUNTER_OFFER -> {
                workflow.createOffer(app, decision, result.proposal());
                String message = result.outcome() == DecisionEngine.Outcome.APPROVE
                        ? "Заявка одобрена"
                        : "Заявка одобрена на других условиях";
                workflow.transition(app, ApplicationStatus.OFFERED, Actor.SYSTEM, null, message);
            }
            case MANUAL_REVIEW -> workflow.transition(app, ApplicationStatus.MANUAL_REVIEW, Actor.SYSTEM, null,
                    "Заявку передали кредитному специалисту");
            case DECLINE -> workflow.transition(app, ApplicationStatus.DECLINED, Actor.SYSTEM, null,
                    "Банк не может одобрить заявку");
        }
    }

    record DecisionSummary(int score, String grade, String outcome) {
    }

    private <T> T resultOf(CreditApplication app, PipelineStep step, Class<T> type) {
        return app.getChecks().stream()
                .filter(c -> c.getStep() == step && c.getStatus() == CheckStatus.OK)
                .findFirst()
                .map(c -> json.read(c.getResult(), type))
                .orElse(null);
    }

    private static void startNext(CreditApplication app, ApplicationCheck done) {
        app.getChecks().stream()
                .filter(c -> c.getStepOrder() > done.getStepOrder() && c.getStatus() == CheckStatus.PENDING)
                .findFirst()
                .ifPresent(c -> c.setStatus(CheckStatus.RUNNING));
    }
}
