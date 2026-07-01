package com.ecommerce.commerce.application.port.in;

import com.ecommerce.commerce.domain.model.Order;
import java.util.List;
import java.util.UUID;

public interface ListOrdersUseCase {
    List<Order> listOrders(UUID userId);
}
