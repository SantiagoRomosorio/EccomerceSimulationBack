package com.ecommerce.commerce.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        UUID userId,
        OrderStatus status,
        String currency,
        BigDecimal total,
        OrderAddress shippingAddress,
        OrderAddress billingAddress,
        String notes,
        Instant createdAt,
        List<OrderItem> items
) {
}
