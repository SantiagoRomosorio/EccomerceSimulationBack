package com.ecommerce.identity.adapter.port.in.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMinutes
) {
}
