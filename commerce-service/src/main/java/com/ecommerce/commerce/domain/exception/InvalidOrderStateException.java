package com.ecommerce.commerce.domain.exception;

import java.util.Map;

public class InvalidOrderStateException extends DomainException {
    public InvalidOrderStateException(String message, Map<String, Object> details) {
        super(message, details);
    }
}
