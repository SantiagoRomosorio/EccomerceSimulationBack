package com.ecommerce.commerce.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID productId,
        String sku,
        String productName,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        BigDecimal lineTotal
) {
}
