package com.ecommerce.commerce.config;

import com.ecommerce.common.web.security.InternalGatewayAccessFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder(
            @Value("${internal.gateway-token:local-internal-gateway-token-change-me}") String gatewayToken
    ) {
        return RestClient.builder()
                .defaultHeader(InternalGatewayAccessFilter.HEADER_NAME, gatewayToken);
    }
}
