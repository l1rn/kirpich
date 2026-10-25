package org.kirpich.smart_profile.domain.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kirpich.smart_profile.domain.Finance;
import org.kirpich.smart_profile.domain.policy.CreditPolicy;
import org.kirpich.smart_profile.domain.policy.Product;
import org.kirpich.smart_profile.domain.policy.ScoringPolicy;

/**
 * Балльный скоринг: каждый фактор добавляет или снимает баллы, сумма даёт грейд риска.
 * Скоринг только оценивает риск — что делать с оценкой, решает {@link org.kirpich.smart_profile.domain.decision.DecisionEngine}.
 */
public final class ScoringEngine {

    private static final Map<String, String> LIQUIDATION_STATUSES = Map.of(
            "LIQUIDATING", "Компания в процессе ликвидации",
            "LIQUIDATED", "Компания ликвидирована",
            "BANKRUPT", "Компания в процедуре банкротства");

    private ScoringEngine() {
    }

    public record FactorResult(String code, String label, String value, int points) {
    }

    public record StopFactor(String code, String text) {
    }

    public record ScoreResult(int score, String grade, List<FactorResult> factors, List<StopFactor> stopFactors) {
    }

    public static ScoreResult score(BorrowerProfile profile, long amount, int termMonths, Product product, CreditPolicy policy) {
        ScoringPolicy sp = policy.scoring();
        List<FactorResult> factors = new ArrayList<>();

        if (profile.businessAgeMonths() != null) {
            int age = profile.businessAgeMonths();
            factors.add(numberFactor(sp, "business_age", age, age + " мес."));
        }

        long revenue = profile.revenue12m();
        double amountRatio = revenue > 0 ? (double) amount / revenue : Double.POSITIVE_INFINITY;
        factors.add(numberFactor(sp, "amount_to_revenue", amountRatio, Finance.formatPercent(amountRatio)));

        // Платёжную нагрузку оцениваем по базовой ставке продукта — грейд ещё неизвестен
        long newPayment = Finance.annuityPayment(amount, product.baseRate(), termMonths);
        double monthlyRevenue = revenue / 12.0;
        double load = monthlyRevenue > 0
                ? (profile.existingMonthlyPayments() + newPayment) / monthlyRevenue
                : Double.POSITIVE_INFINITY;
        factors.add(numberFactor(sp, "payment_to_revenue", load, Finance.formatPercent(load)));

        if (profile.creditHistory() != null) {
            factors.add(categoryFactor(sp, "credit_history", profile.creditHistory()));
        }
        if (profile.taxDebt() != null) {
            factors.add(categoryFactor(sp, "tax_debt", profile.taxDebt() ? "present" : "absent"));
        }
        if (profile.massRegistration() != null) {
            factors.add(categoryFactor(sp, "mass_registration", profile.massRegistration() ? "present" : "absent"));
        }

        int total = sp.baseScore() + factors.stream().mapToInt(FactorResult::points).sum();
        total = Math.max(0, Math.min(100, total));
        return new ScoreResult(total, sp.gradeFor(total), List.copyOf(factors), stopFactors(profile, sp));
    }

    static List<StopFactor> stopFactors(BorrowerProfile profile, ScoringPolicy sp) {
        List<StopFactor> stops = new ArrayList<>();
        if (profile.companyStatus() != null && LIQUIDATION_STATUSES.containsKey(profile.companyStatus())) {
            stops.add(new StopFactor("company_status", LIQUIDATION_STATUSES.get(profile.companyStatus())));
        }
        if (profile.directorDisqualified()) {
            stops.add(new StopFactor("director_disqualified", "Руководитель компании дисквалифицирован"));
        }
        if (profile.currentOverdueDays() >= sp.stopOverdueDays()) {
            stops.add(new StopFactor("current_overdue", "Текущая просрочка больше %d дней".formatted(sp.stopOverdueDays())));
        }
        if (profile.businessAgeMonths() != null && profile.businessAgeMonths() < sp.minBusinessAgeMonths()) {
            stops.add(new StopFactor("business_age", "Бизнесу меньше %d месяцев".formatted(sp.minBusinessAgeMonths())));
        }
        return List.copyOf(stops);
    }

    private static FactorResult numberFactor(ScoringPolicy sp, String code, double value, String shown) {
        ScoringPolicy.Factor factor = sp.factor(code);
        ScoringPolicy.Outcome outcome = factor.forNumber(value);
        return new FactorResult(code, factor.label(), outcome.text() + " (" + shown + ")", outcome.points());
    }

    private static FactorResult categoryFactor(ScoringPolicy sp, String code, String key) {
        ScoringPolicy.Factor factor = sp.factor(code);
        ScoringPolicy.Outcome outcome = factor.forCategory(key);
        return new FactorResult(code, factor.label(), outcome.text(), outcome.points());
    }
}
