package com.ecommerce.commerce.application.port.in;

import com.ecommerce.commerce.domain.model.Cart;
import java.util.UUID;

public interface AddCartItemUseCase {
    Cart addItem(UUID userId, Command command);

    record Command(
            UUID productId,
            int quantity
    ) {
    }
}
