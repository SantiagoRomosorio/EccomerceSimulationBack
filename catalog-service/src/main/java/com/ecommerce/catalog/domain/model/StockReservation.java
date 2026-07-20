package com.ecommerce.catalog.domain.model;

import java.util.List;
import java.util.UUID;

public record StockReservation(
        UUID reservationId,
        Status status,
        List<Item> items
) {

    public StockReservation {
        items = List.copyOf(items);
    }

    public StockReservation withStatus(Status newStatus) {
        return new StockReservation(reservationId, newStatus, items);
    }

    public enum Status {
        PENDING,
        RESERVED,
        RELEASED
    }

    public record Item(UUID productId, int quantity) {
    }
}
