package com.ecommerce.catalog.adapter.port.out.jpa.repository;

import com.ecommerce.catalog.adapter.port.out.jpa.entity.StockReservationEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationEntity, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO stock_reservations (reservation_id, status)
            VALUES (:reservationId, 'PENDING')
            ON CONFLICT (reservation_id) DO NOTHING
            """, nativeQuery = true)
    int insertPendingIfAbsent(@Param("reservationId") UUID reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation
            from StockReservationEntity reservation
            where reservation.reservationId = :reservationId
            """)
    Optional<StockReservationEntity> findByIdForUpdate(@Param("reservationId") UUID reservationId);
}
