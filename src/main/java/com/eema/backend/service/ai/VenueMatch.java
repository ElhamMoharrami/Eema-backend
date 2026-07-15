package com.eema.backend.service.ai;

import com.eema.backend.dto.response.VenueDto;

public class VenueMatch {
    private final VenueDto venue;
    private final double similarityScore; // 0.0 to 1.0
    private final String explanation;

    public VenueMatch(VenueDto venue, double similarityScore, String explanation) {
        this.venue = venue;
        this.similarityScore = similarityScore;
        this.explanation = explanation;
    }

    public VenueDto getVenue() {
        return venue;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public String getExplanation() {
        return explanation;
    }
}