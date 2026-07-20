package com.ecommerce.commerce.adapter.port.out.jpa;

import com.ecommerce.commerce.adapter.port.out.jpa.repository.OrderJpaRepository;
import com.ecommerce.commerce.application.port.out.OrderTransitionPort;
import com.ecommerce.commerce.domain.exception.PaymentReferenceConflictException;
import com.ecommerce.commerce.domain.model.OrderStatus;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaOrderTransitionAdapter implements OrderTransitionPort {

    private static final String PAYMENT_REFERENCE_UNIQUE_CONSTRAINT =
            "uk_orders_payment_method_reference";

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
        try {
            return orderRepository.confirmPayment(
                    orderId,
                    userId,
                    paymentMethod,
                    paymentReference,
                    paidAt,
                    OrderStatus.PENDING_PAYMENT,
                    OrderStatus.CONFIRMED
            ) == 1;
        } catch (DataIntegrityViolationException exception) {
            if (isPaymentReferenceConflict(exception)) {
                throw new PaymentReferenceConflictException(paymentMethod, paymentReference);
            }
            throw exception;
        }
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

    private boolean isPaymentReferenceConflict(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && PAYMENT_REFERENCE_UNIQUE_CONSTRAINT.equalsIgnoreCase(
                            constraintViolation.getConstraintName()
                    )) {
                return true;
            }

            if (cause instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())
                    && containsConstraintName(sqlException.getMessage())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean containsConstraintName(String message) {
        return message != null
                && message.toLowerCase().contains(PAYMENT_REFERENCE_UNIQUE_CONSTRAINT);
    }
}
