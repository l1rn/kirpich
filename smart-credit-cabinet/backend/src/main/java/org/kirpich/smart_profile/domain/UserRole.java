package org.kirpich.smart_profile.domain;

public enum UserRole {
    /** Руководитель компании: подаёт заявку и подписывает договор. */
    DIRECTOR,
    /** Бухгалтер: заполняет заявку и загружает документы, но не подписывает договор. */
    ACCOUNTANT,
    /** Кредитный специалист банка. */
    UNDERWRITER;

    public boolean isClient() {
        return this != UNDERWRITER;
    }
}
