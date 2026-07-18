package com.eema.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import com.eema.backend.ai.AiService;
import com.eema.backend.dto.request.MatchRequest;
import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.dto.response.MatchedVenueDto;
import com.eema.backend.dto.response.QuoteItemDto;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;
import com.eema.backend.service.VenueService;

@Slf4j
@RestController
@RequestMapping("/api/match")
@Validated
public class MatchController {

    private final AiService aiService;
    private final VenueService venueService;

    public MatchController(AiService aiService, VenueService venueService) {
        this.aiService = aiService;
        this.venueService = venueService;
    }

    /**
     * POST /api/match
     *
     * Input: brief (string 10-1000 chars), optional venueId
     *
     * Behavior:
     * - If venueId provided: return direct quote for that venue
     * - If venueId null: parse brief with AI, return ranked venues
     *
     * Returns: MatchResponse with venues list + explanation
     * Status: 200 OK on success, 400 on validation, 500 on AI failure
     */
    @PostMapping
    public ResponseEntity<MatchResponse> search(@RequestBody @Valid MatchRequest request) {
        log.info("Match request received. Brief length: {}", request.brief().length());
        log.info("Searching venues for brief: '{}'", request.brief().substring(0, Math.min(50, request.brief().length())) + "...");

        MatchResponse response;
        if (request.venueId() != null) {
            // Venue-specific quote
            VenueDto venueDto = venueService.getVenueById(request.venueId());
            VenueListDto venue = new VenueListDto(
                    venueDto.id(),
                    venueDto.name(),
                    venueDto.neighbourhood(),
                    venueDto.sizeM2(),
                    venueDto.dayRateEur(),
                    venueDto.tags(),
                    (venueDto.photoPaths() != null && !venueDto.photoPaths().isEmpty()) ? venueDto.photoPaths().get(0) : null,
                    venueDto.status()
            );
            log.info("Found venue {} for direct quote", venue.id());

            // Build a simple quote item for the day rate
            QuoteItemDto dayRateItem = new QuoteItemDto(
                    "Day rate",
                    venue.dayRateEur(),
                    "Base rate for the venue"
            );
            List<QuoteItemDto> quoteItems = List.of(dayRateItem);

            MatchedVenueDto matchedVenue = new MatchedVenueDto(
                    venue,
                    1.0, // perfect match since user requested this specific venue
                    "Direct quote for the requested venue",
                    quoteItems
            );

            response = new MatchResponse(
                    List.of(matchedVenue),
                    request.brief(),
                    1
            );
        } else {
            // AI-powered search
            try {
                response = aiService.searchVenues(request.brief());
                log.info("AI search returned {} venues", response.venues().size());
            } catch (Exception e) {
                // Let GlobalExceptionHandler handle AI service failures
                throw e;
            }
        }

        return ResponseEntity.ok(response);
    }
}