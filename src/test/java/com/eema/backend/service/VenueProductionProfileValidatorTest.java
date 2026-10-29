package com.eema.backend.service;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.ProductionActivity;
import com.eema.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VenueProductionProfileValidatorTest {
    private final VenueProductionProfileValidator validator = new VenueProductionProfileValidator();

    @Test
    void completeProfileIsReady() {
        Venue venue = Venue.builder()
                .maxCrewSize(20).permittedActivities(List.of(ProductionActivity.PHOTO))
                .naturalLight(false).loadingAccess(false).bathroomsAvailable(true)
                .soundRecordingSuitable(true).productionRules("Keep common areas clear")
                .hourlyRateEur(new BigDecimal("100.00")).minimumBookingMinutes(180)
                .timezone("Europe/Amsterdam").photoPaths(List.of("/api/upload/files/photo.jpg"))
                .build();

        assertTrue(validator.readinessIssues(venue).isEmpty());
    }

    @Test
    void incompleteProfileReportsUnknownFieldsWithoutTreatingThemAsFalse() {
        Venue venue = Venue.builder().hourlyRateEur(BigDecimal.TEN).minimumBookingMinutes(60)
                .timezone("Europe/Amsterdam").photoPaths(List.of()).build();

        List<String> issues = validator.readinessIssues(venue);
        assertTrue(issues.containsAll(List.of("maxCrewSize", "permittedActivities", "productionRules",
                "naturalLight", "loadingAccess", "bathroomsAvailable", "soundRecordingSuitable", "photoPaths")));
        assertNull(venue.getNaturalLight());
    }

    @Test
    void contradictoryActivitiesAndParkingAreRejected() {
        Venue activities = Venue.builder().permittedActivities(List.of(ProductionActivity.FILM))
                .prohibitedActivities(List.of(ProductionActivity.FILM)).build();
        assertThrows(BadRequestException.class, () -> validator.validateConsistency(activities));

        Venue parking = Venue.builder().parkingAvailable(false).parkingSpaces(2).build();
        assertThrows(BadRequestException.class, () -> validator.validateConsistency(parking));
    }
}
