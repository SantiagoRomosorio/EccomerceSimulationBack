package com.ecommerce.catalog.adapter.port.in.controller;

import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import com.ecommerce.catalog.domain.exception.ConflictException;
import com.ecommerce.catalog.domain.exception.DomainException;
import com.ecommerce.catalog.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ApiResponseFactory responseFactory;

    public GlobalExceptionHandler(ApiResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Object>> handleDomainException(
            DomainException exception,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                statusFor(exception),
                exception.getMessage(),
                exception.getClass().getSimpleName(),
                exception.details(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()
                ))
                .toList());

        return responseFactory.error(
                ApiStatusCode.BAD_REQUEST,
                message,
                "Request body validation failed",
                details,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                ApiStatusCode.BAD_REQUEST,
                "Invalid request body",
                exception.getClass().getSimpleName(),
                Map.of("error", "Malformed JSON or incompatible field type"),
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", exception.getName());
        details.put("value", exception.getValue());
        details.put("requiredType", exception.getRequiredType() == null
                ? "unknown"
                : exception.getRequiredType().getSimpleName());

        return responseFactory.error(
                ApiStatusCode.BAD_REQUEST,
                "Invalid request parameter",
                exception.getClass().getSimpleName(),
                details,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                ApiStatusCode.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                exception.getClass().getSimpleName(),
                Map.of("exception", exception.getMessage() == null ? "No message" : exception.getMessage()),
                request
        );
    }

    private ApiStatusCode statusFor(DomainException exception) {
        if (exception instanceof ConflictException) {
            return ApiStatusCode.CONFLICT;
        }

        if (exception instanceof ResourceNotFoundException) {
            return ApiStatusCode.NOT_FOUND;
        }

        return ApiStatusCode.BAD_REQUEST;
    }
}
