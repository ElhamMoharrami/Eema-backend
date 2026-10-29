package com.eema.backend.service.ai;

import com.eema.backend.dto.response.MatchResponse;

public interface AiService {

    MatchResponse searchVenues(String brief);
}