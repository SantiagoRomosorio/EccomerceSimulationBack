package com.ecommerce.catalog.adapter.port.out.jpa.entity;

import com.ecommerce.catalog.domain.model.StockReservation;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
public class StockReservationEntity {

    @Id
    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StockReservation.Status status;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "stock_reservation_items",
            joinColumns = @JoinColumn(name = "reservation_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_stock_reservation_product",
                    columnNames = {"reservation_id", "product_id"}
            )
    )
    @OrderColumn(name = "item_order")
    private List<StockReservationItemEntity> items = new ArrayList<>();

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public StockReservation.Status getStatus() {
        return status;
    }

    public void setStatus(StockReservation.Status status) {
        this.status = status;
    }

    public List<StockReservationItemEntity> getItems() {
        return items;
    }

    public void setItems(List<StockReservationItemEntity> items) {
        this.items = new ArrayList<>(items);
    }
}
