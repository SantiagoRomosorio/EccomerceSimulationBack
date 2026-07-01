package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.Product;
import java.util.List;

public interface ListProductsUseCase {

    List<Product> listProducts();
}
