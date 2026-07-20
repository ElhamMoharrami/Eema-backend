package com.eema.backend.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * The parsed, structured output from Gemini when analyzing a scout's plain-text brief.
 * This is the contract between the Gemini brief parser and the DB filter builder.
 *
 * VenueFilterBuilder uses this to construct database queries.
 * If a field is null or empty, VenueFilterBuilder treats it as "no filter for this dimension".
 */
public record ParsedBrief(
    // Budget: how much willing to spend per day
    BigDecimal budgetMinEur,        // min budget (e.g., 1500)
    BigDecimal budgetMaxEur,        // max budget (e.g., 4000)

    // Physical requirements
    Integer sizeM2Min,              // minimum venue size in m² (e.g., 200)
    Integer sizeM2Max,              // maximum venue size in m² (can be null if no limit)

    // Location
    String neighbourhood,           // Amsterdam neighbourhood (e.g., "Noord", "Westerpark", or null)
    List<String> neighbourhoodOptions,  // multiple options (e.g., ["Noord", "NDSM"] — try ANY)

    // Aesthetics & features (tag-based)
    List<String> mustHaveTags,      // tags that must be present (AND logic): ["industrial", "warehouse"]
    List<String> niceToHaveTags,    // tags that are preferred but optional

    // Descriptive fields (for Gemini re-ranker context)
    String aesthetic,               // visual style: "moody", "bright", "minimalist", "luxury", etc.
    String lighting,                // lighting preference: "natural", "directional", "controlled", etc.
    String purpose,                 // what shoot: "product photography", "portrait", "commercial", etc.
    String briefSummary,            // 1-line summary of what scout wants

    // Confidence
    Double confidenceScore          // 0.0 to 1.0: how confident Gemini is in the parsing (optional)
) {}