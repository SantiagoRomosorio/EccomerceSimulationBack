package com.ecommerce.commerce.application.port.out;

import java.util.List;
import java.util.UUID;

public interface ProductInventoryPort {

    void reserveStock(List<Reservation> reservations);

    void releaseStock(List<Reservation> reservations);

    record Reservation(UUID productId, int quantity) {
    }
}
