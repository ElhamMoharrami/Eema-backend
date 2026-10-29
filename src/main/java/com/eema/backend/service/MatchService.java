package com.eema.backend.service;

import com.eema.backend.dto.request.MatchRequest;
import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.dto.response.MatchedVenueDto;
import com.eema.backend.dto.response.QuoteItemDto;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;

import com.eema.backend.service.ai.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private static final double DIRECT_MATCH_SCORE = 1.0;

    private final AiService aiService;
    private final VenueService venueService;

    public MatchResponse findMatches(MatchRequest request) {
        if (request.venueId() != null) {
            return createDirectQuote(request);
        }

        return searchWithAi(request.brief());
    }

    private MatchResponse createDirectQuote(MatchRequest request) {
        VenueDto venueDto = venueService.getVenueById(request.venueId());

        VenueListDto venue = toVenueListDto(venueDto);

        QuoteItemDto dayRateItem = new QuoteItemDto(
                "Day rate",
                venue.dayRateEur(),
                "Base rate for the venue"
        );

        MatchedVenueDto matchedVenue = new MatchedVenueDto(
                venue,
                DIRECT_MATCH_SCORE,
                "Direct quote for the requested venue",
                List.of(dayRateItem)
        );

        log.info("Direct quote generated for venueId={}", venue.id());

        return new MatchResponse(
                List.of(matchedVenue),
                request.brief(),
                1
        );
    }

    private MatchResponse searchWithAi(String brief) {
        MatchResponse response = aiService.searchVenues(brief);

        log.info(
                "AI venue search completed: matchCount={}",
                response.venues().size()
        );

        return response;
    }

    private VenueListDto toVenueListDto(VenueDto venue) {
        return new VenueListDto(
                venue.id(),
                venue.name(),
                venue.neighbourhood(),
                venue.sizeM2(),
                venue.dayRateEur(),
                venue.tags(),
                getPrimaryPhoto(venue),
                venue.status()
        );
    }

    private String getPrimaryPhoto(VenueDto venue) {
        if (venue.photoPaths() == null || venue.photoPaths().isEmpty()) {
            return null;
        }

        return venue.photoPaths().get(0);
    }
}