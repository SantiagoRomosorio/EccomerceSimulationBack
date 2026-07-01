package com.ecommerce.commerce.application.port.in;

import com.ecommerce.commerce.domain.model.Order;
import java.util.UUID;

public interface ConfirmOrderPaymentUseCase {
    Order confirmPayment(UUID userId, UUID orderId, Command command);

    record Command(
            String paymentMethod,
            String providerReference
    ) {
    }
}
