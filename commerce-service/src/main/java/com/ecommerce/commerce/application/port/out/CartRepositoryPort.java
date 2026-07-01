package com.ecommerce.commerce.application.port.out;

import com.ecommerce.commerce.domain.model.Cart;
import java.util.Optional;
import java.util.UUID;

public interface CartRepositoryPort {
    Optional<Cart> findByUserId(UUID userId);

    Cart save(Cart cart);

    void deleteById(UUID cartId);
}
