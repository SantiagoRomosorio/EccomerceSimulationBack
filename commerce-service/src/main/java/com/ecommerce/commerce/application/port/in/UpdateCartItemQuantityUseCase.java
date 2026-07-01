package com.ecommerce.commerce.application.port.in;

import com.ecommerce.commerce.domain.model.Cart;
import java.util.UUID;

public interface UpdateCartItemQuantityUseCase {
    Cart updateQuantity(UUID userId, UUID productId, Command command);

    record Command(int quantity) {
    }
}
