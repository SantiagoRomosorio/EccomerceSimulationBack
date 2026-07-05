package com.ecommerce.identity.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "admin")
public record AdminBootstrapProperties(
        String email,
        String password,
        String fullName
) {

    public boolean configured() {
        return StringUtils.hasText(email) && StringUtils.hasText(password);
    }

    public boolean incomplete() {
        return StringUtils.hasText(email) != StringUtils.hasText(password);
    }

    public String normalizedEmail() {
        return email.trim().toLowerCase();
    }

    public String resolvedFullName() {
        if (StringUtils.hasText(fullName)) {
            return fullName.trim();
        }

        return "Administrator";
    }
}
