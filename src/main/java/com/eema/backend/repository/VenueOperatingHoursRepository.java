package com.eema.backend.repository;

import com.eema.backend.entity.VenueOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface VenueOperatingHoursRepository extends JpaRepository<VenueOperatingHours, UUID> {
    List<VenueOperatingHours> findByVenueIdOrderByDayOfWeekAscStartTimeAsc(UUID venueId);
    List<VenueOperatingHours> findByVenueIdAndDayOfWeekOrderByStartTime(UUID venueId, DayOfWeek day);
    void deleteByVenueId(UUID venueId);
}
