package com.ecommerce.commerce.config;

import com.ecommerce.commerce.application.port.out.CartRepositoryPort;
import com.ecommerce.commerce.application.port.out.OrderRepositoryPort;
import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import com.ecommerce.commerce.application.service.CommerceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    @Bean
    public CommerceService commerceService(
            CartRepositoryPort cartRepository,
            OrderRepositoryPort orderRepository,
            ProductInventoryPort productInventoryPort
    ) {
        return new CommerceService(cartRepository, orderRepository, productInventoryPort);
    }
}
