package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.Product;
import java.util.List;
import java.util.UUID;

public interface ReleaseProductStockUseCase {
    List<Product> releaseStock(List<Command> commands);

    record Command(UUID productId, int quantity) {
    }
}
