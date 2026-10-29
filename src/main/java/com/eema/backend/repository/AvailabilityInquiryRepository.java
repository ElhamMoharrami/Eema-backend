package com.eema.backend.repository;

import com.eema.backend.entity.AvailabilityInquiry;
import com.eema.backend.entity.enums.AvailabilityInquiryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.*;

public interface AvailabilityInquiryRepository extends JpaRepository<AvailabilityInquiry, UUID> {
    List<AvailabilityInquiry> findByScoutIdOrderByCreatedAtDesc(UUID scoutId);
    List<AvailabilityInquiry> findByVenueIdInOrderByCreatedAtDesc(List<UUID> venueIds);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AvailabilityInquiry> findByStatusAndOfferExpiresAtBefore(AvailabilityInquiryStatus status, OffsetDateTime cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from AvailabilityInquiry i where i.id = :id")
    Optional<AvailabilityInquiry> findByIdForUpdate(@Param("id") UUID id);
}
