package com.eema.backend.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Validates that the Gemini API key is present at application startup.
 * Logs an INFO message confirming the key is present (without exposing the key value).
 * Matching remains optional; the service reports a controlled error when invoked without a key.
 */
@Component
public class GeminiClientInitializer {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientInitializer.class);

    private final GeminiProperties properties;

    @Autowired
    public GeminiClientInitializer(GeminiProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validateApiKeyPresence() {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini integration is disabled because no API key is configured");
            return;
        }
        log.info("Gemini integration is configured");
    }
}
