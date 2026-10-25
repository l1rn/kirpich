package org.kirpich.smart_profile.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.kirpich.smart_profile.config.JsonCodec;
import org.kirpich.smart_profile.domain.Actor;
import org.kirpich.smart_profile.domain.ApplicationStatus;
import org.kirpich.smart_profile.domain.CheckStatus;
import org.kirpich.smart_profile.domain.PipelineStep;
import org.kirpich.smart_profile.domain.decision.DecisionEngine.OfferProposal;
import org.kirpich.smart_profile.model.ApplicationCheck;
import org.kirpich.smart_profile.model.ApplicationEvent;
import org.kirpich.smart_profile.model.CreditApplication;
import org.kirpich.smart_profile.model.Decision;
import org.kirpich.smart_profile.model.InfoRequest;
import org.kirpich.smart_profile.model.Offer;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import tools.jackson.core.type.TypeReference;

/** Общие операции над заявкой. Любая смена статуса идёт только через {@link #transition}. */
@Component
public class ApplicationWorkflow {

    private static final TypeReference<List<InfoItem>> INFO_ITEMS = new TypeReference<>() {
    };

    private final EntityManager em;
    private final JsonCodec json;

    public ApplicationWorkflow(EntityManager em, JsonCodec json) {
        this.em = em;
        this.json = json;
    }

    /** Подсказка «что дальше» — одна и та же для кабинета клиента и кабинета банка. */
    public record NextAction(String actor, String title, String description) {
    }

    public void transition(CreditApplication app, ApplicationStatus target, Actor actor, Long actorId, String message) {
        app.getStatus().ensureCanMoveTo(target);
        ApplicationEvent event = log(app, "status_changed", message != null ? message : target.label(), actor, actorId);
        event.setFromStatus(app.getStatus());
        event.setToStatus(target);
        app.setStatus(target);
        if (target == ApplicationStatus.OFFERED || target == ApplicationStatus.DECLINED) {
            app.setDecidedAt(Instant.now());
        }
    }

    public ApplicationEvent log(CreditApplication app, String type, String message, Actor actor, Long actorId) {
        ApplicationEvent event = new ApplicationEvent();
        event.setApplication(app);
        event.setEventType(type);
        event.setMessage(message);
        event.setActorType(actor);
        event.setActorId(actorId);
        app.getEvents().add(event);
        return event;
    }

    /** Запускает конвейер заново: все шаги в PENDING, первый — сразу RUNNING. */
    public void restartChecks(CreditApplication app) {
        app.getChecks().clear();
        // Hibernate вставляет строки раньше, чем удаляет: без flush упрёмся в unique (application_id, step)
        em.flush();
        for (PipelineStep step : PipelineStep.values()) {
            CheckStatus status = step == PipelineStep.VALIDATION ? CheckStatus.RUNNING : CheckStatus.PENDING;
            app.getChecks().add(new ApplicationCheck(app, step, status));
        }
    }

    public Offer createOffer(CreditApplication app, Decision decision, OfferProposal proposal) {
        Offer offer = new Offer();
        offer.setApplication(app);
        offer.setDecisionId(decision.getId());
        offer.setKind(proposal.kind());
        offer.setProductCode(proposal.productCode());
        offer.setAmount(proposal.amount());
        offer.setTermMonths(proposal.termMonths());
        offer.setRate(proposal.rate());
        offer.setMonthlyPayment(proposal.monthlyPayment());
        app.getOffers().add(offer);
        return offer;
    }

    public Optional<Offer> currentOffer(CreditApplication app) {
        return app.getOffers().reversed().stream().filter(o -> o.getStatus() != Offer.Status.REJECTED).findFirst();
    }

    public Optional<InfoRequest> openInfoRequest(CreditApplication app) {
        return app.getInfoRequests().stream().filter(r -> r.getStatus() == InfoRequest.Status.OPEN).findFirst();
    }

    public Optional<Decision> latestDecision(CreditApplication app, String source) {
        return app.getDecisions().reversed().stream()
                .filter(d -> source == null || source.equals(d.getSource()))
                .findFirst();
    }

    public List<InfoItem> items(InfoRequest request) {
        return json.read(request.getItems(), INFO_ITEMS);
    }

    public NextAction nextAction(CreditApplication app) {
        return switch (app.getStatus()) {
            case DRAFT -> new NextAction("CLIENT", "Заполните и отправьте заявку", "Черновик сохраняется автоматически.");
            case CHECKING -> new NextAction("BANK", "Проверяем заявку",
                    "Сверяем данные с ЕГРЮЛ, ФНС и бюро кредитных историй. Обычно это занимает меньше минуты.");
            case NEEDS_INFO -> new NextAction("CLIENT", "Ответьте на запрос банка",
                    "Без этих сведений рассмотрение не продолжится.");
            case MANUAL_REVIEW -> new NextAction("BANK", "Заявку изучает кредитный специалист",
                    "Если понадобятся документы, запрос появится на этой странице.");
            case OFFERED -> currentOffer(app).filter(o -> o.getStatus() == Offer.Status.ACCEPTED).isPresent()
                    ? new NextAction("CLIENT", "Подпишите договор", "Подписать может только руководитель компании — кодом подтверждения.")
                    : new NextAction("CLIENT", "Изучите предложение банка",
                            "Примите его, чтобы перейти к подписанию договора, или откажитесь.");
            case DECLINED -> new NextAction("NONE", "Банк не может одобрить заявку",
                    "Причины указаны ниже. Можно подать новую заявку на другую сумму или другой продукт.");
            case ISSUED -> new NextAction("NONE", "Деньги зачислены на расчётный счёт", "Кредит оформлен.");
            case CANCELLED -> new NextAction("NONE", "Заявка отозвана", "Можно подать новую заявку.");
        };
    }
}
