package com.ecommerce.identity.domain.exception;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super(DomainErrorCode.INVALID_CREDENTIALS);
    }
}
