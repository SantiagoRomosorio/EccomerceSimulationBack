package com.ecommerce.commerce.adapter.port.in.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.commerce.domain.exception.CatalogUnavailableException;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.ResourceAccessException;

class GlobalExceptionHandlerTests {

    @Test
    void mapsCatalogUnavailableToServiceUnavailableWithoutInfrastructureDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(new ApiResponseFactory());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/checkout");
        CatalogUnavailableException exception = new CatalogUnavailableException(
                new ResourceAccessException("connection refused at internal-host:8082")
        );

        ResponseEntity<ApiResponse<Object>> response = handler.handleDomainException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().message()).isEqualTo("Catalog service is temporarily unavailable");
        assertThat(response.getBody().data()).isEqualTo(Map.of("service", "catalog"));
        assertThat(response.toString()).doesNotContain("internal-host");
    }
}
