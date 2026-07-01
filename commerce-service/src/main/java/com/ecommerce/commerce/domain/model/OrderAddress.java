package com.ecommerce.commerce.domain.model;

public record OrderAddress(
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
