package com.ecommerce.catalog.adapter.port.out.jpa;

import com.ecommerce.catalog.adapter.port.out.jpa.entity.StockReservationEntity;
import com.ecommerce.catalog.adapter.port.out.jpa.entity.StockReservationItemEntity;
import com.ecommerce.catalog.adapter.port.out.jpa.repository.StockReservationJpaRepository;
import com.ecommerce.catalog.application.port.out.StockReservationRepositoryPort;
import com.ecommerce.catalog.domain.model.StockReservation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StockReservationPersistenceAdapter implements StockReservationRepositoryPort {

    private final StockReservationJpaRepository repository;

    public StockReservationPersistenceAdapter(StockReservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean claim(UUID reservationId) {
        return repository.insertPendingIfAbsent(reservationId) == 1;
    }

    @Override
    public Optional<StockReservation> findByIdForUpdate(UUID reservationId) {
        return repository.findByIdForUpdate(reservationId).map(this::toDomain);
    }

    @Override
    public StockReservation save(StockReservation reservation) {
        return toDomain(repository.save(toEntity(reservation)));
    }

    private StockReservationEntity toEntity(StockReservation reservation) {
        StockReservationEntity entity = new StockReservationEntity();
        entity.setReservationId(reservation.reservationId());
        entity.setStatus(reservation.status());
        entity.setItems(reservation.items().stream().map(this::toEntity).toList());
        return entity;
    }

    private StockReservationItemEntity toEntity(StockReservation.Item item) {
        StockReservationItemEntity entity = new StockReservationItemEntity();
        entity.setProductId(item.productId());
        entity.setQuantity(item.quantity());
        return entity;
    }

    private StockReservation toDomain(StockReservationEntity entity) {
        return new StockReservation(
                entity.getReservationId(),
                entity.getStatus(),
                entity.getItems().stream()
                        .map(item -> new StockReservation.Item(item.getProductId(), item.getQuantity()))
                        .toList()
        );
    }
}
