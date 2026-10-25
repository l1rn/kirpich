package org.kirpich.smart_profile.domain.sources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Кредитный отчёт бюро кредитных историй (контракт упрощён). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BkiReport(String inn, String reportId, String generatedAt, List<Loan> loans, int inquiries30d) {

    public BkiReport {
        loans = loans == null ? List.of() : List.copyOf(loans);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Loan(
            String creditor,
            String type,
            long amount,
            String openedAt,
            int termMonths,
            /** active или closed. */
            String status,
            long outstanding,
            long monthlyPayment,
            int maxOverdueDays12m,
            int currentOverdueDays) {

        public boolean isActive() {
            return "active".equals(status);
        }
    }
}
