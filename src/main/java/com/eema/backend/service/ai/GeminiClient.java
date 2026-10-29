package com.eema.backend.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GeminiClient {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiClient.class);

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + "gemini-3.6-flash:generateContent";

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 500;

    private static final Pattern MARKDOWN_FENCE_PATTERN =
            Pattern.compile(
                    "^\\s*```(?:json)?\\s*([\\s\\S]*?)\\s*```\\s*$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final GenerationConfig GENERATION_CONFIG =
            new GenerationConfig(
                    0.2f,
                    40,
                    0.95f,
                    2048,
                    "application/json"
            );

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            GeminiProperties properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.apiKey = properties.getApiKey();
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateContent(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key not configured"
            );
        }

        String url = API_URL;

        GeminiRequest request = new GeminiRequest(
                List.of(
                        new GeminiContent(
                                List.of(
                                        new GeminiPart(prompt)
                                )
                        )
                ),
                GENERATION_CONFIG
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<GeminiRequest> entity =
                new HttpEntity<>(request, headers);

        Exception lastException = null;

        for (int attempt = 1;
             attempt <= MAX_ATTEMPTS;
             attempt++) {

            try {
                ResponseEntity<String> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                entity,
                                String.class
                        );

                if (response.getStatusCode().is2xxSuccessful()) {
                    return response.getBody();
                }

                lastException = new AiServiceException(
                        "Gemini returned status "
                                + response.getStatusCode()
                );

            } catch (HttpStatusCodeException e) {

                if (!isRetryable(e.getStatusCode())) {
                    throw new AiServiceException(
                            "Gemini API request failed",
                            e
                    );
                }

                lastException = e;

            } catch (ResourceAccessException e) {
                lastException = e;
            }

            if (attempt < MAX_ATTEMPTS) {
                sleepBeforeRetry(attempt);
            }
        }

        throw new AiServiceException(
                "Gemini API failed after "
                        + MAX_ATTEMPTS
                        + " attempts",
                lastException
        );
    }

    public String extractGeneratedText(String response) {
        if (response == null || response.isBlank()) {
            throw new AiServiceException(
                    "Gemini returned an empty response"
            );
        }

        try {
            JsonNode root =
                    objectMapper.readTree(response);

            JsonNode candidates =
                    root.path("candidates");

            if (!candidates.isArray()
                    || candidates.isEmpty()) {
                throw new AiServiceException(
                        "Gemini returned no candidates"
                );
            }

            JsonNode parts =
                    candidates
                            .get(0)
                            .path("content")
                            .path("parts");

            if (!parts.isArray() || parts.isEmpty()) {
                throw new AiServiceException(
                        "Gemini returned no content"
                );
            }

            JsonNode textNode =
                    parts.get(0).path("text");

            if (!textNode.isTextual()) {
                throw new AiServiceException(
                        "Gemini response contained no text"
                );
            }

            return stripCodeFence(
                    textNode.asText()
            );

        } catch (AiServiceException e) {
            throw e;

        } catch (Exception e) {
            throw new AiServiceException(
                    "Invalid Gemini response",
                    e
            );
        }
    }

    private boolean isRetryable(
            HttpStatusCode status
    ) {
        return status.value() == 429
                || status.is5xxServerError();
    }

    private void sleepBeforeRetry(int attempt) {
        long delay =
                BASE_BACKOFF_MS
                        * (1L << (attempt - 1));

        long jitter =
                ThreadLocalRandom.current()
                        .nextLong(
                                Math.max(1, delay / 2)
                        );

        try {
            TimeUnit.MILLISECONDS.sleep(
                    delay + jitter
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new AiServiceException(
                    "Gemini retry interrupted",
                    e
            );
        }
    }

    private String stripCodeFence(String text) {
        Matcher matcher =
                MARKDOWN_FENCE_PATTERN.matcher(text);

        return matcher.matches()
                ? matcher.group(1).trim()
                : text.trim();
    }

    private record GeminiRequest(
            List<GeminiContent> contents,
            GenerationConfig generationConfig
    ) {
    }

    private record GeminiContent(
            List<GeminiPart> parts
    ) {
    }

    private record GeminiPart(
            String text
    ) {
    }

    private record GenerationConfig(
            float temperature,
            int topK,
            float topP,
            int maxOutputTokens,
            String responseMimeType
    ) {
    }
}