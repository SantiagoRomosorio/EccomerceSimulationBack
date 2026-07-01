package com.ecommerce.identity.domain.exception;

import java.util.Map;

public class ConflictException extends DomainException {

    public ConflictException(DomainErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, errorCode.defaultMessage(), details);
    }
}
