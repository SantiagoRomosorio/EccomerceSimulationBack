package com.ecommerce.commerce.config;

import com.ecommerce.commerce.config.properties.CatalogClientProperties;
import com.ecommerce.common.web.security.InternalGatewayAccessFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder(
            @Value("${internal.gateway-token:local-internal-gateway-token-change-me}") String gatewayToken,
            CatalogClientProperties catalogClientProperties
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(catalogClientProperties.connectTimeout());
        requestFactory.setReadTimeout(catalogClientProperties.readTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(InternalGatewayAccessFilter.HEADER_NAME, gatewayToken);
    }
}
