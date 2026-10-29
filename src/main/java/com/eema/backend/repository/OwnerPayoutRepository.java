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

public interface OwnerPayoutRepository extends JpaRepository<OwnerPayout, UUID> {
    Optional<OwnerPayout> findByBookingId(UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from OwnerPayout p where p.id = :id")
    Optional<OwnerPayout> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select p from OwnerPayout p
            where (:status is null or p.status = :status)
              and (:ownerId is null or p.ownerId = :ownerId)
              and (:bookingId is null or p.bookingId = :bookingId)
              and (:venueId is null or p.venueId = :venueId)
            order by case p.status
                when com.eema.backend.entity.enums.OwnerPayoutStatus.READY then 0
                when com.eema.backend.entity.enums.OwnerPayoutStatus.HELD then 1
                when com.eema.backend.entity.enums.OwnerPayoutStatus.CALCULATED then 2
                when com.eema.backend.entity.enums.OwnerPayoutStatus.PAID then 3
                else 4 end,
                p.readyAt asc nulls last, p.createdAt desc
            """)
    Page<OwnerPayout> search(@Param("status") OwnerPayoutStatus status,
            @Param("ownerId") UUID ownerId, @Param("bookingId") UUID bookingId,
            @Param("venueId") UUID venueId, Pageable pageable);

    @Query(value = """
            select * from owner_payouts
            where status in ('CALCULATED','HELD') and eligible_at <= :now
            order by eligible_at for update skip locked limit :limit
            """, nativeQuery = true)
    List<OwnerPayout> lockDueForReadiness(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    long countByStatus(OwnerPayoutStatus status);
    long countByStatusAndReadyAtBefore(OwnerPayoutStatus status, OffsetDateTime cutoff);
    List<OwnerPayout> findByStatusOrderByCreatedAtAsc(OwnerPayoutStatus status, Pageable pageable);
    List<OwnerPayout> findByStatusAndReadyAtBeforeOrderByReadyAtAsc(
            OwnerPayoutStatus status, OffsetDateTime cutoff, Pageable pageable);
}
