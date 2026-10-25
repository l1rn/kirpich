package org.kirpich.smart_profile.domain;

/** Шаги конвейера внутри статуса CHECKING — в порядке выполнения. */
public enum PipelineStep {
    VALIDATION("Проверка заявки"),
    EGRUL("ЕГРЮЛ / ЕГРИП"),
    FNS("ФНС «Прозрачный бизнес»"),
    BKI("Бюро кредитных историй"),
    DECISION("Скоринг и решение");

    private final String label;

    PipelineStep(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
