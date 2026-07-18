package com.eema.backend.dto.response;

import java.util.List;

/**
 * Response for venue matching request.
 */
public record MatchResponse(
    List<MatchedVenueDto> venues,    // ranked list of matched venues
    String brief,                     // echo back the original brief for context
    Integer totalMatches              // total count
) {}