package com.ecommerce.commerce.adapter.port.in.mapper;

import com.ecommerce.commerce.adapter.port.in.dto.CartItemResponse;
import com.ecommerce.commerce.adapter.port.in.dto.CartResponse;
import com.ecommerce.commerce.adapter.port.in.dto.CheckoutAddressRequest;
import com.ecommerce.commerce.adapter.port.in.dto.OrderItemResponse;
import com.ecommerce.commerce.adapter.port.in.dto.OrderAddressResponse;
import com.ecommerce.commerce.adapter.port.in.dto.OrderResponse;
import com.ecommerce.commerce.domain.model.Cart;
import com.ecommerce.commerce.domain.model.CartItem;
import com.ecommerce.commerce.domain.model.Order;
import com.ecommerce.commerce.domain.model.OrderAddress;
import com.ecommerce.commerce.domain.model.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class CommerceDtoMapper {

    public CartResponse toResponse(Cart cart) {
        return new CartResponse(
                cart.id(),
                cart.userId(),
                cart.items().stream().map(this::toResponse).toList(),
                cart.total()
        );
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.id(),
                order.userId(),
                order.status(),
                order.currency(),
                order.total(),
                toResponse(order.shippingAddress()),
                toResponse(order.billingAddress()),
                order.notes(),
                order.paymentMethod(),
                order.paymentReference(),
                order.paidAt(),
                order.createdAt(),
                order.items().stream().map(this::toResponse).toList()
        );
    }

    public OrderAddress toDomain(CheckoutAddressRequest request) {
        if (request == null) {
            return null;
        }

        return new OrderAddress(
                request.recipientName(),
                request.line1(),
                request.line2(),
                request.city(),
                request.region(),
                request.postalCode(),
                request.country(),
                request.phone()
        );
    }

    private CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.id(),
                item.productId(),
                item.sku(),
                item.productName(),
                item.unitPrice(),
                item.currency(),
                item.quantity(),
                item.lineTotal()
        );
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.id(),
                item.productId(),
                item.sku(),
                item.productName(),
                item.unitPrice(),
                item.currency(),
                item.quantity(),
                item.lineTotal()
        );
    }

    private OrderAddressResponse toResponse(OrderAddress address) {
        return new OrderAddressResponse(
                address.recipientName(),
                address.line1(),
                address.line2(),
                address.city(),
                address.region(),
                address.postalCode(),
                address.country(),
                address.phone()
        );
    }
}
