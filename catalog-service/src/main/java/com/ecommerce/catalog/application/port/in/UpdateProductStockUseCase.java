package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.Product;
import java.util.UUID;

public interface UpdateProductStockUseCase {

    Product updateStock(UUID productId, Command command);

    record Command(int stockQuantity) {
    }
}
