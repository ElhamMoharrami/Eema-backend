package com.eema.backend.dto.response;

import java.util.List;

/**
 * Represents a venue matched to a brief, with score and explanation.
 */
public record MatchedVenueDto(
    VenueListDto venue,
    Double score,                    // 0.0 to 1.0, relevance score
    String explanation,              // why this venue matches the brief
    List<QuoteItemDto> quoteItems    // optional: breakdown of costs
) {}