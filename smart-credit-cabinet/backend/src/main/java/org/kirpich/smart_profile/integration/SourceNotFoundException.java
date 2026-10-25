package org.kirpich.smart_profile.integration;

/** Источник ответил, что сведений нет. Повтор не поможет. */
public class SourceNotFoundException extends RuntimeException {

    public SourceNotFoundException(String message) {
        super(message);
    }
}
