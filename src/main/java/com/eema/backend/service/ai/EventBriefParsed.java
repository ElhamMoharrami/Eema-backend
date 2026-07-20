package com.eema.backend.service.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Data transfer object representing the parsed brief from the AI service for an event.
 * Uses a record for immutability and reduced boilerplate.
 */
public record EventBriefParsed(
        String aesthetic,
        String lighting,
        Integer size,
        Budget budget,
        List<String> tags,
        String shootType,
        String locationPreferences,
        String neighbourhood,
        String indoorOutdoor, // "indoor", "outdoor", or "both"
        List<String> requiredAmenities,
        Integer numPeople,
        String accessibility,
        String timingDatePreferences,
        Double confidenceScore // between 0 and 1
) {
    public EventBriefParsed {
        // Validate fields
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (budget != null) {
            if (budget.min() == null || budget.max() == null || budget.currency() == null) {
                throw new IllegalArgumentException("budget must have min, max, and currency non-null");
            }
            if (budget.min().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("budget.min must be non-negative");
            }
            if (budget.max().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("budget.max must be non-negative");
            }
            if (budget.min().compareTo(budget.max()) > 0) {
                throw new IllegalArgumentException("budget.min must be less than or equal to budget.max");
            }
        }
        if (numPeople != null && numPeople < 0) {
            throw new IllegalArgumentException("numPeople must be non-negative");
        }
        if (indoorOutdoor != null && !indoorOutdoor.matches("(?i)indoor|outdoor|both")) {
            throw new IllegalArgumentException("indoorOutdoor must be 'indoor', 'outdoor', or 'both'");
        }
        if (confidenceScore != null && (confidenceScore < 0.0 || confidenceScore > 1.0)) {
            throw new IllegalArgumentException("confidenceScore must be between 0 and 1");
        }
    }

    /**
     * Budget sub-object.
     */
    public record Budget(
            BigDecimal min,
            BigDecimal max,
            String currency
    ) {
        public Budget {
            Objects.requireNonNull(min, "min must not be null");
            Objects.requireNonNull(max, "max must not be null");
            Objects.requireNonNull(currency, "currency must not be null");
        }
    }
}