package com.ecommerce.commerce.adapter.port.out.jpa;

import com.ecommerce.commerce.adapter.port.out.jpa.repository.OrderJpaRepository;
import com.ecommerce.commerce.application.port.out.OrderTransitionPort;
import com.ecommerce.commerce.domain.model.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaOrderTransitionAdapter implements OrderTransitionPort {

    private final OrderJpaRepository orderRepository;

    public JpaOrderTransitionAdapter(OrderJpaRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean confirmPayment(
            UUID orderId,
            UUID userId,
            String paymentMethod,
            String paymentReference,
            Instant paidAt
    ) {
        return orderRepository.confirmPayment(
                orderId,
                userId,
                paymentMethod,
                paymentReference,
                paidAt,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.CONFIRMED
        ) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean beginCancellation(UUID orderId, UUID userId, String cancellationReason) {
        return orderRepository.beginCancellation(
                orderId,
                userId,
                cancellationReason,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.CANCELLATION_PENDING
        ) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean completeCancellation(UUID orderId, UUID userId, Instant cancelledAt) {
        return orderRepository.completeCancellation(
                orderId,
                userId,
                cancelledAt,
                OrderStatus.CANCELLATION_PENDING,
                OrderStatus.CANCELLED
        ) == 1;
    }
}
