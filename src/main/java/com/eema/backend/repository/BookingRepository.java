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
import org.springframework.data.domain.Page;
import com.eema.backend.entity.enums.PaymentCreationStatus;
import com.eema.backend.entity.enums.RefundStatus;
import java.time.OffsetDateTime;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Booking> {

    default Page<Booking> adminSearch(UUID bookingId, UUID venueId, UUID scoutId,
            BookingStatus status, String molliePaymentId, RefundStatus refundStatus,
            PaymentCreationStatus paymentCreationStatus, Pageable pageable) {
        return findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (bookingId != null) predicates.add(cb.equal(root.get("id"), bookingId));
            if (venueId != null) predicates.add(cb.equal(root.get("venueId"), venueId));
            if (scoutId != null) predicates.add(cb.equal(root.get("scoutId"), scoutId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (molliePaymentId != null) predicates.add(cb.equal(root.get("molliePaymentId"), molliePaymentId));
            if (refundStatus != null) predicates.add(cb.equal(root.get("refundStatus"), refundStatus));
            if (paymentCreationStatus != null) predicates.add(cb.equal(root.get("paymentCreationStatus"), paymentCreationStatus));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
    }

    @Query("select b from Booking b where b.paymentCreationStatus = :status and b.paymentCreationStartedAt < :cutoff order by b.paymentCreationStartedAt")
    List<Booking> findStuckPaymentCreations(@Param("status") PaymentCreationStatus status,
            @Param("cutoff") OffsetDateTime cutoff, Pageable pageable);

    @Query("select b from Booking b where b.refundStatus in :statuses and b.refundRequestedAt < :cutoff order by b.refundRequestedAt")
    List<Booking> findStuckRefunds(@Param("statuses") List<RefundStatus> statuses,
            @Param("cutoff") OffsetDateTime cutoff, Pageable pageable);

    @Query(value = "select * from bookings where mollie_fee_reconciled = false and total_paid_eur > 0 and paid_at < :cutoff and status in ('CONFIRMED', 'CANCELLED', 'REFUNDED') order by paid_at", nativeQuery = true)
    List<Booking> findOldUnreconciledMollieFees(@Param("cutoff") OffsetDateTime cutoff, Pageable pageable);

    @Query("select b from Booking b where b.latePaymentDetectedAt is not null and b.refundStatus in :statuses order by b.latePaymentDetectedAt")
    List<Booking> findLatePaymentRefundsInProgress(@Param("statuses") List<RefundStatus> statuses, Pageable pageable);

    List<Booking> findByScoutIdOrderByCreatedAtDesc(UUID scoutId);

    List<Booking> findByVenueIdIn(List<UUID> venueIds);

    Optional<Booking> findByMolliePaymentId(String molliePaymentId);

    @Query("select b.id from Booking b where b.molliePaymentId = :paymentId")
    Optional<UUID> findIdByMolliePaymentId(@Param("paymentId") String paymentId);

    @Query(value = """
            select * from bookings
            where mollie_fee_reconciled = false and mollie_payment_id is not null
              and status in ('CONFIRMED', 'CANCELLED', 'REFUNDED') and total_paid_eur > 0
            order by coalesce(paid_at, created_at), id
            """, nativeQuery = true)
    List<Booking> findPendingFeeReconciliation(Pageable pageable);

    @Query(value = """
            select * from bookings
            where mollie_fee_reconciled = false and mollie_payment_id is not null
              and status in ('CONFIRMED', 'CANCELLED', 'REFUNDED') and total_paid_eur > 0
              and (coalesce(paid_at, created_at), id) > (:afterTime, :afterId)
            order by coalesce(paid_at, created_at), id
            """, nativeQuery = true)
    List<Booking> findPendingFeeReconciliationAfter(@Param("afterTime") OffsetDateTime afterTime,
            @Param("afterId") UUID afterId, Pageable pageable);


    @Query(value = """
            select * from bookings
            where venue_id = :venueId and shoot_date = :date
              and status in ('AWAITING_PAYMENT', 'CONFIRMED')
            order by start_time
            """, nativeQuery = true)
    List<Booking> findBlockingBookings(@Param("venueId") UUID venueId, @Param("date") LocalDate date);

    @Query(value = """
            select * from bookings where status = 'AWAITING_PAYMENT'
              and payment_expires_at <= :cutoff
            order by payment_expires_at for update skip locked limit :batchSize
            """, nativeQuery = true)
    List<Booking> lockDueAwaitingPaymentBookings(@Param("cutoff") OffsetDateTime cutoff,
                                                  @Param("batchSize") int batchSize);

    long countByPaymentCreationStatusAndPaymentCreationStartedAtBefore(
            PaymentCreationStatus status, OffsetDateTime cutoff);

    long countByRefundStatusInAndRefundRequestedAtBefore(
            List<RefundStatus> statuses, OffsetDateTime cutoff);

    @Query(value = """
            select count(*) from bookings
            where status in ('CONFIRMED', 'CANCELLED', 'REFUNDED')
              and total_paid_eur > 0 and mollie_fee_reconciled = false and paid_at < :cutoff
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
