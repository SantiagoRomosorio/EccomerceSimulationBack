package com.ecommerce.commerce.config;

import com.ecommerce.commerce.application.port.out.CartRepositoryPort;
import com.ecommerce.commerce.application.port.out.CheckoutPersistencePort;
import com.ecommerce.commerce.application.port.out.OrderRepositoryPort;
import com.ecommerce.commerce.application.port.out.ProductCatalogPort;
import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import com.ecommerce.commerce.application.service.CommerceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    @Bean
    public CommerceService commerceService(
            CartRepositoryPort cartRepository,
            CheckoutPersistencePort checkoutPersistencePort,
            OrderRepositoryPort orderRepository,
            ProductCatalogPort productCatalogPort,
            ProductInventoryPort productInventoryPort
    ) {
        return new CommerceService(
                cartRepository,
                checkoutPersistencePort,
                orderRepository,
                productCatalogPort,
                productInventoryPort
        );
    }
}
