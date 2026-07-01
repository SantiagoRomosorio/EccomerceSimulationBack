package com.ecommerce.commerce.adapter.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmOrderPaymentRequest(
        @NotBlank @Size(max = 40) String paymentMethod,
        @NotBlank @Size(max = 120) String providerReference
) {
}
