package com.ecommerce.commerce.config;

import com.ecommerce.commerce.config.properties.CatalogClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CatalogClientProperties.class)
public class PropertiesConfig {
}
