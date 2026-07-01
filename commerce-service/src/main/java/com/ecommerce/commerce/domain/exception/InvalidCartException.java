package com.ecommerce.commerce.domain.exception;

import java.util.Map;

public class InvalidCartException extends DomainException {

    public InvalidCartException(String message, Map<String, Object> details) {
        super(message, details);
    }
}
