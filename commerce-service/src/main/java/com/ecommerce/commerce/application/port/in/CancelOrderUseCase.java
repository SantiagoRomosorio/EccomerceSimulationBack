package com.ecommerce.commerce.application.port.in;

import com.ecommerce.commerce.domain.model.Order;
import java.util.UUID;

public interface CancelOrderUseCase {
    Order cancelOrder(UUID userId, UUID orderId, Command command);

    record Command(String reason) {
    }
}
