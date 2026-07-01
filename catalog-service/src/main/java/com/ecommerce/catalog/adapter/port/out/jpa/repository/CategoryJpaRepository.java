package com.ecommerce.catalog.adapter.port.out.jpa.repository;

import com.ecommerce.catalog.adapter.port.out.jpa.entity.CategoryEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {

    boolean existsBySlug(String slug);
}
