package com.ecommerce.catalog.adapter.port.in.dto;

import jakarta.validation.constraints.Min;

public record UpdateProductStockRequest(
        @Min(0) int stockQuantity
) {
}
