package com.ecommerce.identity.adapter.port.in.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Set<String> roles,
        boolean active,
        Instant createdAt
) {
}
