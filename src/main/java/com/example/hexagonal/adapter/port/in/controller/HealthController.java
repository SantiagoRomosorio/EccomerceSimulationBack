package com.example.hexagonal.adapter.port.in.controller;

import com.example.hexagonal.adapter.port.in.response.ApiResponse;
import com.example.hexagonal.adapter.port.in.response.ApiResponseFactory;
import com.example.hexagonal.adapter.port.in.response.ApiStatusCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final ApiResponseFactory responseFactory;

    public HealthController(ApiResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @GetMapping
    @Operation(summary = "Return service health")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Service is healthy"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> health(HttpServletRequest request) {
        return responseFactory.success(
                ApiStatusCode.OK,
                "Service is healthy",
                Map.of("status", "UP", "service", "hexagonal-spring-boot"),
                request
        );
    }
}
