package com.ecommerce.commerce.adapter.port.out.jpa.repository;

import com.ecommerce.commerce.adapter.port.out.jpa.entity.OrderEntity;
import com.ecommerce.commerce.domain.model.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OrderEntity orders
               set orders.status = :confirmedStatus,
                   orders.paymentMethod = :paymentMethod,
                   orders.paymentReference = :paymentReference,
                   orders.paidAt = :paidAt
             where orders.id = :orderId
               and orders.userId = :userId
               and orders.status = :pendingStatus
            """)
    int confirmPayment(
            @Param("orderId") UUID orderId,
            @Param("userId") UUID userId,
            @Param("paymentMethod") String paymentMethod,
            @Param("paymentReference") String paymentReference,
            @Param("paidAt") Instant paidAt,
            @Param("pendingStatus") OrderStatus pendingStatus,
            @Param("confirmedStatus") OrderStatus confirmedStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OrderEntity orders
               set orders.status = :cancellationPendingStatus,
                   orders.cancellationReason = :cancellationReason
             where orders.id = :orderId
               and orders.userId = :userId
               and orders.status = :pendingStatus
            """)
    int beginCancellation(
            @Param("orderId") UUID orderId,
            @Param("userId") UUID userId,
            @Param("cancellationReason") String cancellationReason,
            @Param("pendingStatus") OrderStatus pendingStatus,
            @Param("cancellationPendingStatus") OrderStatus cancellationPendingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OrderEntity orders
               set orders.status = :cancelledStatus,
                   orders.cancelledAt = :cancelledAt
             where orders.id = :orderId
               and orders.userId = :userId
               and orders.status = :cancellationPendingStatus
            """)
    int completeCancellation(
            @Param("orderId") UUID orderId,
            @Param("userId") UUID userId,
            @Param("cancelledAt") Instant cancelledAt,
            @Param("cancellationPendingStatus") OrderStatus cancellationPendingStatus,
            @Param("cancelledStatus") OrderStatus cancelledStatus
    );
}
