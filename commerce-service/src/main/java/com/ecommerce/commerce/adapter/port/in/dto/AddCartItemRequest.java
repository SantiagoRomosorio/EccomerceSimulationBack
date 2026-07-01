package com.ecommerce.commerce.adapter.port.in.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID productId,
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 180) String productName,
        @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Min(1) int quantity
) {
}
