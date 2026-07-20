package com.ecommerce.catalog.config;

import com.ecommerce.catalog.application.port.out.BrandRepositoryPort;
import com.ecommerce.catalog.application.port.out.CategoryRepositoryPort;
import com.ecommerce.catalog.application.port.out.ProductRepositoryPort;
import com.ecommerce.catalog.application.port.out.StockReservationRepositoryPort;
import com.ecommerce.catalog.application.service.CatalogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    @Bean
    public CatalogService catalogService(
            CategoryRepositoryPort categoryRepository,
            BrandRepositoryPort brandRepository,
            ProductRepositoryPort productRepository,
            StockReservationRepositoryPort stockReservationRepository
    ) {
        return new CatalogService(
                categoryRepository,
                brandRepository,
                productRepository,
                stockReservationRepository
        );
    }
}
