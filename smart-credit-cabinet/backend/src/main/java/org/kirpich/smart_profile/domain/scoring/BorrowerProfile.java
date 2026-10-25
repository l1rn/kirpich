package org.kirpich.smart_profile.domain.scoring;

import java.util.List;

/** Профиль заёмщика: анкета клиента, сведённая с ответами ЕГРЮЛ, ФНС и БКИ. Null — источник не ответил. */
public record BorrowerProfile(
        long declaredRevenue12m,
        long existingMonthlyPayments,
        String companyStatus,
        boolean directorDisqualified,
        Integer businessAgeMonths,
        Long reportedRevenue12m,
        String creditHistory,
        int currentOverdueDays,
        Boolean taxDebt,
        Boolean massRegistration,
        List<Source> missingSources) {

    public enum Source {
        EGRUL("ЕГРЮЛ/ЕГРИП"),
        FNS("ФНС"),
        BKI("БКИ");

        private final String label;

        Source(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public BorrowerProfile {
        missingSources = List.copyOf(missingSources);
    }

    /** Для лимитов берём меньшую из заявленной и отчётной выручки — консервативная оценка. */
    public long revenue12m() {
        if (reportedRevenue12m != null && reportedRevenue12m > 0) {
            return Math.min(declaredRevenue12m, reportedRevenue12m);
        }
        return declaredRevenue12m;
    }

    /** Относительное расхождение заявленной выручки с отчётностью ФНС; null — сверить не с чем. */
    public Double revenueMismatch() {
        if (reportedRevenue12m == null || reportedRevenue12m <= 0) {
            return null;
        }
        return Math.abs(declaredRevenue12m - reportedRevenue12m) / (double) reportedRevenue12m;
    }
}
