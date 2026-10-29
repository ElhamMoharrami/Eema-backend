package com.eema.backend.service.ai;

import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.entity.Venue;
import com.eema.backend.service.VenueFilterBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GeminiAiServiceTest {

    @Test
    void ranksOnlyCandidatesReturnedByDeterministicFiltering() {
        BriefParser parser = mock(BriefParser.class);
        VenueFilterBuilder filter = mock(VenueFilterBuilder.class);
        VenueRanker ranker = mock(VenueRanker.class);
        ParsedBrief parsed = ParsedBriefFixtures.empty();
        Venue eligible = mock(Venue.class);
        Venue incompatible = mock(Venue.class);
        MatchResponse ranked = new MatchResponse(List.of(), "brief", 0);

        when(parser.parseBrief("brief")).thenReturn(parsed);
        when(filter.findCandidateVenues(parsed)).thenReturn(List.of(eligible));
        when(ranker.rank("brief", parsed, List.of(eligible))).thenReturn(ranked);

        MatchResponse result = new GeminiAiService(parser, filter, ranker).searchVenues("brief");

        assertThat(result).isSameAs(ranked);
        verify(ranker).rank("brief", parsed, List.of(eligible));
        verifyNoInteractions(incompatible);
    }

    @Test
    void zeroEligibleCandidatesReturnsEmptyResponseWithoutCallingGeminiRanker() {
        BriefParser parser = mock(BriefParser.class);
        VenueFilterBuilder filter = mock(VenueFilterBuilder.class);
        VenueRanker ranker = mock(VenueRanker.class);
        ParsedBrief parsed = ParsedBriefFixtures.empty();
        when(parser.parseBrief("brief")).thenReturn(parsed);
        when(filter.findCandidateVenues(parsed)).thenReturn(List.of());

        MatchResponse result = new GeminiAiService(parser, filter, ranker).searchVenues("brief");

        assertThat(result.venues()).isEmpty();
        assertThat(result.totalMatches()).isZero();
        verifyNoInteractions(ranker);
    }
}
