package com.ecommerce.commerce.adapter.port.out.jpa.repository;

import com.ecommerce.commerce.adapter.port.out.jpa.entity.OrderEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);
}
