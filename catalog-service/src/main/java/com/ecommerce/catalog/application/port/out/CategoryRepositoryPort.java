package com.ecommerce.catalog.application.port.out;

import com.ecommerce.catalog.domain.model.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {

    Category save(Category category);

    List<Category> findAllCategories();

    Optional<Category> findCategoryById(UUID id);

    boolean existsCategoryBySlug(String slug);
}
