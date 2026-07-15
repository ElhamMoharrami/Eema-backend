package com.eema.backend.service.ai;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.VenueStatus;
import com.eema.backend.mapper.VenueMapper;
import com.eema.backend.repository.VenueRepository;
import com.eema.backend.dto.response.VenueDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GeminiAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);
    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    // Regex to match markdown code fences: optional whitespace, triple backticks, optional language identifier, newline, content, newline, triple backticks, optional whitespace
    private static final Pattern MARKDOWN_FENCE_PATTERN = Pattern.compile(
            "^\\s*```[a-zA-Z0-9]*\\s*\\r?\\n([\\s\\S]*?)\\r?\\n\\s*```\\s*$",
            Pattern.DOTALL
    );

    public GeminiAiService(VenueRepository venueRepository,
                           VenueMapper venueMapper,
                           GeminiClient geminiClient,
                           ObjectMapper objectMapper) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ParsedBrief parseBrief(String brief) {
        if (brief == null || blankSafe(brief).isEmpty()) {
            return null;
        }
        String prompt = buildParsePrompt(brief);
        try {
            String response = geminiClient.generateContent(prompt);
            String jsonText = extractTextFromGeminiResponse(response);
            JsonNode jsonNode = objectMapper.readTree(jsonText);
            if (jsonNode.isObject()) {
                ParsedBrief parsed = objectMapper.convertValue(jsonNode, ParsedBrief.class);
                // Log if confidence score is low
                if (parsed.confidenceScore() != null && parsed.confidenceScore() < 0.5) {
                    log.warn("Low confidence score in parsed brief: {}", parsed.confidenceScore());
                }
                return parsed;
            } else {
                log.warn("Gemini response is not a JSON object. Brief length: {}", brief.length());
                // Return an empty ParsedBrief (all nulls) to allow the search to proceed with no filters
                return new ParsedBrief(
                        null, null, null, null, null, null,
                        null, null, null, null, null, null,
                        null, null
                );
            }
        } catch (Exception e) {
            log.warn("Failed to parse brief with Gemini. Brief length: {}", brief.length(), e);
            // Return an empty ParsedBrief (all nulls) to allow the search to proceed with no filters
            return new ParsedBrief(
                    null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    null, null
            );
        }
    }

    @Override
    public List<VenueMatch> match(String brief, int limit) {
        if (brief == null || blankSafe(brief).isEmpty()) {
            return Collections.emptyList();
        }
        // Validate and clamp limit between 1 and 20
        int validLimit = Math.max(1, Math.min(limit, 20));

        ParsedBrief parsed = parseBrief(brief);
        if (parsed == null) {
            // This only happens when brief is empty/blank (handled above) or if we decide to return null for other cases
            // Currently, we only return null for empty/blank brief
            return Collections.emptyList();
        }

        List<Venue> venues = venueRepository.findByStatus(VenueStatus.ACTIVE);
        if (venues == null || venues.isEmpty()) {
            return Collections.emptyList();
        }

        List<VenueDto> venueDtos = venues.stream()
                .map(venueMapper::toDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<VenueDto> filtered = filterVenues(venueDtos, parsed);
        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        // Limit the venues sent to Gemini to avoid token limits (top 50 by preliminary score)
        List<VenueDto> limitedForGemini = limitVenuesForGemini(filtered, 50);

        try {
            List<VenueScore> scored = rankWithGemini(parsed, limitedForGemini);
            // Sort by score descending, then by venue name ascending for tie-breaking
            scored.sort(Comparator
                    .comparingDouble(VenueScore::score).reversed()
                    .thenComparing(vs -> {
                        VenueDto v = findVenueById(limitedForGemini, vs.venueId());
                        return v != null ? v.name() : "";
                    }));
            List<VenueScore> top = scored.stream()
                    .limit(Math.min(validLimit, scored.size()))
                    .collect(Collectors.toList());

            return top.stream()
                    .map(vs -> {
                        VenueDto venueDto = findVenueById(limitedForGemini, vs.venueId());
                        if (venueDto == null) {
                            return null;
                        }
                        return new VenueMatch(venueDto, vs.score() / 100.0, vs.explanation());
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Gemini ranking unavailable, falling back to deterministic ranking. Brief length: {}", brief.length(), e);
            // Fallback: return the top 'validLimit' venues from our deterministically sorted list
            // with a default score of 0.5 and a message indicating AI is unavailable
            return limitedForGemini.stream()
                    .limit(Math.min(validLimit, limitedForGemini.size()))
                    .map(v -> new VenueMatch(
                            v,
                            0.5,
                            "AI ranking is temporarily unavailable."
                    ))
                    .collect(Collectors.toList());
        }
    }

    private String buildParsePrompt(String brief) {
        return """
                You are an expert event planner. Extract the following information from the client's brief.
                If a piece of information is not mentioned, set its value to null.
                Return ONLY a JSON object with the following keys (use null for missing values):
                - aesthetic: string
                - locationPreferences: string
                - neighbourhood: string
                - indoorOutdoor: string ("indoor", "outdoor", or "both")
                - sizeM2: integer
                - budgetPerDay: number
                - requiredAmenities: list of strings
                - shootType: string
                - numPeople: integer
                - lightingPreferences: string
                - accessibility: string
                - timingDatePreferences: string
                - tags: list of strings
                - confidenceScore: number between 0 and 1

                Do not include any extra text, markdown, or formatting.
                Brief: """ + blankSafe(brief);
    }

    private String blankSafe(String s) {
        return s == null ? "" : s;
    }

    private List<VenueDto> filterVenues(List<VenueDto> venues, ParsedBrief parsed) {
        return venues.stream()
                .filter(v -> matchesBudget(v, parsed))
                .filter(v -> matchesNeighbourhood(v, parsed))
                .filter(v -> matchesSize(v, parsed))
                .filter(v -> matchesTags(v, parsed))
                .collect(Collectors.toList());
    }

    private boolean matchesBudget(VenueDto v, ParsedBrief parsed) {
        BigDecimal budget = parsed.budgetPerDay();
        if (budget != null) {
            BigDecimal venueRate = v.dayRateEur();
            if (venueRate == null) {
                // If venue rate is null, we cannot determine if it's within budget -> exclude
                return false;
            }
            return venueRate.compareTo(budget) <= 0;
        }
        return true;
    }

    private boolean matchesNeighbourhood(VenueDto v, ParsedBrief parsed) {
        String neighbourhood = parsed.neighbourhood();
        if (neighbourhood != null && !neighbourhood.isEmpty()) {
            String wanted = neighbourhood.trim().toLowerCase();
            String actual = v.neighbourhood();
            if (actual == null) {
                return false;
            }
            return actual.toLowerCase().contains(wanted);
        }
        return true;
    }

    private boolean matchesSize(VenueDto v, ParsedBrief parsed) {
        Integer sizeM2 = parsed.sizeM2();
        if (sizeM2 != null) {
            int venueSize = v.sizeM2(); // primitive int, never null
            return venueSize >= sizeM2;
        }
        return true;
    }

    private boolean matchesTags(VenueDto v, ParsedBrief parsed) {
        List<String> required = parsed.tags();
        if (required != null && !required.isEmpty()) {
            List<String> requiredTrimmed = required.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (requiredTrimmed.isEmpty()) {
                return true;
            }
            List<String> venueTags = v.tags() != null ? v.tags() : Collections.emptyList();
            List<String> venueTagsLower = venueTags.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            return requiredTrimmed.stream()
                    .map(String::toLowerCase)
                    .allMatch(venueTagsLower::contains);
        }
        return true;
    }

    private List<VenueDto> limitVenuesForGemini(List<VenueDto> venues, int maxSize) {
        if (venues.size() <= maxSize) {
            return venues;
        }
        // Sort by a combination of factors: lower cost, larger size, more amenities, then name
        return venues.stream()
                .sorted(Comparator
                        .comparingDouble((VenueDto v) -> v.dayRateEur() != null ? v.dayRateEur().doubleValue() : Double.MAX_VALUE)
                        .thenComparing((VenueDto v) -> -v.sizeM2()) // descending size
                        .thenComparing((VenueDto v) -> -(v.tags() != null ? v.tags().size() : 0)) // descending number of tags
                        .thenComparing(VenueDto::name))
                .limit(maxSize)
                .collect(Collectors.toList());
    }

    private String buildRankingPrompt(ParsedBrief parsed, List<VenueDto> venues) {
        // Create a simplified version of venues for the prompt to reduce token size
        List<Map<String, Object>> venueInfo = venues.stream()
                .map(v -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", v.id());
                    map.put("name", v.name());
                    map.put("neighbourhood", v.neighbourhood());
                    map.put("sizeM2", v.sizeM2());
                    map.put("dayRateEur", v.dayRateEur());
                    // Limit tags to first 10
                    List<String> tags = v.tags();
                    if (tags != null && !tags.isEmpty()) {
                        if (tags.size() > 10) {
                            map.put("tags", tags.subList(0, 10));
                        } else {
                            map.put("tags", tags);
                        }
                    } else {
                        map.put("tags", Collections.emptyList());
                    }
                    // Truncate description to 200 characters
                    String description = v.description();
                    if (description != null && !description.isEmpty()) {
                        if (description.length() > 200) {
                            map.put("description", description.substring(0, 200) + "...");
                        } else {
                            map.put("description", description);
                        }
                    } else {
                        map.put("description", "");
                    }
                    return map;
                })
                .collect(Collectors.toList());

        String prompt = """
                You are an expert venue matcher. Given the following parsed user preferences:
                %s

                And the following list of venues (each with id, name, neighbourhood, sizeM2, dayRateEur, tags, description):
                %s

                Rank the venues by suitability for the user's preferences.
                For each venue, provide a score from 0 to 100 (higher is better) and a short explanation (max 2 sentences) why it matches or does not match.
                Return ONLY a JSON array of objects with the following fields:
                - venueId: the venue's id
                - score: number between 0 and 100
                - explanation: string

                Do not include any extra text.
                """.formatted(
                escaper(parsed),
                escaper(venueInfo)
        );
        return prompt;
    }

    private String escaper(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<VenueScore> rankWithGemini(ParsedBrief parsed, List<VenueDto> venues) throws Exception {
        String prompt = buildRankingPrompt(parsed, venues);
        String response = geminiClient.generateContent(prompt);
        String jsonText = extractTextFromGeminiResponse(response);
        JsonNode root = objectMapper.readTree(jsonText);
        if (!root.isArray()) {
            throw new IllegalArgumentException("Expected JSON array from Gemini");
        }
        List<VenueScore> scores = new ArrayList<>();
        for (JsonNode node : root) {
            if (node.isObject()) {
                String venueId = node.get("venueId") != null ? node.get("venueId").asText() : null;
                double score = node.get("score") != null ? node.get("score").asDouble() : 0.0;
                String explanation = node.get("explanation") != null ? node.get("explanation").asText() : "";
                if (venueId != null) {
                    scores.add(new VenueScore(venueId, score, explanation));
                }
            }
        }
        return scores;
    }

    private VenueDto findVenueById(List<VenueDto> list, String venueIdStr) {
        try {
            UUID id = UUID.fromString(venueIdStr);
            return list.stream()
                    .filter(v -> v.id().equals(id))
                    .findFirst()
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extracts the text payload from a Gemini API response.
     * Expected format: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
     * Also strips markdown code fences if present.
     * Handles safety ratings and finish reasons.
     */
    private String extractTextFromGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.size() == 0) {
                // Check for prompt feedback
                JsonNode promptFeedback = root.path("promptFeedback");
                if (!promptFeedback.isMissingNode()) {
                    throw new IllegalArgumentException("Gemini request blocked due to safety reasons: " + promptFeedback.toPrettyString());
                }
                throw new IllegalArgumentException("No candidates in Gemini response");
            }
            JsonNode firstCandidate = candidates.get(0);
            // Check finish reason
            JsonNode finishReason = firstCandidate.path("finishReason");
            if (!finishReason.isMissingNode()) {
                String reason = finishReason.asText();
                if (!"STOP".equalsIgnoreCase(reason) && !"MAX_TOKENS".equalsIgnoreCase(reason)) {
                    // Log other reasons but continue if we have content
                    log.warn("Gemini finish reason: {}", reason);
                }
            }
            JsonNode safetyRatings = firstCandidate.path("safetyRatings");
            if (!safetyRatings.isMissingNode() && safetyRatings.isArray()) {
                for (JsonNode rating : safetyRatings) {
                    String category = rating.path("category").asText();
                    String probability = rating.path("probability").asText();
                    if ("HIGH".equalsIgnoreCase(probability) || "MEDIUM".equalsIgnoreCase(probability)) {
                        log.warn("Safety rating - category: {}, probability: {}", category, probability);
                    }
                }
            }
            JsonNode content = firstCandidate.path("content");
            JsonNode parts = content.path("parts");
            if (!parts.isArray() || parts.size() == 0) {
                throw new IllegalArgumentException("No parts in Gemini response");
            }
            JsonNode textNode = parts.get(0).path("text");
            if (textNode.isMissingNode() || !textNode.isTextual()) {
                throw new IllegalArgumentException("No text found in Gemini response");
            }
            String text = textNode.asText();
            // Strip markdown code fences if present
            text = stripMarkdownCodeFences(text);
            return text;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    /**
     * Removes markdown code fences (```json ... ```) from the text.
     * Handles optional language identifier and whitespace.
     */
    private String stripMarkdownCodeFences(String text) {
        Matcher matcher = MARKDOWN_FENCE_PATTERN.matcher(text);
        if (matcher.matches()) {
            return matcher.group(1); // Return the content inside the fences
        }
        // Also handle the case where the fences are at the start and end but there might be extra text outside?
        // We'll just return the original if no match.
        return text;
    }

    /* Inner record for scoring result from Gemini */
    public static record VenueScore(
            String venueId,
            double score,
            String explanation
    ) {
        public VenueScore {
            Objects.requireNonNull(venueId, "venueId must not be null");
            if (score < 0.0 || score > 100.0) {
                throw new IllegalArgumentException("score must be between 0 and 100");
            }
            Objects.requireNonNull(explanation, "explanation must not be null");
        }
    }
}