package com.ecommerce.catalog.domain.exception;

import java.util.Map;

public abstract class DomainException extends RuntimeException {

    private final Map<String, Object> details;

    protected DomainException(String message, Map<String, Object> details) {
        super(message);
        this.details = details == null ? Map.of() : details;
    }

    public Map<String, Object> details() {
        return details;
    }
}
