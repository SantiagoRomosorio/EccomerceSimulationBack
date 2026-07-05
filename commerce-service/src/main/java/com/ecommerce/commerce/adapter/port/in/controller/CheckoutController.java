package com.ecommerce.commerce.adapter.port.in.controller;

import static com.ecommerce.common.web.openapi.OpenApiSecurity.BEARER_AUTH;

import com.ecommerce.commerce.adapter.port.in.dto.CheckoutRequest;
import com.ecommerce.commerce.adapter.port.in.dto.OrderResponse;
import com.ecommerce.commerce.adapter.port.in.mapper.CommerceDtoMapper;
import com.ecommerce.commerce.application.port.in.CheckoutUseCase;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@SecurityRequirement(name = BEARER_AUTH)
public class CheckoutController {

    private final CheckoutUseCase checkoutUseCase;
    private final CommerceDtoMapper mapper;
    private final ApiResponseFactory responseFactory;

    public CheckoutController(
            CheckoutUseCase checkoutUseCase,
            CommerceDtoMapper mapper,
            ApiResponseFactory responseFactory
    ) {
        this.checkoutUseCase = checkoutUseCase;
        this.mapper = mapper;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    @Operation(summary = "Checkout current user cart")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid cart"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing authenticated user header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CheckoutRequest body,
            HttpServletRequest request
    ) {
        OrderResponse response = mapper.toResponse(checkoutUseCase.checkout(userId, new CheckoutUseCase.Command(
                mapper.toDomain(body.shippingAddress()),
                mapper.toDomain(body.billingAddress()),
                body.notes()
        )));
        return responseFactory.success(ApiStatusCode.CREATED, "Order created successfully", response, request);
    }
}
