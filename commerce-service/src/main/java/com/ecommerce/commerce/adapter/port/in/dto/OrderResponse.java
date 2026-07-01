package com.ecommerce.commerce.adapter.port.in.dto;

import com.ecommerce.commerce.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        String currency,
        BigDecimal total,
        OrderAddressResponse shippingAddress,
        OrderAddressResponse billingAddress,
        String notes,
        String paymentMethod,
        String paymentReference,
        Instant paidAt,
        Instant createdAt,
        List<OrderItemResponse> items
) {
}
