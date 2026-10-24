package org.kirpich.smart_profile.exceptions;

public class UserHasNoCompanyException extends RuntimeException {
    public UserHasNoCompanyException(String message) {
        super(message);
    }
}
