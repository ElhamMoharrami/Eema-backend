package com.eema.backend.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiBriefParserService implements BriefParser {

    private static final Logger log = LoggerFactory.getLogger(GeminiBriefParserService.class);
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    // Regex to match markdown code fences: optional whitespace, triple backticks, optional language identifier, newline, content, newline, triple backticks, optional whitespace
    private static final Pattern MARKDOWN_FENCE_PATTERN = Pattern.compile(
            "^\\s*```[a-zA-Z0-9]*\\s*\\r?\\n([\\s\\S]*?)\\r?\\n\\s*```\\s*$",
            Pattern.DOTALL
    );

    public GeminiBriefParserService(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public EventBriefParsed parseBrief(String brief) {
        if (brief == null || blankSafe(brief).isEmpty()) {
            return null;
        }
        String prompt = buildParsePrompt(brief);
        try {
            String response = geminiClient.generateContent(prompt);
            String jsonText = extractTextFromGeminiResponse(response);
            JsonNode jsonNode = objectMapper.readTree(jsonText);
            if (jsonNode.isObject()) {
                EventBriefParsed parsed = objectMapper.treeToValue(jsonNode, EventBriefParsed.class);
                // Log if confidence score is low
                if (parsed.confidenceScore() != null && parsed.confidenceScore() < 0.5) {
                    log.warn("Low confidence score in parsed brief: {}", parsed.confidenceScore());
                }
                return parsed;
            } else {
                log.warn("Gemini response is not a JSON object. Brief length: {}", brief.length());
                // Return an empty parsed brief (all nulls) to allow the process to proceed with no filters
                return new EventBriefParsed(
                        null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null
                );
            }
        } catch (Exception e) {
            log.warn("Failed to parse brief with Gemini. Brief length: {}", brief.length(), e);
            // Return an empty parsed brief (all nulls) to allow the process to proceed with no filters
            return new EventBriefParsed(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null
            );
        }
    }

    private String buildParsePrompt(String brief) {
        return """
                You are an expert event planner. Extract the following information from the client's brief.
                If a piece of information is not mentioned, set its value to null.
                For budget, if a range is mentioned (e.g., \"€2000-4000\" or \"€2000 to €4000\"), extract the min and max values and the currency (e.g., EUR, USD, GBP).
                If only a single value is mentioned, set both min and max to that value.
                If the currency is not specified, assume EUR.
                Return ONLY a JSON object with the following keys (use null for missing values):
                - aesthetic: string
                - lighting: string
                - size: integer (square meters, if mentioned)
                - budget: object with min (number), max (number), currency (string)
                - tags: list of strings
                - shootType: string
                - locationPreferences: string
                - neighbourhood: string
                - indoorOutdoor: string (\"indoor\", \"outdoor\", or \"both\")
                - requiredAmenities: list of strings
                - numPeople: integer
                - accessibility: string
                - timingDatePreferences: string
                - confidenceScore: number between 0 and 1

                Do not include any extra text, markdown, or formatting.
                Brief: """ + blankSafe(brief);
    }

    private String blankSafe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Extracts the text payload from a Gemini API response.
     * Expected format: { \"candidates\": [{ \"content\": { \"parts\": [{ \"text\": \"...\" }] }] } }
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
}