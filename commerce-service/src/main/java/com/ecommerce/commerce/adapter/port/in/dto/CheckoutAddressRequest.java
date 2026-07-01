package com.ecommerce.commerce.adapter.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutAddressRequest(
        @NotBlank @Size(max = 120) String recipientName,
        @NotBlank @Size(max = 180) String line1,
        @Size(max = 180) String line2,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String region,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotBlank @Size(max = 40) String phone
) {
}
