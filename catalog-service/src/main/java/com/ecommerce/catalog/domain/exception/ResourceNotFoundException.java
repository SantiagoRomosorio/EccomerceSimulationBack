package com.ecommerce.catalog.domain.exception;

import java.util.Map;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message, Map<String, Object> details) {
        super(message, details);
    }
}
