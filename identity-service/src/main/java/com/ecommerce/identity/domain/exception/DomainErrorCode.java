package com.ecommerce.identity.domain.exception;

public enum DomainErrorCode {
    VALIDATION_ERROR("Invalid request"),
    EMAIL_ALREADY_REGISTERED("Email is already registered"),
    INVALID_CREDENTIALS("Invalid credentials"),
    USER_NOT_FOUND("User not found");

    private final String defaultMessage;

    DomainErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
