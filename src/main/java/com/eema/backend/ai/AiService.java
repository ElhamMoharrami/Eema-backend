package com.eema.backend.ai;

import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.service.ai.AiServiceException;

/**
 * Service for AI-powered venue matching (simplified interface for the match controller).
 */
public interface AiService {

    /**
     * Search for venues matching the given brief.
     *
     * @param brief the free-text description of the desired venue
     * @return matching venues with scores and explanations
     * @throws AiServiceException if the AI service fails
     */
    MatchResponse searchVenues(String brief) throws AiServiceException;
}