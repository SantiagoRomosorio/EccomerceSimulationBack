package com.ecommerce.common.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standard API response wrapper used by all backend endpoints")
public record ApiResponse<T>(
        @Schema(description = "UTC response timestamp")
        Instant timestamp,
        @Schema(description = "HTTP method that generated the response", example = "POST")
        String method,
        @Schema(description = "HTTP numeric status code", example = "200")
        int status,
        @Schema(description = "HTTP reason phrase", example = "OK")
        String result,
        @Schema(description = "Short technical detail. Null on successful responses.")
        String developerMessage,
        @Schema(description = "Human-readable response message")
        String message,
        @Schema(description = "Request path that generated the response", example = "/api/products")
        String path,
        @Schema(description = "Response payload. Error responses may use null or an object with details.")
        T data
) {
}
