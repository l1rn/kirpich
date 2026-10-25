package org.kirpich.smart_profile.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.kirpich.smart_profile.domain.Finance;
import org.kirpich.smart_profile.domain.policy.CreditPolicy;
import org.kirpich.smart_profile.domain.policy.Product;
import org.kirpich.smart_profile.web.dto.CatalogDtos.CatalogView;
import org.kirpich.smart_profile.web.dto.CatalogDtos.RecommendationRequest;
import org.kirpich.smart_profile.web.dto.CatalogDtos.RecommendationView;
import org.springframework.stereotype.Service;

/**
 * Подбор продукта от задачи клиента, а не от названия продукта: клиент без банковской экспертизы
 * думает «нужны деньги на закупку», а не «нужна кредитная линия».
 */
@Service
public class CatalogService {

    /** Типичный срок для оценки платежа в подборе. */
    private static final int TYPICAL_TERM_MONTHS = 24;

    private final CreditPolicy policy;

    public CatalogService(CreditPolicy policy) {
        this.policy = policy;
    }

    public CatalogView catalog() {
        return new CatalogView(policy.products(), policy.purposes(), policy.documents());
    }

    public List<RecommendationView> recommend(RecommendationRequest request) {
        String purposeLabel = policy.purposeLabel(request.purpose())
                .orElseThrow(() -> new ValidationFailedException("Неизвестная цель кредита: " + request.purpose()));
        List<RecommendationView> result = new ArrayList<>();
        for (Product product : policy.products()) {
            List<String> reasons = new ArrayList<>();
            boolean suitable = true;
            if (product.purposes().contains(request.purpose())) {
                reasons.add("Подходит для цели «%s»".formatted(purposeLabel));
            } else {
                suitable = false;
                reasons.add("Не предназначен для этой цели");
            }
            if (request.amount() < product.minAmount()) {
                suitable = false;
                reasons.add("Минимальная сумма — " + Finance.formatRub(product.minAmount()));
            } else if (request.amount() > product.maxAmount()) {
                suitable = false;
                reasons.add("Максимальная сумма — " + Finance.formatRub(product.maxAmount()));
            }
            if (suitable) {
                reasons.add(request.amount() <= product.autoApproveMax()
                        ? "Решение автоматически, за несколько минут"
                        : "Для такой суммы решение принимает кредитный специалист — это дольше");
                List<String> docs = product.documentsNeeded(request.amount());
                if (!docs.isEmpty()) {
                    reasons.add("Понадобятся документы: " + String.join(", ",
                            docs.stream().map(d -> policy.documentLabel(d).orElse(d)).toList()));
                }
            }
            int term = product.clampTerm(TYPICAL_TERM_MONTHS);
            Long payment = suitable ? Finance.annuityPayment(request.amount(), product.baseRate(), term) : null;
            result.add(new RecommendationView(product, suitable, reasons, payment));
        }
        Comparator<RecommendationView> order = Comparator.comparing((RecommendationView r) -> !r.suitable());
        if (request.urgent()) {
            order = order.thenComparing(r -> request.amount() > r.product().autoApproveMax());
        }
        order = order.thenComparingDouble(r -> r.product().baseRate());
        return result.stream().sorted(order).toList();
    }
}
