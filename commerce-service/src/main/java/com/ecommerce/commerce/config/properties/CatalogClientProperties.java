package com.ecommerce.commerce.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "clients.catalog")
public record CatalogClientProperties(
        String baseUrl,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout
) {

    @ConstructorBinding
    public CatalogClientProperties {
    }

    public CatalogClientProperties(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(5));
    }
}
