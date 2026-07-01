package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.Category;

public interface CreateCategoryUseCase {

    Category create(Command command);

    record Command(String name, String slug, String description) {
    }
}
