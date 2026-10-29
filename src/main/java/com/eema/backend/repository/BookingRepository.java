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
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import com.eema.backend.entity.enums.PaymentCreationStatus;
import com.eema.backend.entity.enums.RefundStatus;
import java.time.OffsetDateTime;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByScoutIdOrderByCreatedAtDesc(UUID scoutId);

    List<Booking> findByVenueIdIn(List<UUID> venueIds);

    List<Booking> findByStatus(BookingStatus status);

    Optional<Booking> findByMolliePaymentId(String molliePaymentId);

    @Query(value = "select * from bookings where mollie_fee_reconciled = false and mollie_payment_id is not null and status in ('PAID', 'CONFIRMED') order by paid_at", nativeQuery = true)
    List<Booking> findPendingFeeReconciliation(Pageable pageable);

    boolean existsByMolliePaymentId(String molliePaymentId);

    @Query(value = """
            select * from bookings
            where venue_id = :venueId and shoot_date = :date
              and status in ('PENDING', 'AWAITING_PAYMENT', 'PAID', 'CONFIRMED')
            order by start_time
            """, nativeQuery = true)
    List<Booking> findBlockingBookings(@Param("venueId") UUID venueId, @Param("date") LocalDate date);

    List<Booking> findByStatusAndPaymentExpiresAtBefore(BookingStatus status, java.time.OffsetDateTime cutoff);

    long countByPaymentCreationStatusAndPaymentCreationStartedAtBefore(
            PaymentCreationStatus status, OffsetDateTime cutoff);

    long countByRefundStatusInAndRefundRequestedAtBefore(
            List<RefundStatus> statuses, OffsetDateTime cutoff);

    @Query(value = """
            select count(*) from bookings
            where status in ('PAID', 'CONFIRMED')
              and mollie_fee_reconciled = false and paid_at < :cutoff
            """, nativeQuery = true)
    long countOldUnreconciledMollieFees(@Param("cutoff") OffsetDateTime cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from Booking b
            where b.id = :id
            """)
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
            select * from bookings
            where status = CAST('REQUESTED' AS booking_status)
              and owner_response_expires_at <= :now
            order by owner_response_expires_at
            for update skip locked
            limit :limit
            """, nativeQuery = true)
    List<Booking> lockDueRequestedBookings(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    @Query(value = """
            select * from bookings
            where venue_id = :venueId and shoot_date = :date
              and status = CAST('REQUESTED' AS booking_status) and id <> :winnerId
              and start_time < :endTime and end_time > :startTime
            for update
            """, nativeQuery = true)
    List<Booking> findOverlappingRequestedForUpdate(@Param("venueId") UUID venueId,
            @Param("date") LocalDate date, @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime, @Param("winnerId") UUID winnerId);
}
