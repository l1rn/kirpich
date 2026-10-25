package org.kirpich.smart_profile.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Статусы заявки и допустимые переходы между ними. Проверки внутри CHECKING (ЕГРЮЛ, ФНС, БКИ, скоринг)
 * — не статусы, а шаги конвейера: так машина состояний остаётся маленькой.
 */
public enum ApplicationStatus {
    DRAFT("Черновик", 0),
    CHECKING("Проверяем заявку", 1),
    NEEDS_INFO("Нужны уточнения", 1),
    MANUAL_REVIEW("Заявку изучает специалист", 2),
    OFFERED("Есть предложение", 3),
    DECLINED("Отказ", 2),
    ISSUED("Деньги выданы", 4),
    CANCELLED("Заявка отозвана", 0);

    /** Этапы, которые клиент видит на шкале прогресса. */
    public static final List<String> STAGES = List.of("Заявка", "Проверка", "Решение", "Оформление", "Выдача");

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> TRANSITIONS = new EnumMap<>(ApplicationStatus.class);

    static {
        TRANSITIONS.put(DRAFT, EnumSet.of(CHECKING, CANCELLED));
        TRANSITIONS.put(CHECKING, EnumSet.of(NEEDS_INFO, MANUAL_REVIEW, OFFERED, DECLINED, CANCELLED));
        TRANSITIONS.put(NEEDS_INFO, EnumSet.of(CHECKING, MANUAL_REVIEW, CANCELLED));
        TRANSITIONS.put(MANUAL_REVIEW, EnumSet.of(OFFERED, DECLINED, NEEDS_INFO, CANCELLED));
        TRANSITIONS.put(OFFERED, EnumSet.of(ISSUED, CANCELLED));
        TRANSITIONS.put(DECLINED, EnumSet.noneOf(ApplicationStatus.class));
        TRANSITIONS.put(ISSUED, EnumSet.noneOf(ApplicationStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(ApplicationStatus.class));
    }

    private final String label;
    private final int stageIndex;

    ApplicationStatus(String label, int stageIndex) {
        this.label = label;
        this.stageIndex = stageIndex;
    }

    public String label() {
        return label;
    }

    public int stageIndex() {
        return stageIndex;
    }

    public boolean canMoveTo(ApplicationStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public boolean isFinal() {
        return TRANSITIONS.get(this).isEmpty();
    }

    public void ensureCanMoveTo(ApplicationStatus target) {
        if (!canMoveTo(target)) {
            throw new TransitionException(this, target);
        }
    }
}
