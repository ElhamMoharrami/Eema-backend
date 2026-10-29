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

public interface OwnerFinancialLiabilityRepository extends JpaRepository<OwnerFinancialLiability, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<OwnerFinancialLiability> {
    Optional<OwnerFinancialLiability> findByBookingIdAndType(UUID bookingId, OwnerFinancialLiabilityType type);
    List<OwnerFinancialLiability> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);
    long countByStatus(OwnerFinancialLiabilityStatus status);

    default Page<OwnerFinancialLiability> search(OwnerFinancialLiabilityStatus status,
            OwnerFinancialLiabilityType type, UUID ownerId, UUID bookingId, Pageable pageable) {
        return findAll((root, query, cb) -> {
            var filters = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) filters.add(cb.equal(root.get("status"), status));
            if (type != null) filters.add(cb.equal(root.get("type"), type));
            if (ownerId != null) filters.add(cb.equal(root.get("ownerId"), ownerId));
            if (bookingId != null) filters.add(cb.equal(root.get("bookingId"), bookingId));
            if (query.getResultType() != Long.class) {
                var priority = cb.<Integer>selectCase()
                        .when(cb.equal(root.get("status"), OwnerFinancialLiabilityStatus.OPEN), 0).otherwise(1);
                query.orderBy(cb.asc(priority), cb.asc(root.get("createdAt")));
            }
            return cb.and(filters.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable);
    }
    List<OwnerFinancialLiability> findByStatusOrderByCreatedAtAsc(
            OwnerFinancialLiabilityStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from OwnerFinancialLiability l where l.id = :id")
    Optional<OwnerFinancialLiability> findByIdForUpdate(@Param("id") UUID id);
}
