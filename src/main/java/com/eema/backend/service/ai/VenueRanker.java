package com.eema.backend.service.ai;

import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.entity.Venue;

import java.util.List;

public interface VenueRanker {

    MatchResponse rank(
            String originalBrief,
            ParsedBrief parsedBrief,
            List<Venue> candidates
    );
}