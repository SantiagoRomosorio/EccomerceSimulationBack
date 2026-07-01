package com.ecommerce.catalog.domain.exception;

import java.util.Map;

public class ConflictException extends DomainException {

    public ConflictException(String message, Map<String, Object> details) {
        super(message, details);
    }
}
