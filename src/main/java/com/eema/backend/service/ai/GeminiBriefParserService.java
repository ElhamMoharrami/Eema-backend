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

            Convert the user's venue brief into structured requirements
            that will be used in two stages:

            1. Hard requirements are used to filter venues from the database.
            2. Soft preferences are used later by an AI ranker.

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

              "shootType": null,
              "numPeople": null,

              "accessibility": null,
              "timingDatePreferences": null,

              "briefSummary": null,
              "confidenceScore": 0.0
            }

            General rules:

            - Return ONE complete valid JSON object.
            - Return JSON only.
            - Do not return markdown.
            - Do not return explanations outside the JSON.
            - Never invent requirements.
            - Unknown scalar fields must be null.
            - Unknown list fields must be [].
            - confidenceScore must be between 0.0 and 1.0.

            Budget rules:

            - All budget values must be numeric EUR values.
            - If the user provides a maximum budget only,
              put it in budgetMaxEur.
            - If the user provides a minimum budget only,
              put it in budgetMinEur.
            - If the user provides a range,
              populate both budgetMinEur and budgetMaxEur.
            - Never invent a missing budget boundary.

            Size rules:

            - All sizes must be represented in square metres.
            - If only a minimum size is requested,
              put it in sizeM2Min.
            - If only a maximum size is requested,
              put it in sizeM2Max.
            - If a range is given, populate both.
            - Never invent a missing size boundary.

            Location rules:

            - Put one explicitly requested neighbourhood
              in neighbourhood.
            - If the user gives multiple acceptable neighbourhoods,
              put them in neighbourhoodOptions.
            - Do not invent alternative neighbourhoods.

            Hard requirement rules:

            - mustHaveTags are database filtering constraints.
            - Only put a tag in mustHaveTags when it represents
              a concrete venue type or physical feature that should
              eliminate venues that do not have it.

            Good mustHaveTags examples:

            - warehouse
            - studio
            - gallery
            - rooftop
            - loft
            - kitchen
            - parking
            - loading-dock
            - high-ceiling
            - garden
            - canal-view

            - If the user says "I need a warehouse",
              "warehouse" belongs in mustHaveTags.

            - If the user says "must have parking",
              "parking" belongs in mustHaveTags.

            - If the user says "must have high ceilings",
              "high-ceiling" belongs in mustHaveTags.

            Soft preference rules:

            - Do NOT put aesthetic, mood, atmosphere,
              visual style, or subjective descriptors
              into mustHaveTags.

            These should normally go into aesthetic:

            - moody
            - industrial-looking
            - cinematic
            - luxury
            - elegant
            - minimalist
            - dramatic
            - warm
            - bright
            - raw
            - premium
            - rustic
            - modern

            - Example:
              "I need a moody industrial warehouse"

              should normally become:

              aesthetic = "moody industrial"
              mustHaveTags = ["warehouse"]

              Do NOT automatically put "moody"
              or "industrial" into mustHaveTags.

            - niceToHaveTags are optional concrete features
              that should improve ranking but should NOT
              eliminate venues during database filtering.

            - Example:
              "A warehouse, preferably with daylight"

              should normally become:

              mustHaveTags = ["warehouse"]
              niceToHaveTags = ["daylight"]

            Lighting rules:

            - Put lighting style or preference in lighting.
            - If lighting is merely preferred, do not make it
              a hard tag.
            - Only make a lighting-related feature a mustHaveTag
              if the user explicitly states it is mandatory.

            Amenities rules:

            - Put explicitly required practical amenities
              in requiredAmenities.
            - Do not invent amenities.

            Shoot type rules:

            - Extract the production purpose into shootType.
            - Examples include:
              fashion shoot,
              product photography,
              portrait shoot,
              commercial,
              film production,
              interview,
              event.

            Summary rules:

            - briefSummary should be one concise sentence
              summarizing the user's actual request.
            - Do not add requirements that were not present.

            Final decision rule:

            When uncertain whether something is a hard database filter
            or a soft preference, prefer treating it as a soft preference.
            It is better to return several plausible venue candidates
            for ranking than to eliminate good venues too early.

            Return the complete JSON object only.
            """.formatted(brief);
    }
}