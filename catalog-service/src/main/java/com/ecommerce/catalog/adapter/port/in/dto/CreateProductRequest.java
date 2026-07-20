package com.ecommerce.catalog.adapter.port.in.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 180) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull UUID categoryId,
        @NotNull UUID brandId,
        @Min(0) @Max(1_000_000) int stockQuantity
) {
}
