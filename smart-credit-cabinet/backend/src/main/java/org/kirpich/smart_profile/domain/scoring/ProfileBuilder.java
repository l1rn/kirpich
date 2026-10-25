package org.kirpich.smart_profile.domain.scoring;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.kirpich.smart_profile.domain.scoring.BorrowerProfile.Source;
import org.kirpich.smart_profile.domain.sources.BkiReport;
import org.kirpich.smart_profile.domain.sources.EgrulCompany;
import org.kirpich.smart_profile.domain.sources.FnsBusiness;

public final class ProfileBuilder {

    private ProfileBuilder() {
    }

    public static BorrowerProfile build(
            long declaredRevenue12m,
            long declaredMonthlyPayments,
            EgrulCompany egrul,
            FnsBusiness fns,
            BkiReport bki,
            LocalDate today) {
        List<Source> missing = new ArrayList<>();

        String companyStatus = null;
        boolean directorDisqualified = false;
        Integer businessAge = null;
        if (egrul != null) {
            companyStatus = egrul.status();
            directorDisqualified = egrul.director() != null && egrul.director().disqualified();
            if (egrul.registrationDate() != null) {
                businessAge = (int) ChronoUnit.MONTHS.between(LocalDate.parse(egrul.registrationDate()), today);
            }
        } else {
            missing.add(Source.EGRUL);
        }

        Long reportedRevenue = null;
        Boolean taxDebt = null;
        Boolean massRegistration = null;
        if (fns != null) {
            reportedRevenue = fns.reportedRevenue() == null ? null : fns.reportedRevenue().amount();
            taxDebt = fns.taxDebt();
            massRegistration = fns.massAddress() || fns.massDirector();
        } else {
            missing.add(Source.FNS);
        }

        long existingPayments = declaredMonthlyPayments;
        String creditHistory = null;
        int currentOverdue = 0;
        if (bki != null) {
            long bkiPayments = bki.loans().stream()
                    .filter(BkiReport.Loan::isActive)
                    .mapToLong(BkiReport.Loan::monthlyPayment)
                    .sum();
            // Берём большее: клиент мог указать не все кредиты
            existingPayments = Math.max(declaredMonthlyPayments, bkiPayments);
            creditHistory = categorize(bki.loans());
            currentOverdue = bki.loans().stream().mapToInt(BkiReport.Loan::currentOverdueDays).max().orElse(0);
        } else {
            missing.add(Source.BKI);
        }

        return new BorrowerProfile(
                declaredRevenue12m,
                existingPayments,
                companyStatus,
                directorDisqualified,
                businessAge,
                reportedRevenue,
                creditHistory,
                currentOverdue,
                taxDebt,
                massRegistration,
                missing);
    }

    static String categorize(List<BkiReport.Loan> loans) {
        if (loans.isEmpty()) {
            return "none";
        }
        int maxOverdue = loans.stream().mapToInt(BkiReport.Loan::maxOverdueDays12m).max().orElse(0);
        if (maxOverdue == 0) {
            return "clean";
        }
        if (maxOverdue <= 30) {
            return "overdue_up_to_30";
        }
        if (maxOverdue <= 90) {
            return "overdue_30_90";
        }
        return "overdue_90_plus";
    }
}
