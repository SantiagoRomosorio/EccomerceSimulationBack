package com.ecommerce.commerce.application.service;

import com.ecommerce.commerce.application.port.in.AddCartItemUseCase;
import com.ecommerce.commerce.application.port.in.CheckoutUseCase;
import com.ecommerce.commerce.application.port.in.GetCartUseCase;
import com.ecommerce.commerce.application.port.in.GetOrderUseCase;
import com.ecommerce.commerce.application.port.in.ListOrdersUseCase;
import com.ecommerce.commerce.application.port.in.RemoveCartItemUseCase;
import com.ecommerce.commerce.application.port.in.UpdateCartItemQuantityUseCase;
import com.ecommerce.commerce.application.port.out.CartRepositoryPort;
import com.ecommerce.commerce.application.port.out.OrderRepositoryPort;
import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import com.ecommerce.commerce.domain.exception.InvalidCartException;
import com.ecommerce.commerce.domain.exception.ResourceNotFoundException;
import com.ecommerce.commerce.domain.model.Cart;
import com.ecommerce.commerce.domain.model.CartItem;
import com.ecommerce.commerce.domain.model.Order;
import com.ecommerce.commerce.domain.model.OrderItem;
import com.ecommerce.commerce.domain.model.OrderStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommerceService implements GetCartUseCase, AddCartItemUseCase,
        UpdateCartItemQuantityUseCase, RemoveCartItemUseCase, CheckoutUseCase,
        ListOrdersUseCase, GetOrderUseCase {

    private final CartRepositoryPort cartRepository;
    private final OrderRepositoryPort orderRepository;
    private final ProductInventoryPort productInventoryPort;

    public CommerceService(
            CartRepositoryPort cartRepository,
            OrderRepositoryPort orderRepository,
            ProductInventoryPort productInventoryPort
    ) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.productInventoryPort = productInventoryPort;
    }

    @Override
    public Cart getCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> new Cart(UUID.randomUUID(), userId, List.of()));
    }

    @Override
    public Cart addItem(UUID userId, AddCartItemUseCase.Command command) {
        Cart cart = getCart(userId);
        List<CartItem> items = new ArrayList<>(cart.items());
        boolean merged = false;

        for (int i = 0; i < items.size(); i++) {
            CartItem current = items.get(i);
            if (current.productId().equals(command.productId())) {
                items.set(i, new CartItem(
                        current.id(),
                        current.productId(),
                        command.sku(),
                        command.productName(),
                        command.unitPrice(),
                        command.currency(),
                        current.quantity() + command.quantity()
                ));
                merged = true;
                break;
            }
        }

        if (!merged) {
            items.add(new CartItem(
                    UUID.randomUUID(),
                    command.productId(),
                    command.sku(),
                    command.productName(),
                    command.unitPrice(),
                    command.currency(),
                    command.quantity()
            ));
        }

        return cartRepository.save(new Cart(cart.id(), userId, items));
    }

    @Override
    public Cart updateQuantity(UUID userId, UUID productId, UpdateCartItemQuantityUseCase.Command command) {
        Cart cart = getExistingCart(userId);
        boolean found = cart.items().stream().anyMatch(item -> item.productId().equals(productId));
        if (!found) {
            throw new ResourceNotFoundException("Cart item not found", Map.of("productId", productId));
        }

        List<CartItem> items = cart.items().stream()
                .map(item -> item.productId().equals(productId)
                        ? new CartItem(item.id(), item.productId(), item.sku(), item.productName(),
                        item.unitPrice(), item.currency(), command.quantity())
                        : item)
                .toList();

        return cartRepository.save(new Cart(cart.id(), userId, items));
    }

    @Override
    public Cart removeItem(UUID userId, UUID productId) {
        Cart cart = getExistingCart(userId);
        boolean found = cart.items().stream().anyMatch(item -> item.productId().equals(productId));
        if (!found) {
            throw new ResourceNotFoundException("Cart item not found", Map.of("productId", productId));
        }

        List<CartItem> items = cart.items().stream()
                .filter(item -> !item.productId().equals(productId))
                .toList();
        return cartRepository.save(new Cart(cart.id(), userId, items));
    }

    @Override
    public Order checkout(UUID userId, CheckoutUseCase.Command command) {
        Cart cart = getExistingCart(userId);
        if (cart.items().isEmpty()) {
            throw new InvalidCartException("Cart is empty", Map.of("userId", userId));
        }

        String currency = cart.items().getFirst().currency();
        boolean mixedCurrencies = cart.items().stream().anyMatch(item -> !currency.equals(item.currency()));
        if (mixedCurrencies) {
            throw new InvalidCartException("Cart has mixed currencies", Map.of("userId", userId));
        }

        productInventoryPort.reserveStock(cart.items().stream()
                .map(item -> new ProductInventoryPort.Reservation(item.productId(), item.quantity()))
                .toList());

        List<OrderItem> orderItems = cart.items().stream()
                .map(item -> new OrderItem(
                        UUID.randomUUID(),
                        item.productId(),
                        item.sku(),
                        item.productName(),
                        item.unitPrice(),
                        item.currency(),
                        item.quantity(),
                        item.lineTotal()
                ))
                .toList();

        Order order = orderRepository.save(new Order(
                UUID.randomUUID(),
                userId,
                OrderStatus.PENDING_PAYMENT,
                currency,
                cart.total(),
                command.shippingAddress(),
                command.billingAddress() == null ? command.shippingAddress() : command.billingAddress(),
                command.notes(),
                Instant.now(),
                orderItems
        ));
        cartRepository.deleteById(cart.id());
        return order;
    }

    @Override
    public List<Order> listOrders(UUID userId) {
        return orderRepository.findOrdersByUserId(userId);
    }

    @Override
    public Order getOrder(UUID userId, UUID orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found", Map.of("orderId", orderId)));
    }

    private Cart getExistingCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found", Map.of("userId", userId)));
    }
}
