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

    @Query("""
            select b from Booking b
            where b.venueId = :venueId and b.shootDate = :date
              and b.status in (com.eema.backend.entity.enums.BookingStatus.PENDING,
                               com.eema.backend.entity.enums.BookingStatus.REQUESTED,
                               com.eema.backend.entity.enums.BookingStatus.AWAITING_PAYMENT,
                               com.eema.backend.entity.enums.BookingStatus.PAID,
                               com.eema.backend.entity.enums.BookingStatus.CONFIRMED)
            order by b.startTime
            """)
    List<Booking> findBlockingBookings(@Param("venueId") UUID venueId, @Param("date") LocalDate date);

    List<Booking> findByStatusAndPaymentExpiresAtBefore(BookingStatus status, java.time.OffsetDateTime cutoff);

    long countByPaymentCreationStatusAndPaymentCreationStartedAtBefore(
            PaymentCreationStatus status, OffsetDateTime cutoff);

    long countByRefundStatusInAndRefundRequestedAtBefore(
            List<RefundStatus> statuses, OffsetDateTime cutoff);

    @Query("""
            select count(b) from Booking b
            where b.status in (com.eema.backend.entity.enums.BookingStatus.PAID,
                               com.eema.backend.entity.enums.BookingStatus.CONFIRMED)
              and b.mollieFeeReconciled = false
              and b.paidAt < :cutoff
            """)
    long countOldUnreconciledMollieFees(@Param("cutoff") OffsetDateTime cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b
            from Booking b
            where b.id = :id
            """)
    Optional<Booking> findByIdForUpdate(@Param("id") UUID id);
}
