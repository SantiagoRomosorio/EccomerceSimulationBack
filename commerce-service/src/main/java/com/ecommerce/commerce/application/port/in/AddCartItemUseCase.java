package com.ecommerce.commerce.application.port.in;

import com.ecommerce.commerce.domain.model.Cart;
import java.math.BigDecimal;
import java.util.UUID;

public interface AddCartItemUseCase {
    Cart addItem(UUID userId, Command command);

    record Command(
            UUID productId,
            String sku,
            String productName,
            BigDecimal unitPrice,
            String currency,
            int quantity
    ) {
    }
}
