package com.ecommerce.commerce.adapter.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotBlank @Size(max = 300) String reason
) {
}
