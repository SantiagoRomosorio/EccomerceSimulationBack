package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.Product;
import java.math.BigDecimal;
import java.util.UUID;

public interface CreateProductUseCase {

    Product create(Command command);

    record Command(
            String sku,
            String name,
            String description,
            BigDecimal price,
            String currency,
            UUID categoryId,
            UUID brandId,
            int stockQuantity
    ) {
    }
}
