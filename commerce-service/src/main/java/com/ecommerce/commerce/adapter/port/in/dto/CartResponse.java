package com.ecommerce.commerce.adapter.port.in.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID userId,
        List<CartItemResponse> items,
        BigDecimal total
) {
}
