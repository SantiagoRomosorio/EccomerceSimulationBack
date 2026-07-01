package com.ecommerce.catalog.application.port.out;

import com.ecommerce.catalog.domain.model.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {

    Product save(Product product);

    List<Product> findAllProducts();

    Optional<Product> findProductById(UUID id);

    Optional<Product> findProductByIdForUpdate(UUID id);

    boolean existsBySku(String sku);
}
