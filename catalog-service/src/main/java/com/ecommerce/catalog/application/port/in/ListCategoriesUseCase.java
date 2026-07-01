package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.Category;
import java.util.List;

public interface ListCategoriesUseCase {

    List<Category> listCategories();
}
