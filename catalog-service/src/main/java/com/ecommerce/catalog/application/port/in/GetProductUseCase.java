package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.Product;
import java.util.UUID;

public interface GetProductUseCase {

    Product getById(UUID id);
}
