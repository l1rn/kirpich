package org.kirpich.smart_profile.domain.policy;

import java.util.Comparator;
import java.util.List;

public record ScoringPolicy(
        int baseScore,
        List<Grade> grades,
        double maxPaymentToRevenue,
        double revenueMismatchTolerance,
        int minBusinessAgeMonths,
        int stopOverdueDays,
        List<Factor> factors) {

    /** Грейд ниже всех порогов — отказ. */
    public static final String DECLINE_GRADE = "D";

    public String gradeFor(int score) {
        return grades.stream()
                .sorted(Comparator.comparingInt(Grade::minScore).reversed())
                .filter(g -> score >= g.minScore())
                .map(Grade::name)
                .findFirst()
                .orElse(DECLINE_GRADE);
    }

    public double ratePremium(String grade) {
        return grades.stream().filter(g -> g.name().equals(grade)).mapToDouble(Grade::ratePremium).findFirst().orElse(0);
    }

    public Factor factor(String code) {
        return factors.stream()
                .filter(f -> f.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("В политике нет фактора " + code));
    }

    public record Grade(String name, int minScore, double ratePremium) {
    }

    public record Factor(String code, String label, List<Band> bands, List<CategoryOutcome> values) {

        public Factor {
            bands = bands == null ? List.of() : List.copyOf(bands);
            values = values == null ? List.of() : List.copyOf(values);
        }

        /** Интервалы проверяются по порядку; верхняя граница включительно, без границы — «всё остальное». */
        public Outcome forNumber(double value) {
            return bands.stream()
                    .filter(b -> b.max() == null || value <= b.max())
                    .findFirst()
                    .map(b -> new Outcome(b.points(), b.text()))
                    .orElseThrow(() -> new IllegalStateException("Нет интервала для %s в факторе «%s»".formatted(value, label)));
        }

        public Outcome forCategory(String key) {
            return values.stream()
                    .filter(v -> v.key().equals(key))
                    .findFirst()
                    .map(v -> new Outcome(v.points(), v.text()))
                    .orElseThrow(() -> new IllegalStateException("Нет значения %s в факторе «%s»".formatted(key, label)));
        }
    }

    public record Band(Double max, int points, String text) {
    }

    public record CategoryOutcome(String key, int points, String text) {
    }

    public record Outcome(int points, String text) {
    }
}
