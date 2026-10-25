package org.kirpich.smart_profile.integration;

/** Временная ошибка источника: таймаут, нет соединения, 5xx. Запрос можно повторить. */
public class SourceUnavailableException extends RuntimeException {

    public SourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
