package com.example.hexagonal.adapter.port.in.response;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {

    public <T> ResponseEntity<ApiResponse<T>> success(
            ApiStatusCode status,
            String message,
            T data,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status.httpStatus())
                .body(new ApiResponse<>(
                        Instant.now(),
                        request.getMethod(),
                        status.value(),
                        status.reason(),
                        null,
                        message,
                        request.getRequestURI(),
                        data
                ));
    }

    public ResponseEntity<ApiResponse<Object>> error(
            ApiStatusCode status,
            String message,
            String developerMessage,
            Map<String, Object> details,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status.httpStatus())
                .body(new ApiResponse<>(
                        Instant.now(),
                        request.getMethod(),
                        status.value(),
                        status.reason(),
                        developerMessage,
                        message,
                        request.getRequestURI(),
                        details == null || details.isEmpty() ? null : details
                ));
    }
}
