package com.eema.backend.service.ai;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GeminiClient {

    private final String apiKey;
    private final RestTemplate restTemplate;

    // Static final GenerationConfig to avoid recreation on every request
    private static final GenerationConfig GENERATION_CONFIG = new GenerationConfig(
            0.2f, 40, 0.95f, 1024, "application/json"
    );

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    public GeminiClient(GeminiProperties properties, RestTemplate restTemplate) {
        this.apiKey = properties.getApiKey();
        this.restTemplate = restTemplate;
    }

    public String generateContent(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key not configured");
        }

        String url = API_URL + "?key=" + apiKey;

        GeminiRequest request = new GeminiRequest(
                Collections.singletonList(
                        new GeminiContent(
                                Collections.singletonList(new GeminiPart(prompt))
                        )
                ),
                GENERATION_CONFIG
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

        // Retry logic for transient errors (5xx, 429) and network issues with jitter
        int maxAttempts = 3;
        long baseBackoffMillis = 500;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    return response.getBody();
                } else {
                    // If we get a non-2xx response that is not a retryable error, throw immediately
                    if (response.getStatusCode().value() != 429 &&
                            (response.getStatusCode().value() < 500 || response.getStatusCode().value() >= 600)) {
                        throw new RuntimeException("Gemini API error: " + response.getStatusCode() + " - " + response.getBody());
                    }
                    // Otherwise, treat as retryable (429 or 5xx)
                    lastException = new RuntimeException("Gemini API error: " + response.getStatusCode() + " - " + response.getBody());
                }
            } catch (HttpStatusCodeException e) {
                // Check if we should retry
                if (e.getStatusCode().value() == 429 ||
                        (e.getStatusCode().value() >= 500 && e.getStatusCode().value() < 600)) {
                    // Retryable
                    lastException = e;
                } else {
                    throw e;
                }
            } catch (ResourceAccessException e) {
                // Network issues, retry
                lastException = e;
            }

            if (attempt == maxAttempts) {
                // If we've exhausted retries, break to throw the last exception
                break;
            }

            // Wait before retrying with exponential backoff plus jitter
            long delay = baseBackoffMillis * (long) Math.pow(2, attempt - 1);
            long jitter = ThreadLocalRandom.current().nextLong(delay / 2); // up to 50% jitter
            try {
                TimeUnit.MILLISECONDS.sleep(delay + jitter);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during retry backoff", ie);
            }
        }

        // If we got here, all retries exhausted
        if (lastException != null) {
            throw new RuntimeException("Gemini API failed after " + maxAttempts + " attempts", lastException);
        }
        throw new RuntimeException("Unexpected state in retry logic");
    }

    /* Inner classes for request/response mapping */
    private static class GeminiRequest {
        private final java.util.List<GeminiContent> contents;
        private final GenerationConfig generationConfig;

        GeminiRequest(java.util.List<GeminiContent> contents, GenerationConfig generationConfig) {
            this.contents = contents;
            this.generationConfig = generationConfig;
        }
    }

    private static class GeminiContent {
        private final java.util.List<GeminiPart> parts;

        GeminiContent(java.util.List<GeminiPart> parts) {
            this.parts = parts;
        }
    }

    private static class GeminiPart {
        private final String text;

        GeminiPart(String text) {
            this.text = text;
        }
    }

    private static class GenerationConfig {
        private final float temperature;
        private final int topK;
        private final float topP;
        private final int maxOutputTokens;
        private final String responseMimeType;

        GenerationConfig(float temperature, int topK, float topP, int maxOutputTokens, String responseMimeType) {
            this.temperature = temperature;
            this.topK = topK;
            this.topP = topP;
            this.maxOutputTokens = maxOutputTokens;
            this.responseMimeType = responseMimeType;
        }
    }
}