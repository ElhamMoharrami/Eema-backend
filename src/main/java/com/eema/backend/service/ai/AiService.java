package com.eema.backend.service.ai;

import com.eema.backend.dto.response.VenueDto;

import java.util.List;

/**
 * Service for AI-powered venue matching.
 */
public interface AiService {

    /**
     * Parse a free-text brief into structured data using AI.
     *
     * @param brief the free-text description of the desired venue
     * @return parsed brief; null if parsing fails
     */
    ParsedBrief parseBrief(String brief);

    /**
     * Match venues to a brief.
     *
     * @param brief the free-text description
     * @param limit maximum number of results to return
     * @return list of matched venues with scores and explanations, ordered by relevance
     */
    List<VenueMatch> match(String brief, int limit);
}