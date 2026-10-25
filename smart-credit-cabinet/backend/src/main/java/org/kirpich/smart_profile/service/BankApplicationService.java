package org.kirpich.smart_profile.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.kirpich.smart_profile.config.JsonCodec;
import org.kirpich.smart_profile.domain.Actor;
import org.kirpich.smart_profile.domain.ApplicationStatus;
import org.kirpich.smart_profile.domain.Finance;
import org.kirpich.smart_profile.domain.decision.DecisionEngine.OfferKind;
import org.kirpich.smart_profile.domain.decision.DecisionEngine.OfferProposal;
import org.kirpich.smart_profile.domain.decision.DecisionEngine.Outcome;
import org.kirpich.smart_profile.domain.policy.CreditPolicy;
import org.kirpich.smart_profile.domain.policy.Product;
import org.kirpich.smart_profile.model.AppUser;
import org.kirpich.smart_profile.model.CreditApplication;
import org.kirpich.smart_profile.model.Decision;
import org.kirpich.smart_profile.model.InfoRequest;
import org.kirpich.smart_profile.repository.CreditApplicationRepository;
import org.kirpich.smart_profile.repository.DecisionRepository;
import org.kirpich.smart_profile.web.dto.BankDtos.BankDecisionRequest;
import org.kirpich.smart_profile.web.dto.BankDtos.BankInfoRequest;
import org.kirpich.smart_profile.web.dto.BankDtos.BankOutcome;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Действия кредитного специалиста: очередь, взять в работу, решение, запрос информации у клиента. */
@Service
@Transactional
public class BankApplicationService {

    private final CreditApplicationRepository applications;
    private final DecisionRepository decisions;
    private final ApplicationWorkflow workflow;
    private final CreditPolicy policy;
    private final JsonCodec json;

    public BankApplicationService(
            CreditApplicationRepository applications,
            DecisionRepository decisions,
            ApplicationWorkflow workflow,
            CreditPolicy policy,
            JsonCodec json) {
        this.applications = applications;
        this.decisions = decisions;
        this.workflow = workflow;
        this.policy = policy;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<CreditApplication> list(ApplicationStatus status) {
        Set<ApplicationStatus> statuses = status != null
                ? EnumSet.of(status)
                : EnumSet.complementOf(EnumSet.of(ApplicationStatus.DRAFT));
        return applications.findByStatusInOrderByUpdatedAtDesc(statuses);
    }

    @Transactional(readOnly = true)
    public CreditApplication get(Long id) {
        CreditApplication app = applications.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
        if (app.getStatus() == ApplicationStatus.DRAFT) {
            throw new NotFoundException("Заявка не найдена");
        }
        return app;
    }

    public CreditApplication assign(AppUser user, Long id) {
        CreditApplication app = get(id);
        app.setAssignee(user);
        workflow.log(app, "assigned", "Заявку взял в работу: " + user.getFullName(), Actor.UNDERWRITER, user.getId());
        return app;
    }

    public CreditApplication decide(AppUser user, Long id, BankDecisionRequest request) {
        CreditApplication app = get(id);
        if (app.getStatus() != ApplicationStatus.MANUAL_REVIEW) {
            throw new ValidationFailedException("Решение принимается только по заявке на ручной проверке");
        }
        Decision decision = new Decision();
        decision.setApplication(app);
        decision.setSource(Decision.UNDERWRITER);
        decision.setDecidedBy(user.getId());
        decision.setNote(request.note());
        decision.setPolicyVersion(policy.version());

        if (request.outcome() == BankOutcome.DECLINE) {
            if (request.reason() == null || request.reason().isBlank()) {
                throw new ValidationFailedException("Укажите причину отказа");
            }
            decision.setOutcome(Outcome.DECLINE);
            decision.setReasons(json.write(List.of(request.reason())));
            app.getDecisions().add(decision);
            decisions.save(decision);
            workflow.transition(app, ApplicationStatus.DECLINED, Actor.UNDERWRITER, user.getId(),
                    "Кредитный специалист отказал: " + request.reason());
            return app;
        }

        OfferProposal recommended = workflow.latestDecision(app, Decision.ENGINE)
                .map(d -> json.read(d.getProposal(), OfferProposal.class))
                .orElse(null);
        OfferProposal proposal = request.outcome() == BankOutcome.APPROVE
                ? asRequested(app, request, recommended)
                : counter(app, request, recommended);

        decision.setOutcome(request.outcome() == BankOutcome.APPROVE ? Outcome.APPROVE : Outcome.COUNTER_OFFER);
        decision.setProposal(json.write(proposal));
        decision.setReasons(json.write(request.note() == null || request.note().isBlank() ? List.of() : List.of(request.note())));
        app.getDecisions().add(decision);
        decisions.save(decision);
        workflow.createOffer(app, decision, proposal);
        workflow.transition(app, ApplicationStatus.OFFERED, Actor.UNDERWRITER, user.getId(),
                proposal.kind() == OfferKind.AS_REQUESTED
                        ? "Кредитный специалист одобрил заявку"
                        : "Кредитный специалист одобрил заявку на других условиях");
        return app;
    }

    public CreditApplication requestInfo(AppUser user, Long id, BankInfoRequest request) {
        CreditApplication app = get(id);
        if (app.getStatus() != ApplicationStatus.MANUAL_REVIEW) {
            throw new ValidationFailedException("Запросить информацию можно только по заявке на ручной проверке");
        }
        // Уникальный префикс: документы по разным запросам не перепутаются
        String prefix = "q" + UUID.randomUUID().toString().substring(0, 8);
        List<InfoItem> items = IntStream.range(0, request.items().size())
                .mapToObj(i -> new InfoItem(prefix + "-" + (i + 1), request.items().get(i).label(), request.items().get(i).type()))
                .toList();
        InfoRequest infoRequest = new InfoRequest();
        infoRequest.setApplication(app);
        infoRequest.setRequestedBy(Actor.UNDERWRITER);
        infoRequest.setRequestedByUser(user.getId());
        infoRequest.setReturnStatus(ApplicationStatus.MANUAL_REVIEW);
        infoRequest.setItems(json.write(items));
        infoRequest.setNote(request.note());
        app.getInfoRequests().add(infoRequest);
        workflow.transition(app, ApplicationStatus.NEEDS_INFO, Actor.UNDERWRITER, user.getId(),
                "Банк запросил дополнительную информацию");
        return app;
    }

    private OfferProposal asRequested(CreditApplication app, BankDecisionRequest request, OfferProposal recommended) {
        Product product = policy.product(app.getProductCode());
        double rate = request.rate() != null ? request.rate()
                : recommended != null && recommended.productCode().equals(product.code()) ? recommended.rate()
                : product.baseRate();
        return OfferProposal.of(product, app.getAmount(), app.getTermMonths(), rate, OfferKind.AS_REQUESTED);
    }

    private OfferProposal counter(CreditApplication app, BankDecisionRequest request, OfferProposal recommended) {
        String code = request.productCode() != null ? request.productCode()
                : recommended != null ? recommended.productCode() : app.getProductCode();
        Product product = policy.findProduct(code).orElseThrow(() -> new ValidationFailedException("Неизвестный продукт: " + code));
        long amount = request.amount() != null ? request.amount() : recommended != null ? recommended.amount() : app.getAmount();
        int term = request.termMonths() != null ? request.termMonths()
                : recommended != null ? recommended.termMonths() : app.getTermMonths();
        double rate = request.rate() != null ? request.rate() : recommended != null ? recommended.rate() : product.baseRate();

        List<String> errors = new ArrayList<>();
        if (amount < product.minAmount() || amount > product.maxAmount()) {
            errors.add("Сумма для «%s» — от %s до %s".formatted(
                    product.name(), Finance.formatRub(product.minAmount()), Finance.formatRub(product.maxAmount())));
        }
        if (term < product.minTerm() || term > product.maxTerm()) {
            errors.add("Срок для «%s» — от %d до %d мес.".formatted(product.name(), product.minTerm(), product.maxTerm()));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return OfferProposal.of(product, amount, term, rate, OfferKind.COUNTER);
    }
}
