package com.ecommerce.commerce.adapter.port.in.controller;

import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
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
    public ResponseEntity<ApiResponse<Map<String, String>>> health(HttpServletRequest request) {
        return responseFactory.success(
                ApiStatusCode.OK,
                "Service is healthy",
                Map.of("service", "commerce-service"),
                request
        );
    }
}
