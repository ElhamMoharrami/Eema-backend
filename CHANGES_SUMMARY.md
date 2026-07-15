### Changes Made to GeminiAiService.java

1. **Improved Exception Logging**:
   - Changed `log.warn("Failed to parse brief with Gemini. Brief length: {}", brief.length());` to `log.warn("Failed to parse brief with Gemini. Brief length: {}", brief.length(), e);` in the catch block of `parseBrief`.
   - Changed `log.warn("Gemini ranking unavailable, falling back to deterministic ranking. Brief length: {}", brief.length());` to `log.warn("Gemini ranking unavailable, falling back to deterministic ranking. Brief length: {}", brief.length(), e);` in the catch block of `match`.

2. **Fallback for Parsing Failures**:
   - In `parseBrief`, when Gemini returns a non-JSON object or an exception occurs, we now return an empty `ParsedBrief` (all fields set to `null`) instead of `null`. This allows the search to proceed with no filters (all venues pass the deterministic filters) and proceed to the ranking step.
   - Note: Empty or blank briefs still return `null`, causing `match()` to return an empty list (as per spec).

3. **Fallback for Ranking Failures**:
   - In the `catch` block of `match` (when Gemini ranking throws an exception), we now return a list of `VenueMatch` objects built from the `limitedForGemini` list (the top 50 venues after deterministic filtering and scoring). Each venue gets:
     - A score of `0.5` (neutral midpoint)
     - Explanation: "AI ranking is temporarily unavailable."
   - The number of results is limited to the requested `limit` (clamped between 1 and 20).
   - This provides a graceful degradation when the AI service is unavailable, showing venues with a default score and explanatory message.

### Build Status
The project compiles successfully with `mvn compile -q`.

These changes address the three specific points raised in the feedback while maintaining the existing functionality and requirements.