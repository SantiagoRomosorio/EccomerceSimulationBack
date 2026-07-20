package com.ecommerce.commerce.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductCatalogPort {

    ProductDetails getProduct(UUID productId);

    record ProductDetails(
            UUID id,
            String sku,
            String name,
            BigDecimal price,
            String currency
    ) {
    }
}
