package com.ecommerce.commerce.application.port.out;

import com.ecommerce.commerce.domain.model.Cart;
import com.ecommerce.commerce.domain.model.Order;

public interface CheckoutPersistencePort {

    Order saveOrderAndDeleteCart(Order order, Cart expectedCart);
}
