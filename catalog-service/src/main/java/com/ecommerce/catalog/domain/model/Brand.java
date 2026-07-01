package com.ecommerce.catalog.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Brand(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static Brand create(String name, String slug, String description) {
        Instant now = Instant.now();
        return new Brand(UUID.randomUUID(), name, slug, description, true, now, now);
    }
}
