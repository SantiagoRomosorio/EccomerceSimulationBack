package com.ecommerce.commerce.adapter.port.out.jpa;

import com.ecommerce.commerce.adapter.port.out.jpa.entity.CartEntity;
import com.ecommerce.commerce.adapter.port.out.jpa.entity.CartItemEntity;
import com.ecommerce.commerce.adapter.port.out.jpa.mapper.CommerceJpaMapper;
import com.ecommerce.commerce.adapter.port.out.jpa.repository.CartJpaRepository;
import com.ecommerce.commerce.adapter.port.out.jpa.repository.OrderJpaRepository;
import com.ecommerce.commerce.application.port.out.CartRepositoryPort;
import com.ecommerce.commerce.application.port.out.OrderRepositoryPort;
import com.ecommerce.commerce.domain.model.Cart;
import com.ecommerce.commerce.domain.model.CartItem;
import com.ecommerce.commerce.domain.model.Order;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Cart save(Cart cart) {
        CartEntity entity = cartRepository.findById(cart.id())
                .or(() -> cartRepository.findByUserId(cart.userId()))
                .orElseGet(CartEntity::new);

        if (entity.getId() == null) {
            entity.setId(cart.id());
        }

        entity.setUserId(cart.userId());
        syncItems(entity, cart);

        return mapper.toDomain(cartRepository.save(entity));
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

    private void syncItems(CartEntity entity, Cart cart) {
        Set<UUID> incomingProductIds = cart.items().stream()
                .map(CartItem::productId)
                .collect(Collectors.toSet());
        Instant deletedAt = Instant.now();

        entity.getItems().stream()
                .filter(item -> item.getDeletedAt() == null)
                .filter(item -> !incomingProductIds.contains(item.getProductId()))
                .forEach(item -> {
                    item.setDeletedAt(deletedAt);
                    item.setDeletedBy(cart.userId());
                });

        for (CartItem item : cart.items()) {
            CartItemEntity entityItem = findActiveItem(entity, item.productId())
                    .orElseGet(() -> {
                        CartItemEntity newItem = mapper.toEntity(item, entity);
                        entity.getItems().add(newItem);
                        return newItem;
                    });

            mapper.updateEntity(item, entityItem);
        }
    }

    private Optional<CartItemEntity> findActiveItem(CartEntity entity, UUID productId) {
        return entity.getItems().stream()
                .filter(item -> item.getDeletedAt() == null)
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }
}
