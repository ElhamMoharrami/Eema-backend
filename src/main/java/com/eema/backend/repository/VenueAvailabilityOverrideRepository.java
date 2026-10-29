package com.eema.backend.repository;

import com.eema.backend.entity.VenueAvailabilityOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VenueAvailabilityOverrideRepository extends JpaRepository<VenueAvailabilityOverride, UUID> {
    List<VenueAvailabilityOverride> findByVenueIdAndDateOrderByStartTime(UUID venueId, LocalDate date);
    List<VenueAvailabilityOverride> findByVenueIdOrderByDateAscStartTimeAsc(UUID venueId);
}
