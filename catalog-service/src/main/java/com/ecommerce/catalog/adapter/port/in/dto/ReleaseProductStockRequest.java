package com.ecommerce.catalog.adapter.port.in.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReleaseProductStockRequest(
        @NotNull UUID reservationId
) {
}
