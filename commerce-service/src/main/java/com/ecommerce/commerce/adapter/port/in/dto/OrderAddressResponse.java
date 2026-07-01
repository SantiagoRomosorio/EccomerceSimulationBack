package com.ecommerce.commerce.adapter.port.in.dto;

public record OrderAddressResponse(
        String recipientName,
        String line1,
        String line2,
        String city,
        String region,
        String postalCode,
        String country,
        String phone
) {
}
