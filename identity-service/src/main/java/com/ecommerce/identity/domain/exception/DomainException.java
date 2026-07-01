package com.ecommerce.identity.domain.exception;

import java.util.Map;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        this(DomainErrorCode.VALIDATION_ERROR, message, Map.of());
    }

    public DomainException(DomainErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), Map.of());
    }

    public DomainException(DomainErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    private final DomainErrorCode errorCode;
    private final Map<String, Object> details;

    public DomainErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> details() {
        return details;
    }
}
