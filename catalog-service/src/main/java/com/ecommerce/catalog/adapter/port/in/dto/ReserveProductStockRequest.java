package com.ecommerce.catalog.adapter.port.in.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReserveProductStockRequest(
        @NotEmpty List<@Valid ReserveProductStockItemRequest> items
) {
}
