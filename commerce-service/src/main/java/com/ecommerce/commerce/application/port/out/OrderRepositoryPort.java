package com.ecommerce.commerce.application.port.out;

import com.ecommerce.commerce.domain.model.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    Order save(Order order);

    List<Order> findOrdersByUserId(UUID userId);

    Optional<Order> findByIdAndUserId(UUID orderId, UUID userId);
}
