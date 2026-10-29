package com.eema.backend.service;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.ProductionActivity;
import com.eema.backend.service.ai.ParsedBrief;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.eema.backend.service.ai.ParsedBriefFixtures.fromJson;

class VenueHardRequirementEvaluatorTest {

    private final VenueHardRequirementEvaluator evaluator = new VenueHardRequirementEvaluator();

    @Test
    void crewCapacityMustBeKnownAndMeetTheRequirement() {
        ParsedBrief brief = fromJson("{\"numPeople\":20}");

        assertThat(evaluator.isEligible(venueWithCrew(20), brief)).isTrue();
        assertThat(evaluator.isEligible(venueWithCrew(19), brief)).isFalse();
        assertThat(evaluator.isEligible(venueWithCrew(null), brief)).isFalse();
    }

    @Test
    void explicitActivityMustBePermittedAndNotProhibited() {
        ParsedBrief brief = fromJson("{\"productionActivity\":\"INTERVIEW\"}");

        Venue permitted = venue();
        when(permitted.getPermittedActivities()).thenReturn(List.of(ProductionActivity.INTERVIEW));
        assertThat(evaluator.isEligible(permitted, brief)).isTrue();

        Venue prohibited = venue();
        when(prohibited.getPermittedActivities()).thenReturn(List.of(ProductionActivity.INTERVIEW));
        when(prohibited.getProhibitedActivities()).thenReturn(List.of(ProductionActivity.INTERVIEW));
        assertThat(evaluator.isEligible(prohibited, brief)).isFalse();

        assertThat(evaluator.isEligible(venue(), brief)).isFalse();

        Venue omittedFromKnownPermissions = venue();
        when(omittedFromKnownPermissions.getPermittedActivities()).thenReturn(List.of(ProductionActivity.PHOTO));
        assertThat(evaluator.isEligible(omittedFromKnownPermissions, brief)).isFalse();
    }

    @Test
    void requiredBooleanFactsFailClosedWhenFalseOrUnknown() {
        assertRequiredBoolean("natural light");
        assertRequiredBoolean("loading access");
        assertRequiredBoolean("sound suitability");
        assertRequiredBoolean("wheelchair access");
        assertRequiredBoolean("lift");
        assertRequiredBoolean("bathrooms");
        assertRequiredBoolean("blackout");
    }

    @Test
    void minimumCeilingHeightMustBeKnownAndHighEnough() {
        ParsedBrief brief = fromJson("{\"minimumCeilingHeightM\":4.50}");

        assertThat(evaluator.isEligible(venueWithCeiling("4.50"), brief)).isTrue();
        assertThat(evaluator.isEligible(venueWithCeiling("4.49"), brief)).isFalse();
        assertThat(evaluator.isEligible(venueWithCeiling(null), brief)).isFalse();
    }

    @Test
    void parkingRequirementAndMinimumSpacesFailClosed() {
        ParsedBrief brief = fromJson("{\"requiresParking\":true}");
        assertThat(evaluator.isEligible(venueWithParking(true, null), brief)).isTrue();
        assertThat(evaluator.isEligible(venueWithParking(false, null), brief)).isFalse();
        assertThat(evaluator.isEligible(venueWithParking(null, null), brief)).isFalse();

        ParsedBrief spacesBrief = fromJson("{\"minimumParkingSpaces\":3}");
        assertThat(evaluator.isEligible(venueWithParking(true, 3), spacesBrief)).isTrue();
        assertThat(evaluator.isEligible(venueWithParking(true, 2), spacesBrief)).isFalse();
        assertThat(evaluator.isEligible(venueWithParking(true, null), spacesBrief)).isFalse();
        assertThat(evaluator.isEligible(venueWithParking(null, 3), spacesBrief)).isFalse();
    }

    @Test
    void preferencesNeverRemoveCandidatesEvenWhenFactsAreUnknown() {
        ParsedBrief brief = fromJson("""
                {"prefersNaturalLight":true,"prefersParking":true,
                 "prefersLift":true,"prefersBlackout":true}
                """);

        assertThat(evaluator.isEligible(venue(), brief)).isTrue();
    }

    private void assertRequiredBoolean(String label) {
        String field = switch (label) {
            case "natural light" -> "requiresNaturalLight";
            case "loading access" -> "requiresLoadingAccess";
            case "sound suitability" -> "requiresSoundRecordingSuitable";
            case "wheelchair access" -> "requiresWheelchairAccessibility";
            case "lift" -> "requiresLift";
            case "bathrooms" -> "requiresBathrooms";
            case "blackout" -> "requiresBlackout";
            default -> throw new AssertionError("Unhandled test field");
        };
        ParsedBrief brief = fromJson("{\"" + field + "\":true}");
        Venue yes = venue();
        Venue no = venue();
        Venue unknown = venue();

        switch (label) {
            case "natural light" -> {
                when(yes.getNaturalLight()).thenReturn(true);
                when(no.getNaturalLight()).thenReturn(false);
            }
            case "loading access" -> {
                when(yes.getLoadingAccess()).thenReturn(true);
                when(no.getLoadingAccess()).thenReturn(false);
            }
            case "sound suitability" -> {
                when(yes.getSoundRecordingSuitable()).thenReturn(true);
                when(no.getSoundRecordingSuitable()).thenReturn(false);
            }
            case "wheelchair access" -> {
                when(yes.getWheelchairAccessible()).thenReturn(true);
                when(no.getWheelchairAccessible()).thenReturn(false);
            }
            case "lift" -> {
                when(yes.getLiftAvailable()).thenReturn(true);
                when(no.getLiftAvailable()).thenReturn(false);
            }
            case "bathrooms" -> {
                when(yes.getBathroomsAvailable()).thenReturn(true);
                when(no.getBathroomsAvailable()).thenReturn(false);
            }
            case "blackout" -> {
                when(yes.getBlackoutPossible()).thenReturn(true);
                when(no.getBlackoutPossible()).thenReturn(false);
            }
            default -> throw new AssertionError("Unhandled test field");
        }

        assertThat(evaluator.isEligible(yes, brief)).as(label).isTrue();
        assertThat(evaluator.isEligible(no, brief)).as(label).isFalse();
        assertThat(evaluator.isEligible(unknown, brief)).as(label).isFalse();
    }

    private Venue venue() {
        return mock(Venue.class);
    }

    private Venue venueWithCrew(Integer capacity) {
        Venue venue = venue();
        when(venue.getMaxCrewSize()).thenReturn(capacity);
        return venue;
    }

    private Venue venueWithCeiling(String height) {
        Venue venue = venue();
        when(venue.getCeilingHeightM()).thenReturn(height == null ? null : new BigDecimal(height));
        return venue;
    }

    private Venue venueWithParking(Boolean available, Integer spaces) {
        Venue venue = venue();
        when(venue.getParkingAvailable()).thenReturn(available);
        when(venue.getParkingSpaces()).thenReturn(spaces);
        return venue;
    }
}
