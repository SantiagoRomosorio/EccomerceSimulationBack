package com.ecommerce.commerce.application.port.in;

import com.ecommerce.commerce.domain.model.Cart;
import java.util.UUID;

public interface GetCartUseCase {
    Cart getCart(UUID userId);
}
