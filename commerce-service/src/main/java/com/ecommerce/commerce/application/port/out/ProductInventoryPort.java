package com.ecommerce.commerce.application.port.out;

import java.util.List;
import java.util.UUID;

public interface ProductInventoryPort {

    void reserveStock(UUID reservationId, List<Reservation> reservations);

    void releaseStock(UUID reservationId);

    record Reservation(UUID productId, int quantity) {
    }
}
