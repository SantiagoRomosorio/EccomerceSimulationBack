package com.ecommerce.commerce.application.port.in;

import com.ecommerce.commerce.domain.model.Order;
import java.util.UUID;

public interface GetOrderUseCase {
    Order getOrder(UUID userId, UUID orderId);
}
