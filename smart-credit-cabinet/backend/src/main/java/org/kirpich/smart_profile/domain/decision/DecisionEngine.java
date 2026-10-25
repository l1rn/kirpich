package org.kirpich.smart_profile.domain.decision;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.kirpich.smart_profile.domain.Finance;
import org.kirpich.smart_profile.domain.policy.CreditPolicy;
import org.kirpich.smart_profile.domain.policy.Product;
import org.kirpich.smart_profile.domain.policy.ScoringPolicy;
import org.kirpich.smart_profile.domain.scoring.BorrowerProfile;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine.ScoreResult;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine.StopFactor;

/**
 * Движок кредитных решений: превращает оценку риска в исход заявки по правилам продукта.
 * Один и тот же балл даёт разные условия для разных продуктов: у них разные лимиты,
 * ставки и пороги автоматического одобрения.
 */
public final class DecisionEngine {

    private static final long ROUNDING_STEP = 10_000;

    private DecisionEngine() {
    }

    public enum Outcome {
        APPROVE,
        COUNTER_OFFER,
        MANUAL_REVIEW,
        DECLINE
    }

    public enum OfferKind {
        AS_REQUESTED,
        COUNTER
    }

    public record OfferProposal(String productCode, long amount, int termMonths, double rate, long monthlyPayment, OfferKind kind) {

        public static OfferProposal of(Product product, long amount, int termMonths, double rate, OfferKind kind) {
            return new OfferProposal(product.code(), amount, termMonths, rate, Finance.annuityPayment(amount, rate, termMonths), kind);
        }

        public OfferProposal withKind(OfferKind newKind) {
            return new OfferProposal(productCode, amount, termMonths, rate, monthlyPayment, newKind);
        }
    }

    /** Для APPROVE и COUNTER_OFFER proposal — предложение клиенту, для MANUAL_REVIEW — рекомендация андеррайтеру. */
    public record DecisionResult(Outcome outcome, List<String> reasons, OfferProposal proposal) {
    }

    public record Fit(OfferProposal proposal, String note) {
    }

    public static DecisionResult decide(
            Product product,
            long amount,
            int termMonths,
            String purpose,
            BorrowerProfile profile,
            ScoreResult scoring,
            CreditPolicy policy) {
        ScoringPolicy sp = policy.scoring();

        if (!scoring.stopFactors().isEmpty()) {
            return new DecisionResult(Outcome.DECLINE, scoring.stopFactors().stream().map(StopFactor::text).toList(), null);
        }
        if (ScoringPolicy.DECLINE_GRADE.equals(scoring.grade())) {
            return new DecisionResult(Outcome.DECLINE,
                    List.of("Высокий кредитный риск: %d баллов из 100".formatted(scoring.score())), null);
        }

        List<String> review = new ArrayList<>();
        List<String> critical = profile.missingSources().stream()
                .filter(s -> s != BorrowerProfile.Source.FNS)
                .map(BorrowerProfile.Source::label)
                .toList();
        if (!critical.isEmpty()) {
            review.add("Не удалось получить данные: " + String.join(", ", critical));
        }
        Double mismatch = profile.revenueMismatch();
        if (mismatch != null && mismatch > sp.revenueMismatchTolerance()) {
            review.add("Выручка в анкете отличается от отчётности ФНС на " + Finance.formatPercent(mismatch));
        }
        if ("C".equals(scoring.grade())) {
            review.add("Средний уровень риска: %d баллов, грейд C".formatted(scoring.score()));
        }

        double rate = roundRate(product.baseRate() + sp.ratePremium(scoring.grade()));
        Fit fit = fitProduct(product, amount, termMonths, rate, profile, policy);
        Product target = product;
        if (fit == null) {
            // Ни сумма, ни срок не помогли — пробуем другие продукты под ту же цель
            for (Product alt : policy.products()) {
                if (alt.code().equals(product.code()) || !alt.purposes().contains(purpose)) {
                    continue;
                }
                double altRate = roundRate(alt.baseRate() + sp.ratePremium(scoring.grade()));
                Fit altFit = fitProduct(alt, Math.min(amount, alt.maxAmount()), alt.clampTerm(termMonths), altRate, profile, policy);
                if (altFit != null) {
                    String note = "Предлагаем другой продукт — «%s»".formatted(alt.name())
                            + (altFit.note() != null ? ". " + altFit.note() : "");
                    fit = new Fit(altFit.proposal().withKind(OfferKind.COUNTER), note);
                    target = alt;
                    break;
                }
            }
        }

        if (fit == null) {
            if (!review.isEmpty()) {
                return new DecisionResult(Outcome.MANUAL_REVIEW, List.copyOf(review), null);
            }
            return new DecisionResult(Outcome.DECLINE,
                    List.of("Платёжная нагрузка слишком высокая: ни один продукт не укладывается в допустимый платёж"), null);
        }

        OfferProposal proposal = fit.proposal();
        if (proposal.amount() > target.autoApproveMax()) {
            review.add("Сумма больше лимита автоматического одобрения (%s)".formatted(Finance.formatRub(target.autoApproveMax())));
        }
        List<String> reasons = new ArrayList<>(review);
        if (fit.note() != null) {
            reasons.add(fit.note());
        }
        if (!review.isEmpty()) {
            return new DecisionResult(Outcome.MANUAL_REVIEW, List.copyOf(reasons), proposal);
        }
        Outcome outcome = proposal.kind() == OfferKind.AS_REQUESTED ? Outcome.APPROVE : Outcome.COUNTER_OFFER;
        return new DecisionResult(outcome, List.copyOf(reasons), proposal);
    }

    /** Подбирает условия в рамках одного продукта: как просили, с более длинным сроком или с меньшей суммой. */
    public static Fit fitProduct(Product product, long amount, int termMonths, double rate, BorrowerProfile profile, CreditPolicy policy) {
        long revenue = profile.revenue12m();
        long revenueCap = (long) (revenue * product.revenueRatio());
        long cap = Math.min(product.maxAmount(), revenueCap);
        double budget = policy.scoring().maxPaymentToRevenue() * revenue / 12.0 - profile.existingMonthlyPayments();
        String maxLoad = Finance.formatPercent(policy.scoring().maxPaymentToRevenue());

        if (amount <= cap && Finance.annuityPayment(amount, rate, termMonths) <= budget) {
            return new Fit(OfferProposal.of(product, amount, termMonths, rate, OfferKind.AS_REQUESTED), null);
        }

        if (amount <= cap) {
            for (int term = termMonths + 1; term <= product.maxTerm(); term++) {
                if (Finance.annuityPayment(amount, rate, term) <= budget) {
                    String note = "Срок увеличен до %d мес., чтобы платёж не превышал %s месячной выручки".formatted(term, maxLoad);
                    return new Fit(OfferProposal.of(product, amount, term, rate, OfferKind.COUNTER), note);
                }
            }
        }

        for (int term : new LinkedHashSet<>(List.of(termMonths, product.maxTerm()))) {
            long paymentCap = Finance.maxAmountForPayment(budget, rate, term);
            long limit = Finance.roundDown(Math.min(cap, paymentCap), ROUNDING_STEP);
            if (limit < product.minAmount()) {
                continue;
            }
            String why;
            if (paymentCap < cap) {
                why = "платёж не должен превышать %s месячной выручки".formatted(maxLoad);
            } else if (cap == product.maxAmount()) {
                why = "максимальная сумма продукта — " + Finance.formatRub(product.maxAmount());
            } else {
                why = "лимит продукта — %s годовой выручки".formatted(Finance.formatPercent(product.revenueRatio()));
            }
            String note = "Сумма уменьшена до %s: %s".formatted(Finance.formatRub(limit), why);
            if (term != termMonths) {
                note += "; срок увеличен до %d мес.".formatted(term);
            }
            return new Fit(OfferProposal.of(product, limit, term, rate, OfferKind.COUNTER), note);
        }
        return null;
    }

    private static double roundRate(double rate) {
        return Math.round(rate * 100) / 100.0;
    }
}
