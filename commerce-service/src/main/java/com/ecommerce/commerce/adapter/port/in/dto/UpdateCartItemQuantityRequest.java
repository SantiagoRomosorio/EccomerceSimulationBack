package com.ecommerce.commerce.adapter.port.in.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemQuantityRequest(@Min(1) int quantity) {
}
