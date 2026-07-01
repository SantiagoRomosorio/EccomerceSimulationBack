package com.ecommerce.commerce.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients.catalog")
public record CatalogClientProperties(String baseUrl) {
}
