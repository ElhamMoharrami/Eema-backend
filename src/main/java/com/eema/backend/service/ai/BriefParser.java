package com.eema.backend.service.ai;

/**
 * Service for parsing a free-text brief into a structured event brief.
 */
public interface BriefParser {
    /**
     * Parses the given brief into a structured event brief.
     *
     * @param brief the free-text brief
     * @return the parsed brief, or null if the brief is null or empty
     */
    EventBriefParsed parseBrief(String brief);
}