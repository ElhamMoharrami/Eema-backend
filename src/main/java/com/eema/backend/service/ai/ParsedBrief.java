package com.eema.backend.service.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import com.eema.backend.entity.enums.ProductionActivity;

public record ParsedBrief(
        String aesthetic,
        String lighting,

        Integer sizeM2Min,
        Integer sizeM2Max,

        BigDecimal budgetMinEur,
        BigDecimal budgetMaxEur,

        String neighbourhood,
        List<String> neighbourhoodOptions,

        String indoorOutdoor,

        List<String> requiredAmenities,
        List<String> mustHaveTags,
        List<String> niceToHaveTags,

        ProductionActivity productionActivity,
        Integer numPeople,

        Boolean requiresNaturalLight,
        Boolean prefersNaturalLight,
        Boolean requiresBlackout,
        Boolean prefersBlackout,
        BigDecimal minimumCeilingHeightM,
        Boolean requiresLoadingAccess,
        Boolean prefersLoadingAccess,
        Boolean requiresParking,
        Boolean prefersParking,
        Integer minimumParkingSpaces,
        Boolean requiresWheelchairAccessibility,
        Boolean requiresLift,
        Boolean prefersLift,
        Boolean requiresBathrooms,
        Boolean requiresSoundRecordingSuitable,

        LocalDate requestedDate,
        LocalTime requestedStartTime,
        LocalTime requestedEndTime,
        String timingDatePreferences,

        String briefSummary,
        Double confidenceScore
) {

    public ParsedBrief {
        neighbourhoodOptions = safeList(neighbourhoodOptions);
        requiredAmenities = safeList(requiredAmenities);
        mustHaveTags = safeList(mustHaveTags);
        niceToHaveTags = safeList(niceToHaveTags);

        if (sizeM2Min != null && sizeM2Min < 0) {
            throw new IllegalArgumentException(
                    "sizeM2Min must be non-negative"
            );
        }

        if (sizeM2Max != null && sizeM2Max < 0) {
            throw new IllegalArgumentException(
                    "sizeM2Max must be non-negative"
            );
        }

        if (sizeM2Min != null
                && sizeM2Max != null
                && sizeM2Min > sizeM2Max) {
            throw new IllegalArgumentException(
                    "sizeM2Min cannot be greater than sizeM2Max"
            );
        }

        if (budgetMinEur != null
                && budgetMinEur.signum() < 0) {
            throw new IllegalArgumentException(
                    "budgetMinEur must be non-negative"
            );
        }

        if (budgetMaxEur != null
                && budgetMaxEur.signum() < 0) {
            throw new IllegalArgumentException(
                    "budgetMaxEur must be non-negative"
            );
        }

        if (budgetMinEur != null
                && budgetMaxEur != null
                && budgetMinEur.compareTo(budgetMaxEur) > 0) {
            throw new IllegalArgumentException(
                    "budgetMinEur cannot be greater than budgetMaxEur"
            );
        }

        if (numPeople != null && numPeople <= 0) {
            throw new IllegalArgumentException(
                    "numPeople must be greater than zero"
            );
        }

        if (minimumCeilingHeightM != null && minimumCeilingHeightM.signum() <= 0) {
            throw new IllegalArgumentException("minimumCeilingHeightM must be greater than zero");
        }

        if (minimumParkingSpaces != null && minimumParkingSpaces < 0) {
            throw new IllegalArgumentException("minimumParkingSpaces must be non-negative");
        }

        if (requestedStartTime != null
                && requestedEndTime != null
                && !requestedEndTime.isAfter(requestedStartTime)) {
            throw new IllegalArgumentException(
                    "requestedEndTime must be after requestedStartTime"
            );
        }

        if (confidenceScore != null
                && (confidenceScore < 0 || confidenceScore > 1)) {
            throw new IllegalArgumentException(
                    "confidenceScore must be between 0 and 1"
            );
        }
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }
}
