package com.eema.backend.service.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Data transfer object representing the parsed brief from the AI service.
 * Uses a record for immutability and reduced boilerplate.
 */
public record ParsedBrief(
        String aesthetic,
        String locationPreferences,
        String neighbourhood,
        String indoorOutdoor, // "indoor", "outdoor", or "both"
        Integer sizeM2,
        BigDecimal budgetPerDay,
        List<String> requiredAmenities,
        String shootType,
        Integer numPeople,
        String lightingPreferences,
        String accessibility,
        String timingDatePreferences,
        List<String> tags,
        Double confidenceScore // between 0 and 1
) {
    public ParsedBrief {
        // Validate fields
        if (sizeM2 != null && sizeM2 < 0) {
            throw new IllegalArgumentException("sizeM2 must be non-negative");
        }
        if (budgetPerDay != null && budgetPerDay.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("budgetPerDay must be non-negative");
        }
        if (numPeople != null && numPeople < 0) {
            throw new IllegalArgumentException("numPeople must be non-negative");
        }
        if (confidenceScore != null && (confidenceScore < 0.0 || confidenceScore > 1.0)) {
            throw new IllegalArgumentException("confidenceScore must be between 0 and 1");
        }
        // Validate indoorOutdoor
        if (indoorOutdoor != null && !indoorOutdoor.matches("(?i)indoor|outdoor|both")) {
            throw new IllegalArgumentException("indoorOutdoor must be 'indoor', 'outdoor', or 'both'");
        }
    }
}