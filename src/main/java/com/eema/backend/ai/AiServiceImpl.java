package com.eema.backend.ai;

import com.eema.backend.entity.Venue;
import com.eema.backend.service.ai.AiServiceException;
import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.dto.response.MatchedVenueDto;
import com.eema.backend.dto.response.QuoteItemDto;
import com.eema.backend.dto.response.VenueListDto;
import com.eema.backend.repository.VenueRepository;
import com.eema.backend.service.VenueFilterBuilder;
import com.eema.backend.ai.ParsedBrief;
import com.eema.backend.service.ai.GeminiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final int MAX_CANDIDATES = 15;  // VenueFilterBuilder returns max 15
    private static final int TARGET_RESULTS = 5;   // return top 5 to scout

    private final RestTemplate restTemplate;
    private final GeminiProperties geminiProperties;
    private final VenueRepository venueRepository;
    private final VenueFilterBuilder venueFilterBuilder;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(
            RestTemplate restTemplate,
            GeminiProperties geminiProperties,
            VenueRepository venueRepository,
            VenueFilterBuilder venueFilterBuilder,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.geminiProperties = geminiProperties;
        this.venueRepository = venueRepository;
        this.venueFilterBuilder = venueFilterBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * Main orchestration method called by MatchController.
     *
     * Flow:
     * 1. Parse brief with Gemini → ParsedBrief
     * 2. Filter venues from DB using ParsedBrief → 10–15 candidates
     * 3. Re-rank candidates with Gemini → top 5 with explanations
     * 4. Return MatchResponse
     *
     * If any step fails, logs error and attempts graceful degradation.
     */
    @Override
    public MatchResponse searchVenues(String brief) throws AiServiceException {
        long startTime = System.currentTimeMillis();

        try {
            log.info("AI match started. Brief length: {} chars", brief.length());

            // Step 1: Parse brief
            log.info("Step 1: Parsing brief with Gemini...");
            com.eema.backend.ai.ParsedBrief parsedBrief = parseBrief(brief);
            log.info("Step 1 complete. Parsed: aesthetic={}, budget={}-{}, size={}-{}, tags={}",
                    parsedBrief.aesthetic(),
                    parsedBrief.budgetMinEur(),
                    parsedBrief.budgetMaxEur(),
                    parsedBrief.sizeM2Min(),
                    parsedBrief.sizeM2Max(),
                    parsedBrief.mustHaveTags());

            // Step 2: Filter venues from DB
            log.info("Step 2: Filtering venues from database...");
            // Convert our ParsedBrief to the format expected by VenueFilterBuilder
            com.eema.backend.service.ai.ParsedBrief serviceBrief = convertToServiceFormat(parsedBrief);
            List<Venue> candidates = venueFilterBuilder.findCandidateVenues(serviceBrief);
            log.info("Step 2 complete. Found {} candidate venues", candidates.size());

            if (candidates.isEmpty()) {
                log.warn("No venues matched the criteria. Returning empty results.");
                return buildEmptyResponse(brief);
            }

            // Step 3: Re-rank with Gemini
            log.info("Step 3: Re-ranking {} candidates with Gemini...", candidates.size());
            MatchResponse response = rankVenues(brief, parsedBrief, candidates);
            log.info("Step 3 complete. Returned {} ranked venues", response.venues().size());

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info("AI match completed in {}ms. Total cost estimate: €{}",
                    elapsedMs,
                    estimateGeminiCost());

            return response;

        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("AI match failed after {}ms. Error: {}", elapsedMs, e.getMessage(), e);

            // Attempt fallback: return unranked DB results
            try {
                com.eema.backend.ai.ParsedBrief fallbackParsed = parseBriefFallback(brief);
                com.eema.backend.service.ai.ParsedBrief serviceFallback = convertToServiceFormat(fallbackParsed);
                List<Venue> candidates = venueFilterBuilder.findCandidateVenues(serviceFallback);
                return buildFallbackResponse(candidates, brief);
            } catch (Exception fallbackError) {
                log.error("Fallback also failed. Returning empty response.", fallbackError);
                throw new AiServiceException("AI matching service temporarily unavailable", fallbackError);
            }
        }
    }

    /**
     * Step 1: Parse a plain-text brief into structured ParsedBrief using Gemini.
     *
     * Prompt tells Gemini to extract:
     * - Budget range
     * - Size requirements
     * - Location preference
     * - Required tags
     * - Aesthetic preferences
     *
     * Returns: ParsedBrief record
     */
    private ParsedBrief parseBrief(String brief) throws AiServiceException {
        String prompt = buildParserPrompt(brief);
        String jsonResponse = callGemini(prompt, "brief_parser");

        try {
            // Gemini returns JSON, parse it into ParsedBrief
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = objectMapper.readValue(jsonResponse, java.util.Map.class);

            return new com.eema.backend.ai.ParsedBrief(
                    data.containsKey("budgetMinEur") ?
                            BigDecimal.valueOf(((Number) data.get("budgetMinEur")).doubleValue()) : null,
                    data.containsKey("budgetMaxEur") ?
                            BigDecimal.valueOf(((Number) data.get("budgetMaxEur")).doubleValue()) : null,
                    data.containsKey("sizeM2Min") ? ((Number) data.get("sizeM2Min")).intValue() : null,
                    data.containsKey("sizeM2Max") ? ((Number) data.get("sizeM2Max")).intValue() : null,
                    (String) data.get("neighbourhood"),
                    (List<String>) data.getOrDefault("neighbourhoodOptions", List.of()),
                    (List<String>) data.getOrDefault("mustHaveTags", List.of()),
                    (List<String>) data.getOrDefault("niceToHaveTags", List.of()),
                    (String) data.get("aesthetic"),
                    (String) data.get("lighting"),
                    (String) data.get("purpose"),
                    (String) data.get("briefSummary"),
                    data.containsKey("confidenceScore") ?
                            ((Number) data.get("confidenceScore")).doubleValue() : null
            );
        } catch (Exception e) {
            log.error("Failed to parse Gemini response as ParsedBrief. Response: {}", jsonResponse, e);
            throw new AiServiceException("Gemini response was not valid JSON", e);
        }
    }

    /**
     * Fallback brief parser if Gemini is down.
     * Uses simple heuristics to extract fields without AI.
     */
    private ParsedBrief parseBriefFallback(String brief) {
        log.warn("Using fallback brief parser (Gemini unavailable)");

        // Simple heuristics
        BigDecimal budgetMax = extractBudgetHeuristic(brief);
        Integer sizeMin = brief.toLowerCase().contains("large") ? 300 :
                brief.toLowerCase().contains("small") ? 50 : null;
        List<String> tags = extractTagsHeuristic(brief);

        return new com.eema.backend.ai.ParsedBrief(
                BigDecimal.valueOf(1000),  // assume min budget
                budgetMax,                  // extracted from brief
                sizeMin,                    // heuristic guess
                null,
                null,
                null,
                tags,                       // extracted tags
                List.of(),
                "unknown",
                "unknown",
                "unknown",
                brief.substring(0, Math.min(100, brief.length())),
                0.0  // low confidence
        );
    }

    /**
     * Converts our AI package ParsedBrief to the service package format
     * expected by VenueFilterBuilder.
     *
     * Mapping strategy:
     * - budgetPerDay: use budgetMaxEur (conservative estimate)
     * - neighbourhood: use first option from neighbourhoodOptions, or neighbourhood if set
     * - sizeM2: use sizeM2Min (minimum requirement)
     * - tags: combine mustHaveTags and niceToHaveTags
     */
     com.eema.backend.service.ai.ParsedBrief convertToServiceFormat(com.eema.backend.ai.ParsedBrief brief) {
        // Determine budget: use max if available, otherwise min, otherwise null
        java.math.BigDecimal budgetPerDay = null;
        if (brief.budgetMaxEur() != null) {
            budgetPerDay = brief.budgetMaxEur();
        } else if (brief.budgetMinEur() != null) {
            budgetPerDay = brief.budgetMinEur();
        }

        // Determine neighbourhood: prefer options list, fallback to single value
        String neighbourhood = null;
        if (brief.neighbourhoodOptions() != null && !brief.neighbourhoodOptions().isEmpty()) {
            neighbourhood = brief.neighbourhoodOptions().get(0);
        } else if (brief.neighbourhood() != null) {
            neighbourhood = brief.neighbourhood();
        }

        // Determine size: use min size
        java.lang.Integer sizeM2 = brief.sizeM2Min();

        // Combine tags: must-have + nice-to-have
        java.util.List<String> allTags = new java.util.ArrayList<>();
        if (brief.mustHaveTags() != null) {
            allTags.addAll(brief.mustHaveTags());
        }
        if (brief.niceToHaveTags() != null) {
            allTags.addAll(brief.niceToHaveTags());
        }

        return new com.eema.backend.service.ai.ParsedBrief(
                brief.aesthetic(),                           // aesthetic
                null,                                        // locationPreferences (not available in AI version)
                neighbourhood,                               // neighbourhood
                "both",                                      // indoorOutdoor (default)
                sizeM2,                                      // sizeM2
                budgetPerDay,                                // budgetPerDay
                null,                                        // requiredAmenities (not available in AI version)
                brief.purpose(),                             // shootType (maps to purpose)
                null,                                        // numPeople (not available in AI version)
                brief.lighting(),                            // lightingPreferences (maps to lighting)
                null,                                        // accessibility (not available in AI version)
                null,                                        // timingDatePreferences (not available in AI version)
                allTags,                                     // tags
                brief.confidenceScore()                      // confidenceScore
        );
    }

    /**
     * Step 3: Re-rank candidate venues using Gemini.
     *
     * Sends: original brief + 10–15 candidate venues
     * Receives: top 5 ranked with match scores + explanations
     */
    private MatchResponse rankVenues(
            String brief,
            ParsedBrief parsedBrief,
            List<Venue> candidates
    ) throws AiServiceException {
        String prompt = buildRankerPrompt(brief, parsedBrief, candidates);
        String jsonResponse = callGemini(prompt, "venue_ranker");

        try {
            // Parse Gemini's ranked response
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response = objectMapper.readValue(jsonResponse, java.util.Map.class);

            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> rankedVenues = (List<java.util.Map<String, Object>>)
                    response.get("ranked_venues");

            // Convert to MatchResponse
            List<MatchedVenueDto> matchedVenues = new ArrayList<>();
            for (java.util.Map<String, Object> rankedVenue : rankedVenues) {
                MatchedVenueDto matchedVenue = buildMatchedVenueDto(rankedVenue, candidates);
                if (matchedVenue != null) {
                    matchedVenues.add(matchedVenue);
                }
            }

            // Sort by score descending (Gemini should have done this, but just in case)
            Collections.sort(matchedVenues, Comparator.comparingDouble(MatchedVenueDto::score).reversed());

            // Limit to target results
            List<MatchedVenueDto> limited = matchedVenues.stream()
                    .limit(TARGET_RESULTS)
                    .toList();

            return new MatchResponse(
                    limited,
                    brief,
                    candidates.size()
            );
        } catch (Exception e) {
            log.error("Failed to parse Gemini ranker response. Response: {}", jsonResponse, e);
            throw new AiServiceException("Gemini re-ranker response was invalid", e);
        }
    }

    /**
     * Build a MatchedVenueDto from Gemini's ranked response and the original candidates list.
     */
    private MatchedVenueDto buildMatchedVenueDto(java.util.Map<String, Object> ranked, List<Venue> candidates) {
        Object venueIdObj = ranked.get("venue_id");
        if (venueIdObj == null) {
            return null;
        }

        String venueIdStr = venueIdObj.toString();
        try {
            UUID venueId = UUID.fromString(venueIdStr);
            Venue venue = null;

            // First try to find in our candidates list
            for (Venue v : candidates) {
                if (v.getId().equals(venueId)) {
                    venue = v;
                    break;
                }
            }

            // If not in candidates, try to fetch from repository
            if (venue == null) {
                venue = venueRepository.findById(venueId).orElse(null);
            }

            if (venue != null) {
                return buildMatchedVenueDtoFromVenue(
                        venue,
                        ((Number) ranked.getOrDefault("match_score", 0.5)).doubleValue(),
                        (String) ranked.getOrDefault("explanation", "Matched by AI ranking")
                );
            }

            return null;
        } catch (IllegalArgumentException e) {
            // Invalid UUID
            return null;
        }
    }

    /**
     * Build a MatchedVenueDto from a Venue entity and AI-generated score/explanation.
     */
    private MatchedVenueDto buildMatchedVenueDtoFromVenue(Venue venue, double score, String explanation) {
        // Build VenueListDto
        String firstPhotoUrl = "";
        if (venue.getPhotoPaths() != null && !venue.getPhotoPaths().isEmpty()) {
            firstPhotoUrl = venue.getPhotoPaths().get(0);
        }
        VenueListDto venueDto = new VenueListDto(
                venue.getId(),
                venue.getName(),
                venue.getNeighbourhood(),
                venue.getSizeM2(),
                venue.getDayRateEur(),
                venue.getTags() != null ? venue.getTags() : List.of(),
                firstPhotoUrl,
                venue.getStatus()
        );

        // Build quote items (simplified)
        List<QuoteItemDto> quoteItems = List.of(
                new QuoteItemDto(
                        "Day rate",
                        venue.getDayRateEur(),
                        "Daily rental rate"
                )
        );

        return new MatchedVenueDto(
                venueDto,
                score,
                explanation,
                quoteItems
        );
    }

    /**
     * Call Gemini API with a prompt.
     *
     * Returns: raw JSON string from Gemini's response.
     */
    private String callGemini(String prompt, String operation) throws AiServiceException {
        long startTime = System.currentTimeMillis();

        try {
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            java.util.Map<String, Object> content = new java.util.HashMap<>();
            java.util.Map<String, Object> part = new java.util.HashMap<>();
            part.put("text", prompt);
            content.put("parts", java.util.List.of(part));
            requestBody.put("contents", java.util.List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<java.util.Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = GEMINI_API_BASE + "?key=" + geminiProperties.getApiKey();

            ResponseEntity<java.util.Map> geminiResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    java.util.Map.class
            );

            if (geminiResponse.getBody() == null) {
                throw new IllegalStateException("Gemini returned empty response");
            }

            // Extract text from Gemini's response format
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> candidates =
                    (java.util.List<java.util.Map<String, Object>>) geminiResponse.getBody().get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                throw new IllegalStateException("Gemini returned no candidates");
            }

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> contentObj = (java.util.Map<String, Object>)
                    candidates.get(0).get("content");

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> parts = (java.util.List<java.util.Map<String, Object>>)
                    contentObj.get("parts");

            if (parts == null || parts.isEmpty()) {
                throw new IllegalStateException("Gemini returned no text parts");
            }

            String text = (String) parts.get(0).get("text");

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info("Gemini API call ({}) completed in {}ms", operation, elapsedMs);

            return text;

        } catch (RestClientException e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new AiServiceException("Gemini API error: " + e.getMessage(), e);
        }
    }

    /**
     * Build the Gemini prompt for brief parsing.
     */
    private String buildParserPrompt(String brief) {
        return """
You are a location scout assistant. Extract structured information from this film/photo production brief.

BRIEF:
{BRIEF}

Respond ONLY with valid JSON (no markdown, no explanation). Use this exact schema:
{
  "budgetMinEur": 1500,
  "budgetMaxEur": 4000,
  "sizeM2Min": 150,
  "sizeM2Max": 1000,
  "neighbourhood": "Noord",
  "neighbourhoodOptions": ["Noord", "NDSM"],
  "mustHaveTags": ["industrial", "warehouse", "high-ceiling"],
  "niceToHaveTags": ["parking", "loading-dock"],
  "aesthetic": "moody industrial",
  "lighting": "directional, high-contrast",
  "purpose": "luxury jewelry product shoot",
  "briefSummary": "Looking for raw industrial warehouse with dramatic lighting for jewelry campaign"
}

Extraction rules:
- If budget is mentioned, extract min/max range. If only one number, use it for both.
- If size is mentioned, extract in m². If only one number, use it for min and double for max.
- For neighbourhood: if Amsterdam district mentioned, include it. If not mentioned, leave null.
- Extract tags that match Amsterdam venue features: warehouse, loft, studio, gallery, industrial, minimal, daylight, white-walls, high-ceiling, rooftop, canal-view, garden, etc.
- If a field is not mentioned, use null or empty array.

Amsterdam neighbourhoods: Noord, Westerpark, NDSM, Jordaan, De Pijp, Oud-West, Zuidas, Oud-Zuid, Plantage, Centrum

Respond only with JSON. No extra text.
""".replace("{BRIEF}", brief);
    }

    /**
     * Build the Gemini prompt for venue re-ranking.
     */
    private String buildRankerPrompt(String brief, ParsedBrief parsed, List<Venue> candidates) {
        StringBuilder venuesJson = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            Venue v = candidates.get(i);
            venuesJson.append(String.format("""
  {
    "venue_id": "%s",
    "name": "%s",
    "neighbourhood": "%s",
    "size_m2": %d,
    "day_rate_eur": %.2f,
    "tags": %s,
    "description": "%s"
  }""", v.getId(), v.getName(), v.getNeighbourhood(), v.getSizeM2(), v.getDayRateEur(),
                    v.getTags() != null ? v.getTags().toString() : "[]",
                    v.getDescription() != null ? v.getDescription() : ""));
            if (i < candidates.size() - 1) {
                if (i < candidates.size() - 1) {
                    // This condition is always true when the outer condition is true, so it's redundant
                    // But let's keep the structure and just add the comma and newline
                    if (i < candidates.size() - 1) {
                        // This is still redundant, but harmless
                    }
                    venuesJson.append(",\n");
                }
            }
        }

        // Fix the logic - simplify to just add comma and newline when not the last element
        StringBuilder correctedJson = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            Venue v = candidates.get(i);
            correctedJson.append(String.format("""
  {
    "venue_id": "%s",
    "name": "%s",
    "neighbourhood": "%s",
    "size_m2": %d,
    "day_rate_eur": %.2f,
    "tags": %s,
    "description": "%s"
  }""", v.getId(), v.getName(), v.getNeighbourhood(), v.getSizeM2(), v.getDayRateEur(),
                    v.getTags() != null ? v.getTags().toString() : "[]",
                    v.getDescription() != null ? v.getDescription() : ""));
            if (i < candidates.size() - 1) {
                correctedJson.append(",\n");
            }
        }

        return String.format("""
You are a location scout expert. Rank these venues by how well they match the scout's brief.

SCOUT'S BRIEF:
%s

SCOUT'S PARSED REQUIREMENTS:
- Budget: €%s – €%s per day
- Size: %d – %d m²
- Aesthetic: %s
- Lighting: %s
- Must-have tags: %s
- Nice-to-have tags: %s

CANDIDATE VENUES:
[
%s
]

Respond ONLY with valid JSON (no markdown). Use this schema:
{
  "ranked_venues": [
    {
      "venue_id": "uuid",
      "rank": 1,
      "match_score": 0.92,
      "explanation": "Why this venue matches the brief"
    }
  ]
}

Ranking criteria:
1. Visual aesthetics match (does venue look right for the brief?)
2. Practical fit (size, lighting, amenities match needs?)
3. Price (is it within budget?)
4. Location (accessible from central Amsterdam?)
5. Features (must-have tags present? nice-to-have tags present?)

Rank ALL venues. Give each a score 0.0–1.0 (higher = better match). Be specific in explanations (mention actual venue features, why it works or doesn't for this brief).

Respond only with JSON. No extra text.
""",
            brief,
            parsed.budgetMinEur() != null ? parsed.budgetMinEur() : "N/A",
            parsed.budgetMaxEur() != null ? parsed.budgetMaxEur() : "N/A",
            parsed.sizeM2Min() != null ? parsed.sizeM2Min() : "N/A",
            parsed.sizeM2Max() != null ? parsed.sizeM2Max() : "N/A",
            parsed.aesthetic() != null ? parsed.aesthetic() : "not specified",
            parsed.lighting() != null ? parsed.lighting() : "not specified",
            parsed.mustHaveTags(),
            parsed.niceToHaveTags(),
            correctedJson.toString()
        );
    }

    // Helper methods for building responses and fallbacks...

    private MatchResponse buildEmptyResponse(String brief) {
        return new MatchResponse(java.util.List.of(), brief, 0);
    }

    private MatchResponse buildFallbackResponse(List<Venue> venues, String brief) {
        // Return unranked venues sorted by price (cheapest first)
        java.util.List<Venue> sortedVenues = new java.util.ArrayList<>(venues);
        sortedVenues.sort(java.util.Comparator.comparing(
                v -> v.getDayRateEur() != null ? v.getDayRateEur() : new java.math.BigDecimal("0")
        ));

        java.util.List<MatchedVenueDto> matchedVenues = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(TARGET_RESULTS, sortedVenues.size()); i++) {
            Venue venue = sortedVenues.get(i);
            matchedVenues.add(buildFallbackMatchedVenueDto(venue));
        }

        return new MatchResponse(
                matchedVenues,
                brief,
                venues.size()
        );
    }

    private MatchedVenueDto buildFallbackMatchedVenueDto(Venue venue) {
        // Build VenueListDto
        String firstPhotoUrl = "";
        if (venue.getPhotoPaths() != null && !venue.getPhotoPaths().isEmpty()) {
            firstPhotoUrl = venue.getPhotoPaths().get(0);
        }
        VenueListDto venueDto = new VenueListDto(
                venue.getId(),
                venue.getName(),
                venue.getNeighbourhood(),
                venue.getSizeM2(),
                venue.getDayRateEur(),
                venue.getTags() != null ? venue.getTags() : List.of(),
                firstPhotoUrl,
                venue.getStatus()
        );

        // Build quote items (simplified)
        List<QuoteItemDto> quoteItems = List.of(
                new QuoteItemDto(
                        "Day rate",
                        venue.getDayRateEur(),
                        "Daily rental rate"
                )
        );

        return new MatchedVenueDto(
                venueDto,
                0.5,  // neutral score when AI is unavailable
                "Matched by database filter (AI ranking unavailable)",
                quoteItems
        );
    }

    private BigDecimal extractBudgetHeuristic(String brief) {
        // Try to find "€XXXX" or "$XXXX" in text
        // This is a simplified implementation - in reality you'd want a more robust regex
        if (containsEuroSymbol(brief)) {
            // Very basic extraction - find number after €
            int euroIndex = brief.indexOf("€");
            StringBuilder numBuilder = new StringBuilder();
            for (int i = euroIndex + 1; i < Math.min(euroIndex + 20, brief.length()); i++) { // Limit to prevent overflow
                char c = brief.charAt(i);
                if (Character.isDigit(c) || c == '.' || c == ',') {
                    if (c == ',') continue; // Skip commas
                    numBuilder.append(c);
                } else if (c == ' ') {
                    // Space might indicate end of number
                    break;
                } else {
                    break;
                }
            }
            try {
                String numStr = numBuilder.toString().replace(",", "");
                if (!numStr.isEmpty()) {
                    return new BigDecimal(numStr);
                }
            } catch (NumberFormatException e) {
                // Fallback to default
            }
        }
        // Default fallback
        return new BigDecimal("5000");
    }

    private boolean containsEuroSymbol(String text) {
        return text != null && text.contains("€");
    }

    private java.util.List<String> extractTagsHeuristic(String brief) {
        java.util.List<String> tags = new java.util.ArrayList<>();
        String lowerCase = brief.toLowerCase();
        if (lowerCase.contains("warehouse")) tags.add("warehouse");
        if (lowerCase.contains("industrial")) tags.add("industrial");
        if (lowerCase.contains("studio")) tags.add("studio");
        if (lowerCase.contains("gallery")) tags.add("gallery");
        if (lowerCase.contains("rooftop")) tags.add("rooftop");
        if (lowerCase.contains("loft")) tags.add("loft");
        if (lowerCase.contains("penthouse")) tags.add("penthouse");
        if (lowerCase.contains("townhouse")) tags.add("townhouse");
        if (lowerCase.contains("kitchen")) tags.add("kitchen");
        if (lowerCase.contains("salon")) tags.add("salon");
        if (lowerCase.contains("darkroom")) tags.add("darkroom");
        if (lowerCase.contains("hall")) tags.add("hall");
        if (lowerCase.contains("suite")) tags.add("suite");
        if (lowerCase.contains("bar")) tags.add("bar");
        if (lowerCase.contains("pub")) tags.add("pub");
        if (lowerCase.contains("retail")) tags.add("retail");
        if (lowerCase.contains("space")) tags.add("space");
        if (lowerCase.contains("event")) tags.add("event");
        if (lowerCase.contains("pop-up")) tags.add("pop-up");
        return tags;
    }

    private double estimateGeminiCost() {
        // Brief parser: ~$0.0001
        // Re-ranker (10-15 venues): ~$0.0004
        // Total: ~$0.0005
        return 0.0005;
    }
}