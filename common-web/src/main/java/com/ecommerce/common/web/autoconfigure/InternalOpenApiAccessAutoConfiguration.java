package com.ecommerce.common.web.autoconfigure;

import com.ecommerce.common.web.openapi.InternalOpenApiAccessFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class InternalOpenApiAccessAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "docs.gateway-protection", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    InternalOpenApiAccessFilter internalOpenApiAccessFilter(
            @Value("${docs.gateway-token:local-docs-token-change-me}") String gatewayToken
    ) {
        return new InternalOpenApiAccessFilter(gatewayToken);
    }
}
