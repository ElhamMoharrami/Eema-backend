package com.eema.backend.service.ai;

import java.math.BigDecimal;
import java.util.List;

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

        String shootType,
        Integer numPeople,

        String accessibility,
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

        if (numPeople != null && numPeople < 0) {
            throw new IllegalArgumentException(
                    "numPeople must be non-negative"
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