package com.ecommerce.commerce.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record Cart(
        UUID id,
        UUID userId,
        List<CartItem> items
) {
    public BigDecimal total() {
        return items.stream()
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
