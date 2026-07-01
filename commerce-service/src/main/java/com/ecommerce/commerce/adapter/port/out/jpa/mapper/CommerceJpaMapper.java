package com.ecommerce.commerce.adapter.port.out.jpa.mapper;

import com.ecommerce.commerce.adapter.port.out.jpa.entity.CartEntity;
import com.ecommerce.commerce.adapter.port.out.jpa.entity.CartItemEntity;
import com.ecommerce.commerce.adapter.port.out.jpa.entity.OrderEntity;
import com.ecommerce.commerce.adapter.port.out.jpa.entity.OrderItemEntity;
import com.ecommerce.commerce.domain.model.Cart;
import com.ecommerce.commerce.domain.model.CartItem;
import com.ecommerce.commerce.domain.model.Order;
import com.ecommerce.commerce.domain.model.OrderAddress;
import com.ecommerce.commerce.domain.model.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class CommerceJpaMapper {

    public Cart toDomain(CartEntity entity) {
        return new Cart(entity.getId(), entity.getUserId(), entity.getItems().stream()
                .map(this::toDomain)
                .toList());
    }

    public CartEntity toEntity(Cart cart) {
        CartEntity entity = new CartEntity();
        entity.setId(cart.id());
        entity.setUserId(cart.userId());
        entity.setItems(cart.items().stream()
                .map(item -> toEntity(item, entity))
                .toList());
        return entity;
    }

    public Order toDomain(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getCurrency(),
                entity.getTotal(),
                toShippingAddress(entity),
                toBillingAddress(entity),
                entity.getNotes(),
                entity.getPaymentMethod(),
                entity.getPaymentReference(),
                entity.getPaidAt(),
                entity.getCancellationReason(),
                entity.getCancelledAt(),
                entity.getCreatedAt(),
                entity.getItems().stream().map(this::toDomain).toList()
        );
    }

    public OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.id());
        entity.setUserId(order.userId());
        entity.setStatus(order.status());
        entity.setCurrency(order.currency());
        entity.setTotal(order.total());
        setShippingAddress(entity, order.shippingAddress());
        setBillingAddress(entity, order.billingAddress());
        entity.setNotes(order.notes());
        entity.setPaymentMethod(order.paymentMethod());
        entity.setPaymentReference(order.paymentReference());
        entity.setPaidAt(order.paidAt());
        entity.setCancellationReason(order.cancellationReason());
        entity.setCancelledAt(order.cancelledAt());
        entity.setCreatedAt(order.createdAt());
        entity.setItems(order.items().stream()
                .map(item -> toEntity(item, entity))
                .toList());
        return entity;
    }

    private CartItem toDomain(CartItemEntity entity) {
        return new CartItem(
                entity.getId(),
                entity.getProductId(),
                entity.getSku(),
                entity.getProductName(),
                entity.getUnitPrice(),
                entity.getCurrency(),
                entity.getQuantity()
        );
    }

    private CartItemEntity toEntity(CartItem item, CartEntity cart) {
        CartItemEntity entity = new CartItemEntity();
        entity.setId(item.id());
        entity.setCart(cart);
        entity.setProductId(item.productId());
        entity.setSku(item.sku());
        entity.setProductName(item.productName());
        entity.setUnitPrice(item.unitPrice());
        entity.setCurrency(item.currency());
        entity.setQuantity(item.quantity());
        return entity;
    }

    private OrderItem toDomain(OrderItemEntity entity) {
        return new OrderItem(
                entity.getId(),
                entity.getProductId(),
                entity.getSku(),
                entity.getProductName(),
                entity.getUnitPrice(),
                entity.getCurrency(),
                entity.getQuantity(),
                entity.getLineTotal()
        );
    }

    private OrderItemEntity toEntity(OrderItem item, OrderEntity order) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.setId(item.id());
        entity.setOrder(order);
        entity.setProductId(item.productId());
        entity.setSku(item.sku());
        entity.setProductName(item.productName());
        entity.setUnitPrice(item.unitPrice());
        entity.setCurrency(item.currency());
        entity.setQuantity(item.quantity());
        entity.setLineTotal(item.lineTotal());
        return entity;
    }

    private OrderAddress toShippingAddress(OrderEntity entity) {
        return new OrderAddress(
                entity.getShippingRecipientName(),
                entity.getShippingLine1(),
                entity.getShippingLine2(),
                entity.getShippingCity(),
                entity.getShippingRegion(),
                entity.getShippingPostalCode(),
                entity.getShippingCountry(),
                entity.getShippingPhone()
        );
    }

    private OrderAddress toBillingAddress(OrderEntity entity) {
        return new OrderAddress(
                entity.getBillingRecipientName(),
                entity.getBillingLine1(),
                entity.getBillingLine2(),
                entity.getBillingCity(),
                entity.getBillingRegion(),
                entity.getBillingPostalCode(),
                entity.getBillingCountry(),
                entity.getBillingPhone()
        );
    }

    private void setShippingAddress(OrderEntity entity, OrderAddress address) {
        entity.setShippingRecipientName(address.recipientName());
        entity.setShippingLine1(address.line1());
        entity.setShippingLine2(address.line2());
        entity.setShippingCity(address.city());
        entity.setShippingRegion(address.region());
        entity.setShippingPostalCode(address.postalCode());
        entity.setShippingCountry(address.country());
        entity.setShippingPhone(address.phone());
    }

    private void setBillingAddress(OrderEntity entity, OrderAddress address) {
        entity.setBillingRecipientName(address.recipientName());
        entity.setBillingLine1(address.line1());
        entity.setBillingLine2(address.line2());
        entity.setBillingCity(address.city());
        entity.setBillingRegion(address.region());
        entity.setBillingPostalCode(address.postalCode());
        entity.setBillingCountry(address.country());
        entity.setBillingPhone(address.phone());
    }
}
