package com.ecommerce.catalog.adapter.port.in.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        int stockQuantity,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
