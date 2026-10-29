package com.eema.backend.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ParsedBriefFixtures {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private ParsedBriefFixtures() {
    }

    public static ParsedBrief empty() {
        return fromJson("{}");
    }

    public static ParsedBrief fromJson(String json) {
        try {
            return MAPPER.readValue(json, ParsedBrief.class);
        } catch (Exception exception) {
            throw new AssertionError("Invalid ParsedBrief test fixture", exception);
        }
    }
}
