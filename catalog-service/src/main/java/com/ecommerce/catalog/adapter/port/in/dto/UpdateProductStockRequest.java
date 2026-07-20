package com.ecommerce.catalog.adapter.port.in.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateProductStockRequest(
        @Min(0) @Max(1_000_000) int stockQuantity
) {
}
