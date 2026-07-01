package com.ecommerce.catalog.adapter.port.out.jpa.repository;

import com.ecommerce.catalog.adapter.port.out.jpa.entity.BrandEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandJpaRepository extends JpaRepository<BrandEntity, UUID> {

    boolean existsBySlug(String slug);
}
