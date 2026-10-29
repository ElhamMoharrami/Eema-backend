package com.eema.backend.service.ai;

import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.dto.response.MatchedVenueDto;
import com.eema.backend.dto.response.VenueDto;
import com.eema.backend.dto.response.VenueListDto;
import com.eema.backend.entity.Venue;
import com.eema.backend.mapper.VenueMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GeminiVenueRanker implements VenueRanker {

    private static final int MAX_GEMINI_CANDIDATES = 30;
    private static final int RESULT_LIMIT = 5;

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final VenueMapper venueMapper;

    @Override
    public MatchResponse rank(
            String originalBrief,
            ParsedBrief parsedBrief,
            List<Venue> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return new MatchResponse(
                    List.of(),
                    originalBrief,
                    0
            );
        }

        List<Venue> limitedCandidates = candidates.stream()
                .limit(MAX_GEMINI_CANDIDATES)
                .toList();

        String prompt = buildPrompt(
                originalBrief,
                parsedBrief,
                limitedCandidates
        );

        String response =
                geminiClient.generateContent(prompt);

        String json =
                geminiClient.extractGeneratedText(response);

        List<VenueRanking> rankings =
                parseRankings(json);

        List<MatchedVenueDto> matchedVenues = rankings.stream()
                .sorted(
                        Comparator.comparingDouble(
                                VenueRanking::score
                        ).reversed()
                )
                .limit(RESULT_LIMIT)
                .map(ranking -> {
                    Venue venue = findVenue(
                            limitedCandidates,
                            ranking.venueId()
                    );

                    if (venue == null) {
                        return null;
                    }

                    return toMatchedVenue(
                            venue,
                            ranking
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return new MatchResponse(
                matchedVenues,
                originalBrief,
                matchedVenues.size()
        );
    }

    private MatchedVenueDto toMatchedVenue(
            Venue venue,
            VenueRanking ranking
    ) {
        VenueDto dto = venueMapper.toDto(venue);

        VenueListDto venueListDto = new VenueListDto(
                dto.id(),
                dto.name(),
                dto.neighbourhood(),
                dto.sizeM2(),
                dto.hourlyRateEur(),
                dto.minimumBookingMinutes(),
                dto.fullDayRateEur(),
                dto.maxCrewSize(),
                dto.naturalLight(),
                dto.loadingAccess(),
                dto.parkingAvailable(),
                dto.soundRecordingSuitable(),
                dto.tags(),
                getPrimaryPhoto(dto),
                dto.status()
        );

        return new MatchedVenueDto(
                venueListDto,
                ranking.score(),
                ranking.explanation(),
                List.of()
        );
    }

    private String getPrimaryPhoto(VenueDto venue) {
        if (venue.photoPaths() == null
                || venue.photoPaths().isEmpty()) {
            return null;
        }

        return venue.photoPaths().get(0);
    }

    private String buildPrompt(
            String originalBrief,
            ParsedBrief parsedBrief,
            List<Venue> venues
    ) {
        try {
            String requirements =
                    objectMapper.writeValueAsString(parsedBrief);

            String candidates =
                    objectMapper.writeValueAsString(
                            venues.stream()
                                    .map(VenueCandidate::from)
                                    .toList()
                    );

            return """
                    You are an expert film and photography location scout.

                    ORIGINAL USER BRIEF:
                    %s

                    STRUCTURED REQUIREMENTS:
                    %s

                    AVAILABLE VENUES:
                    %s

                    Rank these venues according to suitability.

                    Important:
                    - Only use facts provided in AVAILABLE VENUES.
                    - Never invent venue features.
                    - Treat must-have requirements as strict requirements.
                    - Do not judge budget because no booking duration is known.
                    - Consider size, location, aesthetic,
                      lighting, amenities and nice-to-have features.
                    - Explain important strengths and mismatches.
                    - Rank only venues provided in AVAILABLE VENUES.
                    - Use exactly the venue ID provided.

                    Return ONLY valid JSON:

                    [
                      {
                        "venueId": "uuid",
                        "score": 0.95,
                        "explanation": "Short explanation"
                      }
                    ]

                    Rules:
                    - score must be between 0.0 and 1.0.
                    - Do not include markdown.
                    - Do not include additional text.
                    """.formatted(
                    originalBrief,
                    requirements,
                    candidates
            );

        } catch (Exception e) {
            throw new AiServiceException(
                    "Failed to create venue ranking prompt",
                    e
            );
        }
    }

    private List<VenueRanking> parseRankings(String json) {
        try {
            JsonNode root =
                    objectMapper.readTree(json);

            if (!root.isArray()) {
                throw new AiServiceException(
                        "Expected Gemini ranking array"
                );
            }

            List<VenueRanking> results =
                    new ArrayList<>();

            for (JsonNode node : root) {
                String venueId =
                        node.path("venueId").asText(null);

                if (venueId == null) {
                    continue;
                }

                double score =
                        node.path("score").asDouble(0.0);

                String explanation =
                        node.path("explanation")
                                .asText("");

                // Protect against invalid Gemini values
                score = Math.max(
                        0.0,
                        Math.min(1.0, score)
                );

                results.add(
                        new VenueRanking(
                                UUID.fromString(venueId),
                                score,
                                explanation
                        )
                );
            }

            return results;

        } catch (AiServiceException e) {
            throw e;

        } catch (Exception e) {
            throw new AiServiceException(
                    "Failed to parse Gemini rankings",
                    e
            );
        }
    }

    private Venue findVenue(
            List<Venue> venues,
            UUID id
    ) {
        return venues.stream()
                .filter(venue ->
                        Objects.equals(
                                venue.getId(),
                                id
                        )
                )
                .findFirst()
                .orElse(null);
    }

    private record VenueRanking(
            UUID venueId,
            double score,
            String explanation
    ) {
    }

    private record VenueCandidate(
            UUID id,
            String name,
            String neighbourhood,
            Integer sizeM2,
            BigDecimal hourlyRateEur,
            Integer minimumBookingMinutes,
            BigDecimal fullDayRateEur,
            List<String> tags,
            String description
    ) {

        private static VenueCandidate from(
                Venue venue
        ) {
            return new VenueCandidate(
                    venue.getId(),
                    venue.getName(),
                    venue.getNeighbourhood(),
                    venue.getSizeM2(),
                    venue.getHourlyRateEur(),
                    venue.getMinimumBookingMinutes(),
                    venue.getFullDayRateEur(),
                    venue.getTags(),
                    venue.getDescription()
            );
        }
    }
}
