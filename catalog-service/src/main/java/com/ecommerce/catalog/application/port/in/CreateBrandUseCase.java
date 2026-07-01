package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.Brand;

public interface CreateBrandUseCase {

    Brand create(Command command);

    record Command(String name, String slug, String description) {
    }
}
