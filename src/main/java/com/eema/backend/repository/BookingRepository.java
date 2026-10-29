package com.eema.backend.repository;

import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.BookingStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByScoutIdOrderByCreatedAtDesc(UUID scoutId);

    List<Booking> findByVenueIdIn(List<UUID> venueIds);

    List<Booking> findByStatus(BookingStatus status);

    Optional<Booking> findByMolliePaymentId(String molliePaymentId);

    @Query(value = "select * from bookings where mollie_fee_reconciled = false and mollie_payment_id is not null and status in ('PAID', 'CONFIRMED') order by paid_at", nativeQuery = true)
    List<Booking> findPendingFeeReconciliation(Pageable pageable);

    boolean existsByMolliePaymentId(String molliePaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from Booking b
            where b.id = :id
            """)
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);
}
