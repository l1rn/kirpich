package org.kirpich.smart_profile.domain;

public class TransitionException extends RuntimeException {

    public TransitionException(ApplicationStatus current, ApplicationStatus target) {
        super("Действие недоступно: заявка в статусе «%s», переход в «%s» невозможен"
                .formatted(current.label(), target.label()));
    }
}
