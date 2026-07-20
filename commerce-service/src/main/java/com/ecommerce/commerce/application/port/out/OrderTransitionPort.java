package com.ecommerce.commerce.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface OrderTransitionPort {

    boolean confirmPayment(
            UUID orderId,
            UUID userId,
            String paymentMethod,
            String paymentReference,
            Instant paidAt
    );

    boolean beginCancellation(UUID orderId, UUID userId, String cancellationReason);

    boolean completeCancellation(UUID orderId, UUID userId, Instant cancelledAt);
}
