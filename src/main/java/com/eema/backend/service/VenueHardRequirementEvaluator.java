package com.eema.backend.service;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.ProductionActivity;
import com.eema.backend.service.ai.ParsedBrief;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VenueHardRequirementEvaluator {

    public boolean isEligible(Venue venue, ParsedBrief brief) {
        return meetsMinimum(venue.getMaxCrewSize(), brief.numPeople())
                && activityAllowed(venue, brief.productionActivity())
                && meetsRequired(venue.getNaturalLight(), brief.requiresNaturalLight())
                && meetsRequired(venue.getBlackoutPossible(), brief.requiresBlackout())
                && meetsMinimum(venue.getCeilingHeightM(), brief.minimumCeilingHeightM())
                && meetsRequired(venue.getLoadingAccess(), brief.requiresLoadingAccess())
                && meetsRequired(venue.getParkingAvailable(), parkingRequired(brief))
                && meetsMinimum(venue.getParkingSpaces(), brief.minimumParkingSpaces())
                && meetsRequired(venue.getWheelchairAccessible(), brief.requiresWheelchairAccessibility())
                && meetsRequired(venue.getLiftAvailable(), brief.requiresLift())
                && meetsRequired(venue.getBathroomsAvailable(), brief.requiresBathrooms())
                && meetsRequired(venue.getSoundRecordingSuitable(), brief.requiresSoundRecordingSuitable());
    }

    private boolean activityAllowed(Venue venue, ProductionActivity activity) {
        if (activity == null) return true;
        List<ProductionActivity> prohibited = venue.getProhibitedActivities();
        if (prohibited != null && prohibited.contains(activity)) return false;
        List<ProductionActivity> permitted = venue.getPermittedActivities();
        return permitted != null && permitted.contains(activity);
    }

    private boolean meetsRequired(Boolean actual, Boolean required) {
        return !Boolean.TRUE.equals(required) || Boolean.TRUE.equals(actual);
    }

    private <T extends Comparable<? super T>> boolean meetsMinimum(T actual, T minimum) {
        return minimum == null || actual != null && actual.compareTo(minimum) >= 0;
    }

    private Boolean parkingRequired(ParsedBrief brief) {
        return Boolean.TRUE.equals(brief.requiresParking()) || brief.minimumParkingSpaces() != null
                ? Boolean.TRUE : null;
    }
}
