package org.kirpich.smart_profile.service;

import java.util.List;

/** Нарушены бизнес-правила: каждое сообщение можно показать пользователю как есть. */
public class ValidationFailedException extends RuntimeException {

    private final List<String> errors;

    public ValidationFailedException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public ValidationFailedException(String error) {
        this(List.of(error));
    }

    public List<String> errors() {
        return errors;
    }
}
