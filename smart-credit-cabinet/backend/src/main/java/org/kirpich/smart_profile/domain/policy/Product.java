package org.kirpich.smart_profile.domain.policy;

import java.util.List;

public record Product(
        String code,
        String name,
        String description,
        List<String> purposes,
        long minAmount,
        long maxAmount,
        int minTerm,
        int maxTerm,
        double baseRate,
        /** Лимит продукта как доля годовой выручки. */
        double revenueRatio,
        /** Выше этой суммы решение принимает только кредитный специалист. */
        long autoApproveMax,
        List<String> requiredDocuments,
        /** Документы обязательны только для сумм выше порога; null — всегда. */
        Long documentsRequiredAbove) {

    public Product {
        purposes = List.copyOf(purposes);
        requiredDocuments = requiredDocuments == null ? List.of() : List.copyOf(requiredDocuments);
    }

    public List<String> documentsNeeded(long amount) {
        if (documentsRequiredAbove == null || amount > documentsRequiredAbove) {
            return requiredDocuments;
        }
        return List.of();
    }

    public int clampTerm(int termMonths) {
        return Math.min(Math.max(termMonths, minTerm), maxTerm);
    }
}
