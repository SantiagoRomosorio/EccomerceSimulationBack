package com.ecommerce.commerce.adapter.port.out.jpa.repository;

import com.ecommerce.commerce.adapter.port.out.jpa.entity.CartEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartJpaRepository extends JpaRepository<CartEntity, UUID> {
    Optional<CartEntity> findByUserId(UUID userId);
}
