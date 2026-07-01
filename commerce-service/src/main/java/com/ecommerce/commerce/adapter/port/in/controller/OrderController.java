package com.ecommerce.commerce.adapter.port.in.controller;

import com.ecommerce.commerce.adapter.port.in.dto.ConfirmOrderPaymentRequest;
import com.ecommerce.commerce.adapter.port.in.dto.OrderResponse;
import com.ecommerce.commerce.adapter.port.in.mapper.CommerceDtoMapper;
import com.ecommerce.commerce.application.port.in.ConfirmOrderPaymentUseCase;
import com.ecommerce.commerce.application.port.in.GetOrderUseCase;
import com.ecommerce.commerce.application.port.in.ListOrdersUseCase;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ListOrdersUseCase listOrdersUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ConfirmOrderPaymentUseCase confirmOrderPaymentUseCase;
    private final CommerceDtoMapper mapper;
    private final ApiResponseFactory responseFactory;

    public OrderController(
            ListOrdersUseCase listOrdersUseCase,
            GetOrderUseCase getOrderUseCase,
            ConfirmOrderPaymentUseCase confirmOrderPaymentUseCase,
            CommerceDtoMapper mapper,
            ApiResponseFactory responseFactory
    ) {
        this.listOrdersUseCase = listOrdersUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.confirmOrderPaymentUseCase = confirmOrderPaymentUseCase;
        this.mapper = mapper;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    @Operation(summary = "List current user orders")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing authenticated user header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<OrderResponse>>> list(
            @RequestHeader("X-User-Id") UUID userId,
            HttpServletRequest request
    ) {
        List<OrderResponse> response = listOrdersUseCase.listOrders(userId).stream()
                .map(mapper::toResponse)
                .toList();
        return responseFactory.success(ApiStatusCode.OK, "Orders returned successfully", response, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get current user order by id")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid order id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing authenticated user header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getById(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return responseFactory.success(
                ApiStatusCode.OK,
                "Order returned successfully",
                mapper.toResponse(getOrderUseCase.getOrder(userId, id)),
                request
        );
    }

    @PostMapping("/{id}/payment-confirmations")
    @Operation(summary = "Confirm current user order payment")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order payment confirmed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or order id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing authenticated user header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Order cannot be paid in current state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPayment(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmOrderPaymentRequest body,
            HttpServletRequest request
    ) {
        OrderResponse response = mapper.toResponse(confirmOrderPaymentUseCase.confirmPayment(
                userId,
                id,
                new ConfirmOrderPaymentUseCase.Command(body.paymentMethod(), body.providerReference())
        ));
        return responseFactory.success(ApiStatusCode.OK, "Order payment confirmed successfully", response, request);
    }
}
