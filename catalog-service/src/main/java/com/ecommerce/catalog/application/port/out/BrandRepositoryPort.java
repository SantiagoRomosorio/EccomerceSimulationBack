package com.ecommerce.catalog.application.port.out;

import com.ecommerce.catalog.domain.model.Brand;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepositoryPort {

    Brand save(Brand brand);

    List<Brand> findAllBrands();

    Optional<Brand> findBrandById(UUID id);

    boolean existsBrandBySlug(String slug);
}
