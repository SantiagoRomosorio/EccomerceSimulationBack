package com.ecommerce.commerce.adapter.port.out.jpa;

import com.ecommerce.commerce.adapter.port.out.jpa.mapper.CommerceJpaMapper;
import com.ecommerce.commerce.adapter.port.out.jpa.repository.CartJpaRepository;
import com.ecommerce.commerce.adapter.port.out.jpa.repository.OrderJpaRepository;
import com.ecommerce.commerce.application.port.out.CartRepositoryPort;
import com.ecommerce.commerce.application.port.out.OrderRepositoryPort;
import com.ecommerce.commerce.domain.model.Cart;
import com.ecommerce.commerce.domain.model.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CommercePersistenceAdapter implements CartRepositoryPort, OrderRepositoryPort {

    private final CartJpaRepository cartRepository;
    private final OrderJpaRepository orderRepository;
    private final CommerceJpaMapper mapper;

    public CommercePersistenceAdapter(
            CartJpaRepository cartRepository,
            OrderJpaRepository orderRepository,
            CommerceJpaMapper mapper
    ) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Cart> findByUserId(UUID userId) {
        return cartRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Cart save(Cart cart) {
        return mapper.toDomain(cartRepository.save(mapper.toEntity(cart)));
    }

    @Override
    public void deleteById(UUID cartId) {
        cartRepository.deleteById(cartId);
    }

    @Override
    public Order save(Order order) {
        return mapper.toDomain(orderRepository.save(mapper.toEntity(order)));
    }

    @Override
    public List<Order> findOrdersByUserId(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Order> findByIdAndUserId(UUID orderId, UUID userId) {
        return orderRepository.findByIdAndUserId(orderId, userId).map(mapper::toDomain);
    }
}
