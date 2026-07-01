package com.ecommerce.identity.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes
) {

    public Duration expiration() {
        return Duration.ofMinutes(expirationMinutes);
    }
}
