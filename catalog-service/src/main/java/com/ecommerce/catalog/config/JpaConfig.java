package com.ecommerce.catalog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.ecommerce.catalog.adapter.port.out.jpa.repository")
public class JpaConfig {
}
