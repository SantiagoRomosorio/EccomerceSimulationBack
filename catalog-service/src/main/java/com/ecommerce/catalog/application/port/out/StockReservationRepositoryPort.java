package com.ecommerce.catalog.application.port.out;

import com.ecommerce.catalog.domain.model.StockReservation;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepositoryPort {

    boolean claim(UUID reservationId);

    Optional<StockReservation> findByIdForUpdate(UUID reservationId);

    StockReservation save(StockReservation reservation);
}
