package com.ecommerce.catalog.adapter.port.in.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReserveProductStockItemRequest(
        @NotNull UUID productId,
        @Min(1) int quantity
) {
}
