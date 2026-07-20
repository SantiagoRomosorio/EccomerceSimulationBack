package com.ecommerce.common.web.response;

import org.springframework.http.HttpStatus;

public enum ApiStatusCode {
    OK(HttpStatus.OK),
    CREATED(HttpStatus.CREATED),
    ACCEPTED(HttpStatus.ACCEPTED),
    NO_CONTENT(HttpStatus.NO_CONTENT),
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    CONFLICT(HttpStatus.CONFLICT),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ApiStatusCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public int value() {
        return httpStatus.value();
    }

    public String reason() {
        return httpStatus.getReasonPhrase();
    }
}
