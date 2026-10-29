package com.eema.backend.service.ai;

import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.entity.Venue;
import com.eema.backend.service.VenueFilterBuilder;
import com.eema.backend.service.VenueMatchAvailabilityFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAiService implements AiService {

    private final BriefParser briefParser;
    private final VenueFilterBuilder venueFilterBuilder;
    private final VenueMatchAvailabilityFilter availabilityFilter;
    private final VenueRanker venueRanker;

    @Override
    public MatchResponse searchVenues(String brief) {

        if (brief == null || brief.isBlank()) {
            throw new IllegalArgumentException(
                    "Brief must not be blank"
            );
        }

        try {
            log.info(
                    "Starting venue matching: briefLength={}",
                    brief.length()
            );

            // Step 1:
            // Convert the user's free-text brief into structured requirements.
            ParsedBrief parsedBrief =
                    briefParser.parseBrief(brief);

            log.debug(
                    "Brief parsed successfully: {}",
                    parsedBrief
            );

            // Step 2:
            // Query the database using the hard requirements extracted by Gemini.
            List<Venue> candidates =
                    venueFilterBuilder.findCandidateVenues(
                            parsedBrief
                    );

            log.info(
                    "Database filtering returned {} candidates",
                    candidates.size()
            );

            candidates = availabilityFilter.retainAvailable(candidates, parsedBrief);

            log.info(
                    "Availability filtering returned {} candidates; exactInterval={}",
                    candidates.size(),
                    availabilityFilter.hasExactInterval(parsedBrief)
            );

            if (candidates.isEmpty()) {
                return new MatchResponse(
                        List.of(),
                        brief,
                        0
                );
            }

            // Step 3:
            // Send only the real DB candidates back to Gemini
            // for semantic/fuzzy ranking.
            MatchResponse response =
                    venueRanker.rank(
                            brief,
                            parsedBrief,
                            candidates
                    );

            log.info(
                    "Venue matching completed: matches={}",
                    response.venues().size()
            );

            return response;

        } catch (AiServiceException e) {
            throw e;

        } catch (Exception e) {
            log.error(
                    "Venue matching failed",
                    e
            );

            throw new AiServiceException(
                    "Venue matching failed",
                    e
            );
        }
    }
}
