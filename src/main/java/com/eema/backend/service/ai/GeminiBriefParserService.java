package com.eema.backend.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiBriefParserService implements BriefParser {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public ParsedBrief parseBrief(String brief) {

        if (brief == null || brief.isBlank()) {
            throw new IllegalArgumentException(
                    "Brief must not be blank"
            );
        }

        try {
            String prompt =
                    buildPrompt(brief);

            String rawResponse =
                    geminiClient.generateContent(prompt);

            String json =
                    geminiClient.extractGeneratedText(
                            rawResponse
                    );

            log.debug(
                    "Gemini parsed brief JSON: {}",
                    json
            );

            ParsedBrief parsedBrief =
                    parseJson(json);

            validateParsedBrief(parsedBrief);

            if (parsedBrief.confidenceScore() != null
                    && parsedBrief.confidenceScore() < 0.5) {

                log.warn(
                        "Gemini parsed brief with low confidence: {}",
                        parsedBrief.confidenceScore()
                );
            }

            return parsedBrief;

        } catch (AiServiceException e) {
            throw e;

        } catch (Exception e) {
            throw new AiServiceException(
                    "Failed to parse venue brief",
                    e
            );
        }
    }

    private ParsedBrief parseJson(String json) {

        if (json == null || json.isBlank()) {
            throw new AiServiceException(
                    "Gemini returned empty parsed brief"
            );
        }

        try {
            JsonNode root =
                    objectMapper.readTree(json);

            if (!root.isObject()) {
                throw new AiServiceException(
                        "Gemini parsed brief must be a JSON object"
                );
            }

            return objectMapper.treeToValue(
                    root,
                    ParsedBrief.class
            );

        } catch (JsonProcessingException e) {

            log.error(
                    "Gemini returned invalid JSON: {}",
                    json
            );

            throw new AiServiceException(
                    "Gemini returned malformed JSON",
                    e
            );
        }
    }

    private void validateParsedBrief(
            ParsedBrief parsed
    ) {

        if (parsed == null) {
            throw new AiServiceException(
                    "Gemini returned null parsed brief"
            );
        }

        if (parsed.sizeM2Min() != null
                && parsed.sizeM2Min() < 0) {

            throw new AiServiceException(
                    "Gemini returned invalid minimum size"
            );
        }

        if (parsed.sizeM2Max() != null
                && parsed.sizeM2Max() < 0) {

            throw new AiServiceException(
                    "Gemini returned invalid maximum size"
            );
        }

        if (parsed.sizeM2Min() != null
                && parsed.sizeM2Max() != null
                && parsed.sizeM2Min()
                > parsed.sizeM2Max()) {

            throw new AiServiceException(
                    "Gemini returned invalid size range"
            );
        }

        if (parsed.budgetMinEur() != null
                && parsed.budgetMaxEur() != null
                && parsed.budgetMinEur()
                .compareTo(
                        parsed.budgetMaxEur()
                ) > 0) {

            throw new AiServiceException(
                    "Gemini returned invalid budget range"
            );
        }

        if (parsed.confidenceScore() != null
                && (
                parsed.confidenceScore() < 0.0
                        || parsed.confidenceScore() > 1.0
        )) {

            throw new AiServiceException(
                    "Gemini returned invalid confidence score"
            );
        }
    }

    private String buildPrompt(String brief) {

            return """
            You are an expert film and photography location scout.

            Convert the brief into structured requirements. Spring will enforce hard
            requirements against authoritative venue data; a later ranker will use preferences.

            USER BRIEF:
            %s

            Return exactly one complete JSON object using this schema:

            {
              "aesthetic": null,
              "lighting": null,

              "sizeM2Min": null,
              "sizeM2Max": null,

              "budgetMinEur": null,
              "budgetMaxEur": null,

              "neighbourhood": null,
              "neighbourhoodOptions": [],

              "indoorOutdoor": null,

              "requiredAmenities": [],
              "mustHaveTags": [],
              "niceToHaveTags": [],

              "productionActivity": null,
              "numPeople": null,
              "requiresNaturalLight": null,
              "prefersNaturalLight": null,
              "requiresBlackout": null,
              "prefersBlackout": null,
              "minimumCeilingHeightM": null,
              "requiresLoadingAccess": null,
              "prefersLoadingAccess": null,
              "requiresParking": null,
              "prefersParking": null,
              "minimumParkingSpaces": null,
              "requiresWheelchairAccessibility": null,
              "requiresLift": null,
              "prefersLift": null,
              "requiresBathrooms": null,
              "requiresSoundRecordingSuitable": null,
              "timingDatePreferences": null,

              "briefSummary": null,
              "confidenceScore": 0.0
            }

            - Return ONE complete valid JSON object.
            - Return JSON only.
            - Never invent requirements.
            - Unknown scalar fields must be null.
            - Unknown list fields must be [].
            - confidenceScore must be between 0.0 and 1.0.

            Requirement strength:
            - "need", "must", "required", and "important" normally set requires... = true.
            - "prefer", "would be nice", and "would be useful" set prefers... = true only.
            - Do not set a required field merely because a feature is mentioned.
            - If strength is unclear, use a preference rather than a requirement.

            Production activity must be exactly one of PHOTO, FILM, VIDEO, INTERVIEW,
            COMMERCIAL, MUSIC_VIDEO, CONTENT_CREATION, or null. Map conservatively:
            fashion/product/portrait shoot -> PHOTO; brand commercial -> COMMERCIAL;
            interview -> INTERVIEW; music video -> MUSIC_VIDEO; social media content ->
            CONTENT_CREATION. If ambiguous, return null. Never return EVENTS.

            Structured facts must use typed fields, not tags. Never put natural light,
            blackout, parking, loading access, accessibility, lift, bathrooms, sound
            recording, crew capacity, or ceiling height in mustHaveTags/niceToHaveTags.
            Tags are only for concrete unstructured venue concepts such as warehouse,
            studio, gallery, rooftop, loft, kitchen, garden, industrial, or canal-view.

            Examples:
            - "I need a daylight studio for 20 people with loading access":
              numPeople=20, requiresNaturalLight=true, requiresLoadingAccess=true,
              mustHaveTags=["studio"]. Set productionActivity only if production intent is clear.
            - "Industrial warehouse, parking would be nice":
              mustHaveTags=["warehouse"], aesthetic="industrial", prefersParking=true.
            - "Interview for 6 people, needs clean audio":
              productionActivity="INTERVIEW", numPeople=6,
              requiresSoundRecordingSuitable=true.

            Use metres for minimumCeilingHeightM, square metres for size, whole numbers
            for people/parking, and numeric EUR for budgets. Never invent boundaries,
            neighbourhood alternatives, amenities, or a production activity.

            lighting may retain subjective lighting style; requiredAmenities may contain
            only requirements that have no dedicated typed field. briefSummary must be one
            concise factual sentence. Parser confidence is interpretation confidence only.

            Return the complete JSON object only.
            """.formatted(brief);
    }
}
