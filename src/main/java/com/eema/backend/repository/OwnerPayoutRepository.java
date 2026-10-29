package com.eema.backend.repository;

import com.eema.backend.entity.OwnerPayout;
import com.eema.backend.entity.enums.OwnerPayoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.*;

public interface OwnerPayoutRepository extends JpaRepository<OwnerPayout, UUID>, JpaSpecificationExecutor<OwnerPayout> {
    Optional<OwnerPayout> findByBookingId(UUID bookingId);

    @org.springframework.data.jpa.repository.Query("select p.bookingId from OwnerPayout p where p.id = :id")
    Optional<UUID> findBookingIdById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from OwnerPayout p where p.id = :id")
    Optional<OwnerPayout> findByIdForUpdate(@Param("id") UUID id);

    default Page<OwnerPayout> search(OwnerPayoutStatus status, UUID ownerId, UUID bookingId,
                                    UUID venueId, Pageable pageable) {
        return findAll((root, query, cb) -> {
            var filters = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) filters.add(cb.equal(root.get("status"), status));
            if (ownerId != null) filters.add(cb.equal(root.get("ownerId"), ownerId));
            if (bookingId != null) filters.add(cb.equal(root.get("bookingId"), bookingId));
            if (venueId != null) filters.add(cb.equal(root.get("venueId"), venueId));
            if (query.getResultType() != Long.class) {
                var priority = cb.<OwnerPayoutStatus, Integer>selectCase(root.get("status"))
                        .when(OwnerPayoutStatus.READY, 0).when(OwnerPayoutStatus.HELD, 1)
                        .when(OwnerPayoutStatus.CALCULATED, 2).when(OwnerPayoutStatus.PAID, 3).otherwise(4);
                query.orderBy(cb.asc(priority), cb.asc(root.get("readyAt")), cb.desc(root.get("createdAt")));
            }
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable);
    }

    @Query(value = """
            select b.id from bookings b join owner_payouts p on p.booking_id = b.id
            where p.status in ('CALCULATED','HELD') and p.eligible_at <= :now
            order by p.eligible_at, b.id for update of b skip locked limit :limit
            """, nativeQuery = true)
    List<UUID> lockBookingIdsDueForReadiness(@Param("now") OffsetDateTime now, @Param("limit") int limit);


    interface ReadinessCandidate {
        UUID getBookingId();
        java.time.Instant getEligibleAt();
    }

    @Query(value = """
            select b.id as "bookingId", p.eligible_at as "eligibleAt"
            from bookings b join owner_payouts p on p.booking_id = b.id
            where p.status in ('CALCULATED','HELD') and p.eligible_at <= :now
              and (cast(:afterTime as timestamptz) is null
                   or (p.eligible_at, b.id) > (cast(:afterTime as timestamptz), cast(:afterId as uuid)))
            order by p.eligible_at, b.id for update of b skip locked limit :limit
            """, nativeQuery = true)
    List<ReadinessCandidate> lockReadinessCandidatesAfter(@Param("now") OffsetDateTime now,
            @Param("afterTime") OffsetDateTime afterTime, @Param("afterId") UUID afterId,
            @Param("limit") int limit);

    long countByStatus(OwnerPayoutStatus status);
    long countByStatusAndReadyAtBefore(OwnerPayoutStatus status, OffsetDateTime cutoff);
    List<OwnerPayout> findByStatusOrderByCreatedAtAsc(OwnerPayoutStatus status, Pageable pageable);
    List<OwnerPayout> findByStatusAndReadyAtBeforeOrderByReadyAtAsc(
            OwnerPayoutStatus status, OffsetDateTime cutoff, Pageable pageable);

    @Query("select p from OwnerPayout p where p.ownerId = :ownerId order by p.createdAt desc")
    Page<OwnerPayout> findOwnerPayouts(@Param("ownerId") UUID ownerId, Pageable pageable);
    List<OwnerPayout> findByOwnerIdAndStatus(UUID ownerId, OwnerPayoutStatus status);
}
