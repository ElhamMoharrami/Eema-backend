package com.eema.backend.service;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.ProductionActivity;
import com.eema.backend.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class VenueProductionProfileValidator {

    public void validateConsistency(Venue venue) {
        List<ProductionActivity> permitted = venue.getPermittedActivities();
        List<ProductionActivity> prohibited = venue.getProhibitedActivities();
        if (permitted != null && new HashSet<>(permitted).size() != permitted.size()) {
            throw new BadRequestException("Permitted activities must not contain duplicates");
        }
        if (prohibited != null && new HashSet<>(prohibited).size() != prohibited.size()) {
            throw new BadRequestException("Prohibited activities must not contain duplicates");
        }
        if (permitted != null && prohibited != null) {
            Set<ProductionActivity> overlap = new HashSet<>(permitted);
            overlap.retainAll(prohibited);
            if (!overlap.isEmpty()) {
                throw new BadRequestException("An activity cannot be both permitted and prohibited");
            }
        }
        if (Boolean.FALSE.equals(venue.getParkingAvailable())
                && venue.getParkingSpaces() != null && venue.getParkingSpaces() > 0) {
            throw new BadRequestException("Parking spaces cannot be positive when parking is unavailable");
        }
    }

    public List<String> readinessIssues(Venue venue) {
        List<String> issues = new ArrayList<>();
        if (venue.getMaxCrewSize() == null) issues.add("maxCrewSize");
        if (venue.getPermittedActivities() == null || venue.getPermittedActivities().isEmpty()) {
            issues.add("permittedActivities");
        }
        if (venue.getProductionRules() == null || venue.getProductionRules().isBlank()) issues.add("productionRules");
        if (venue.getNaturalLight() == null) issues.add("naturalLight");
        if (venue.getLoadingAccess() == null) issues.add("loadingAccess");
        if (venue.getBathroomsAvailable() == null) issues.add("bathroomsAvailable");
        if (venue.getSoundRecordingSuitable() == null) issues.add("soundRecordingSuitable");
        if (venue.getHourlyRateEur() == null) issues.add("hourlyRateEur");
        if (venue.getMinimumBookingMinutes() == null) issues.add("minimumBookingMinutes");
        if (venue.getMinimumLeadTimeMinutes() == null) issues.add("minimumLeadTimeMinutes");
        if (venue.getTimezone() == null || venue.getTimezone().isBlank()) issues.add("timezone");
        if (venue.getPhotoPaths() == null || venue.getPhotoPaths().isEmpty()) issues.add("photoPaths");
        return List.copyOf(issues);
    }
}
