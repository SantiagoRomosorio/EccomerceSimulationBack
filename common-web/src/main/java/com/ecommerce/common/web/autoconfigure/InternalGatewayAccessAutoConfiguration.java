package com.ecommerce.common.web.autoconfigure;

import com.ecommerce.common.web.security.InternalGatewayAccessFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
public class InternalGatewayAccessAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "internalGatewayAccessFilterRegistration")
    @ConditionalOnProperty(prefix = "internal.gateway-protection", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    FilterRegistrationBean<InternalGatewayAccessFilter> internalGatewayAccessFilterRegistration(
            @Value("${internal.gateway-token:local-internal-gateway-token-change-me}") String gatewayToken
    ) {
        FilterRegistrationBean<InternalGatewayAccessFilter> registration = new FilterRegistrationBean<>(
                new InternalGatewayAccessFilter(gatewayToken)
        );
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
