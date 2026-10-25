package org.kirpich.smart_profile.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.kirpich.smart_profile.domain.decision.DecisionEngine;
import org.kirpich.smart_profile.domain.decision.DecisionEngine.DecisionResult;
import org.kirpich.smart_profile.domain.decision.DecisionEngine.OfferKind;
import org.kirpich.smart_profile.domain.decision.DecisionEngine.Outcome;
import org.kirpich.smart_profile.domain.policy.CreditPolicy;
import org.kirpich.smart_profile.domain.policy.Product;
import org.kirpich.smart_profile.domain.scoring.BorrowerProfile;
import org.kirpich.smart_profile.domain.scoring.ProfileBuilder;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine;
import org.kirpich.smart_profile.domain.scoring.ScoringEngine.ScoreResult;
import org.kirpich.smart_profile.domain.sources.BkiReport;
import org.kirpich.smart_profile.domain.sources.EgrulCompany;
import org.kirpich.smart_profile.domain.sources.FnsBusiness;
import org.kirpich.smart_profile.support.TestPolicy;

/** Демо-сценарии на тех же ответах источников, что отдаёт WireMock. Дата зафиксирована. */
class DecisionEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
    private static final CreditPolicy POLICY = TestPolicy.load();

    private record Case(ScoreResult score, DecisionResult decision) {
    }

    private static Case run(String inn, String productCode, String purpose, long amount, int term, long revenue, boolean bkiAvailable) {
        BorrowerProfile profile = ProfileBuilder.build(
                revenue,
                0,
                TestPolicy.mockBody("egrul", inn, EgrulCompany.class),
                TestPolicy.mockBody("fns", inn, FnsBusiness.class),
                bkiAvailable ? TestPolicy.mockBody("bki", inn, BkiReport.class) : null,
                TODAY);
        Product product = POLICY.product(productCode);
        ScoreResult score = ScoringEngine.score(profile, amount, term, product, POLICY);
        return new Case(score, DecisionEngine.decide(product, amount, term, purpose, profile, score, POLICY));
    }

    @Test
    void romashkaIsApprovedAutomatically() {
        Case c = run("7701123451", "BUSINESS_CREDIT", "investment", 3_000_000, 24, 60_000_000, true);

        assertThat(c.score().grade()).isEqualTo("A");
        assertThat(c.decision().outcome()).isEqualTo(Outcome.APPROVE);
        assertThat(c.decision().proposal().kind()).isEqualTo(OfferKind.AS_REQUESTED);
        assertThat(c.decision().proposal().amount()).isEqualTo(3_000_000);
    }

    @Test
    void petrovGetsCounterOfferLimitedByRevenue() {
        Case c = run("771234567859", "BUSINESS_CREDIT", "investment", 8_000_000, 36, 14_000_000, true);

        assertThat(c.score().grade()).isEqualTo("B");
        assertThat(c.decision().outcome()).isEqualTo(Outcome.COUNTER_OFFER);
        assertThat(c.decision().proposal().amount()).isEqualTo(7_000_000);
        assertThat(c.decision().proposal().rate()).isEqualTo(25.0);
        assertThat(c.decision().reasons()).anyMatch(r -> r.contains("50% годовой выручки"));
    }

    @Test
    void stroyGoesToManualReviewAboveAutoApproveLimit() {
        Case c = run("5003123458", "BUSINESS_CREDIT", "investment", 20_000_000, 36, 42_000_000, true);

        assertThat(c.score().factors()).anyMatch(f -> f.code().equals("credit_history") && f.points() < 0);
        assertThat(c.decision().outcome()).isEqualTo(Outcome.MANUAL_REVIEW);
        assertThat(c.decision().proposal()).as("рекомендация андеррайтеру").isNotNull();
        assertThat(c.decision().reasons()).anyMatch(r -> r.contains("автоматического одобрения"));
    }

    @Test
    void zakatIsDeclinedByStopFactor() {
        Case c = run("7801123455", "OVERDRAFT", "cash_gap", 1_000_000, 12, 8_000_000, true);

        assertThat(c.decision().outcome()).isEqualTo(Outcome.DECLINE);
        assertThat(c.decision().reasons()).containsExactly("Компания в процессе ликвидации");
    }

    @Test
    void tishinaGoesToManualReviewWithoutBki() {
        Case c = run("6623123450", "CREDIT_LINE", "working_capital", 3_000_000, 24, 30_000_000, false);

        assertThat(c.decision().outcome()).isEqualTo(Outcome.MANUAL_REVIEW);
        assertThat(c.decision().reasons()).anyMatch(r -> r.contains("Не удалось получить данные: БКИ"));
    }

    @Test
    void sameClientGetsDifferentTermsForDifferentProducts() {
        Case credit = run("771234567859", "BUSINESS_CREDIT", "working_capital", 8_000_000, 24, 14_000_000, true);
        Case line = run("771234567859", "CREDIT_LINE", "working_capital", 8_000_000, 24, 14_000_000, true);

        assertThat(credit.score().grade()).isEqualTo(line.score().grade());
        assertThat(credit.decision().outcome()).isEqualTo(Outcome.COUNTER_OFFER);
        assertThat(credit.decision().proposal().amount()).isEqualTo(7_000_000);
        assertThat(line.decision().outcome()).isEqualTo(Outcome.COUNTER_OFFER);
        assertThat(line.decision().proposal().amount()).isEqualTo(4_200_000);
    }

    @Test
    void statusMachineRejectsSkippingSteps() {
        assertThat(ApplicationStatus.DRAFT.canMoveTo(ApplicationStatus.CHECKING)).isTrue();
        assertThat(ApplicationStatus.DRAFT.canMoveTo(ApplicationStatus.OFFERED)).isFalse();
        assertThat(ApplicationStatus.NEEDS_INFO.canMoveTo(ApplicationStatus.MANUAL_REVIEW)).isTrue();
        assertThat(ApplicationStatus.ISSUED.isFinal()).isTrue();
    }

    @Test
    void innChecksumIsValidated() {
        assertThat(InnValidator.isValid("7701123451")).isTrue();
        assertThat(InnValidator.isValid("771234567859")).isTrue();
        assertThat(InnValidator.isValid("7701123452")).isFalse();
    }

    @Test
    void rublesAreFormattedForPeople() {
        assertThat(Finance.formatRub(7_000_000)).isEqualTo("7 млн ₽");
        assertThat(Finance.formatRub(3_600_000)).isEqualTo("3,6 млн ₽");
        assertThat(Finance.formatRub(350_000)).isEqualTo("350 000 ₽");
    }
}
