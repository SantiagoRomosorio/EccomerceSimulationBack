package com.ecommerce.commerce.adapter.port.in.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID productId,
        @Min(1) @Max(1_000) int quantity
) {
}
