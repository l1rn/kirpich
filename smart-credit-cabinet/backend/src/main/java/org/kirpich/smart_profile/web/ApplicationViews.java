package org.kirpich.smart_profile.web;

import java.util.List;
import java.util.Map;

import org.kirpich.smart_profile.config.JsonCodec;
import org.kirpich.smart_profile.domain.ApplicationStatus;
import org.kirpich.smart_profile.domain.decision.DecisionEngine.OfferProposal;
import org.kirpich.smart_profile.domain.policy.CreditPolicy;
import org.kirpich.smart_profile.domain.policy.Product;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine.FactorResult;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine.StopFactor;
import org.kirpich.smart_profile.model.AppUser;
import org.kirpich.smart_profile.model.ApplicationCheck;
import org.kirpich.smart_profile.model.ApplicationEvent;
import org.kirpich.smart_profile.model.Company;
import org.kirpich.smart_profile.model.CreditApplication;
import org.kirpich.smart_profile.model.Decision;
import org.kirpich.smart_profile.model.Document;
import org.kirpich.smart_profile.model.InfoRequest;
import org.kirpich.smart_profile.model.Offer;
import org.kirpich.smart_profile.service.ApplicationWorkflow;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.CheckView;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.ClientDecisionView;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.DetailView;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.DocumentView;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.EventView;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.InfoRequestView;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.OfferView;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.SummaryView;
import org.kirpich.smart_profile.web.dto.AuthDtos.CompanyView;
import org.kirpich.smart_profile.web.dto.AuthDtos.UserView;
import org.kirpich.smart_profile.web.dto.BankDtos.BankDetailView;
import org.kirpich.smart_profile.web.dto.BankDtos.DecisionView;
import org.kirpich.smart_profile.web.dto.BankDtos.RowView;
import org.kirpich.smart_profile.web.dto.BankDtos.SourceView;
import org.springframework.stereotype.Component;

import tools.jackson.core.type.TypeReference;

/** Сборка ответов API из сущностей. Клиент видит объяснение решения, банк — ещё и сырые данные источников. */
@Component
public class ApplicationViews {

    private static final TypeReference<List<FactorResult>> FACTORS = new TypeReference<>() {
    };
    private static final TypeReference<List<StopFactor>> STOP_FACTORS = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> ANSWERS = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> RAW = new TypeReference<>() {
    };

    private final CreditPolicy policy;
    private final ApplicationWorkflow workflow;
    private final JsonCodec json;

    public ApplicationViews(CreditPolicy policy, ApplicationWorkflow workflow, JsonCodec json) {
        this.policy = policy;
        this.workflow = workflow;
        this.json = json;
    }

    public UserView user(AppUser user) {
        return new UserView(user.getId(), user.getLogin(), user.getFullName(), user.getRole().name(),
                user.getCompany() == null ? null : company(user.getCompany()));
    }

    public CompanyView company(Company c) {
        return new CompanyView(c.getId(), c.getInn(), c.getName(), c.getLegalForm(), c.getOgrn(), c.getOkved(),
                c.getDirectorName(), c.getRegisteredAt());
    }

    public SummaryView summary(CreditApplication app) {
        return new SummaryView(
                app.getId(),
                app.getNumber(),
                app.getStatus().name(),
                app.getStatus().label(),
                app.getProductCode(),
                productName(app.getProductCode()),
                app.getPurpose(),
                policy.purposeLabel(app.getPurpose()).orElse(app.getPurpose()),
                app.getAmount(),
                app.getTermMonths(),
                app.getCreatedAt(),
                app.getUpdatedAt(),
                workflow.nextAction(app));
    }

    public DetailView detail(CreditApplication app) {
        return new DetailView(
                summary(app),
                app.getStatus().stageIndex(),
                ApplicationStatus.STAGES,
                app.getRevenue12m(),
                app.getExistingMonthlyPayments(),
                app.getBkiConsentAt(),
                app.getSubmittedAt(),
                app.getDecidedAt(),
                app.getChecks().stream().map(this::check).toList(),
                workflow.currentOffer(app).map(this::offer).orElse(null),
                workflow.openInfoRequest(app).map(this::infoRequest).orElse(null),
                app.getDocuments().stream().map(this::document).toList(),
                app.getEvents().stream().map(this::event).toList(),
                clientDecision(app));
    }

    public RowView row(CreditApplication app) {
        Decision engine = workflow.latestDecision(app, Decision.ENGINE).orElse(null);
        return new RowView(
                app.getId(),
                app.getNumber(),
                app.getStatus().name(),
                app.getStatus().label(),
                app.getCompany().getName(),
                app.getCompany().getInn(),
                app.getProductCode(),
                productName(app.getProductCode()),
                app.getAmount(),
                app.getTermMonths(),
                engine == null ? null : engine.getScore(),
                engine == null ? null : engine.getGrade(),
                app.getAssignee() == null ? null : app.getAssignee().getFullName(),
                app.getUpdatedAt());
    }

    public BankDetailView bankDetail(CreditApplication app) {
        return new BankDetailView(
                detail(app),
                company(app.getCompany()),
                app.getAssignee() == null ? null : app.getAssignee().getFullName(),
                app.getChecks().stream().map(this::source).toList(),
                app.getDecisions().stream().map(this::decision).toList(),
                app.getInfoRequests().stream().map(this::infoRequest).toList(),
                app.getVersion());
    }

    private ClientDecisionView clientDecision(CreditApplication app) {
        boolean decided = app.getStatus() == ApplicationStatus.OFFERED
                || app.getStatus() == ApplicationStatus.DECLINED
                || app.getStatus() == ApplicationStatus.ISSUED;
        if (!decided) {
            return null;
        }
        Decision last = workflow.latestDecision(app, null).orElse(null);
        if (last == null) {
            return null;
        }
        // Факторы скоринга считает движок; если решал специалист, показываем их из решения движка
        Decision scored = workflow.latestDecision(app, Decision.ENGINE).orElse(last);
        return new ClientDecisionView(
                last.getOutcome().name(),
                json.read(last.getReasons(), STRINGS),
                scored.getScore(),
                scored.getGrade(),
                scored.getFactors() == null ? List.of() : json.read(scored.getFactors(), FACTORS));
    }

    private CheckView check(ApplicationCheck c) {
        return new CheckView(c.getStep().name(), c.getStep().label(), c.getStatus().name(), c.getAttempts(), c.getError(), c.getUpdatedAt());
    }

    private SourceView source(ApplicationCheck c) {
        Object data = c.getResult() == null ? null : json.read(c.getResult(), RAW);
        return new SourceView(c.getStep().name(), c.getStep().label(), c.getStatus().name(), c.getAttempts(), c.getError(), data);
    }

    private OfferView offer(Offer o) {
        return new OfferView(o.getId(), o.getKind().name(), o.getProductCode(), productName(o.getProductCode()), o.getAmount(),
                o.getTermMonths(), o.getRate(), o.getMonthlyPayment(), o.getStatus().name(), o.getCreatedAt(),
                o.getAcceptedAt(), o.getSignedAt());
    }

    private InfoRequestView infoRequest(InfoRequest r) {
        return new InfoRequestView(r.getId(), r.getRequestedBy().name(), workflow.items(r), r.getNote(), r.getStatus().name(),
                r.getResponse() == null ? null : json.read(r.getResponse(), ANSWERS), r.getCreatedAt(), r.getAnsweredAt());
    }

    private DocumentView document(Document d) {
        return new DocumentView(d.getId(), d.getDocType(), policy.documentLabel(d.getDocType()).orElse("Документ по запросу банка"),
                d.getFilename(), d.getSizeBytes(), d.getCreatedAt());
    }

    private EventView event(ApplicationEvent e) {
        return new EventView(e.getId(), e.getEventType(),
                e.getFromStatus() == null ? null : e.getFromStatus().name(),
                e.getToStatus() == null ? null : e.getToStatus().name(),
                e.getActorType().name(), e.getMessage(), e.getCreatedAt());
    }

    private DecisionView decision(Decision d) {
        return new DecisionView(
                d.getId(),
                d.getSource(),
                d.getOutcome().name(),
                d.getScore(),
                d.getGrade(),
                d.getFactors() == null ? List.of() : json.read(d.getFactors(), FACTORS),
                d.getStopFactors() == null ? List.of() : json.read(d.getStopFactors(), STOP_FACTORS),
                d.getReasons() == null ? List.of() : json.read(d.getReasons(), STRINGS),
                json.read(d.getProposal(), OfferProposal.class),
                d.getNote(),
                d.getPolicyVersion(),
                d.getCreatedAt());
    }

    private String productName(String code) {
        return policy.findProduct(code).map(Product::name).orElse(code);
    }
}
