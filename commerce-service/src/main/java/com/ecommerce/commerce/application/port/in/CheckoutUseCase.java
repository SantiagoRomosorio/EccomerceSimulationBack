package com.ecommerce.commerce.application.port.in;

import com.ecommerce.commerce.domain.model.Order;
import com.ecommerce.commerce.domain.model.OrderAddress;
import java.util.UUID;

public interface CheckoutUseCase {
    Order checkout(UUID userId, Command command);

    record Command(
            OrderAddress shippingAddress,
            OrderAddress billingAddress,
            String notes
    ) {
    }
}
