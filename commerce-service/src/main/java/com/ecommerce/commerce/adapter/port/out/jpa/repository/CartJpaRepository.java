package com.ecommerce.commerce.adapter.port.out.jpa.repository;

import com.ecommerce.commerce.adapter.port.out.jpa.entity.CartEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartJpaRepository extends JpaRepository<CartEntity, UUID> {
    Optional<CartEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cart
            from CartEntity cart
            where cart.id = :cartId and cart.userId = :userId
            """)
    Optional<CartEntity> findByIdAndUserIdForUpdate(
            @Param("cartId") UUID cartId,
            @Param("userId") UUID userId
    );
}
