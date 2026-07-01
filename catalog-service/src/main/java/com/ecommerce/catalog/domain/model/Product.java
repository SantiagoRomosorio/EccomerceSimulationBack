package com.ecommerce.catalog.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Product(
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
    public static Product create(
            String sku,
            String name,
            String description,
            BigDecimal price,
            String currency,
            Category category,
            Brand brand,
            int stockQuantity
    ) {
        Instant now = Instant.now();
        return new Product(
                UUID.randomUUID(),
                sku,
                name,
                description,
                price,
                currency,
                category.id(),
                category.name(),
                brand.id(),
                brand.name(),
                stockQuantity,
                true,
                now,
                now
        );
    }

    public Product withStockQuantity(int stockQuantity) {
        return new Product(
                id,
                sku,
                name,
                description,
                price,
                currency,
                categoryId,
                categoryName,
                brandId,
                brandName,
                stockQuantity,
                active,
                createdAt,
                Instant.now()
        );
    }
}
