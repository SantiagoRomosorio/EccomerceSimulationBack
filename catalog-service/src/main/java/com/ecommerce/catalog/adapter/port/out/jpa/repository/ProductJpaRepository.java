package com.ecommerce.catalog.adapter.port.out.jpa.repository;

import com.ecommerce.catalog.adapter.port.out.jpa.entity.ProductEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from ProductEntity product where product.id = :id")
    Optional<ProductEntity> findWithLockingById(@Param("id") UUID id);
}
