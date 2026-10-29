package com.eema.backend.repository;

import com.eema.backend.entity.OwnerFinancialLiability;
import com.eema.backend.entity.enums.OwnerFinancialLiabilityStatus;
import com.eema.backend.entity.enums.OwnerFinancialLiabilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface OwnerFinancialLiabilityRepository extends JpaRepository<OwnerFinancialLiability, UUID> {
    Optional<OwnerFinancialLiability> findByBookingIdAndType(UUID bookingId, OwnerFinancialLiabilityType type);
    List<OwnerFinancialLiability> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);
    long countByStatus(OwnerFinancialLiabilityStatus status);

    @Query("""
            select l from OwnerFinancialLiability l
            where (:status is null or l.status = :status)
              and (:type is null or l.type = :type)
              and (:ownerId is null or l.ownerId = :ownerId)
              and (:bookingId is null or l.bookingId = :bookingId)
            order by case l.status
                when com.eema.backend.entity.enums.OwnerFinancialLiabilityStatus.OPEN then 0
                else 1 end, l.createdAt asc
            """)
    Page<OwnerFinancialLiability> search(@Param("status") OwnerFinancialLiabilityStatus status,
            @Param("type") OwnerFinancialLiabilityType type, @Param("ownerId") UUID ownerId,
            @Param("bookingId") UUID bookingId, Pageable pageable);
    List<OwnerFinancialLiability> findByStatusOrderByCreatedAtAsc(
            OwnerFinancialLiabilityStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from OwnerFinancialLiability l where l.id = :id")
    Optional<OwnerFinancialLiability> findByIdForUpdate(@Param("id") UUID id);
}
