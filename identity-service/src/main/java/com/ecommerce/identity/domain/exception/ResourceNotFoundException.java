package com.ecommerce.identity.domain.exception;

import java.util.Map;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(DomainErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, errorCode.defaultMessage(), details);
    }
}
