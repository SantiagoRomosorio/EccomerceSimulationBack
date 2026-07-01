package com.ecommerce.commerce.adapter.port.in.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotNull @Valid CheckoutAddressRequest shippingAddress,
        @Valid CheckoutAddressRequest billingAddress,
        @Size(max = 500) String notes
) {
}
