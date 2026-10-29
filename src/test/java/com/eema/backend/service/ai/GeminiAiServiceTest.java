package com.eema.backend.service.ai;

import com.eema.backend.dto.response.MatchResponse;
import com.eema.backend.entity.Venue;
import com.eema.backend.service.VenueFilterBuilder;
import com.eema.backend.service.VenueMatchAvailabilityFilter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GeminiAiServiceTest {

    @Test
    void ranksOnlyCandidatesReturnedByDeterministicFiltering() {
        BriefParser parser = mock(BriefParser.class);
        VenueFilterBuilder filter = mock(VenueFilterBuilder.class);
        VenueMatchAvailabilityFilter availability = mock(VenueMatchAvailabilityFilter.class);
        VenueRanker ranker = mock(VenueRanker.class);
        ParsedBrief parsed = ParsedBriefFixtures.empty();
        Venue eligible = mock(Venue.class);
        Venue incompatible = mock(Venue.class);
        MatchResponse ranked = new MatchResponse(List.of(), "brief", 0);

        when(parser.parseBrief("brief")).thenReturn(parsed);
        when(filter.findCandidateBatch(parsed, 0)).thenReturn(new VenueFilterBuilder.CandidateBatch(List.of(eligible), 0, false));
        when(availability.retainAvailable(List.of(eligible), parsed)).thenReturn(List.of(eligible));
        when(ranker.rank("brief", parsed, List.of(eligible))).thenReturn(ranked);

        MatchResponse result = new GeminiAiService(parser, filter, availability, ranker).searchVenues("brief");

        assertThat(result).isSameAs(ranked);
        verify(ranker).rank("brief", parsed, List.of(eligible));
        verifyNoInteractions(incompatible);
    }

    @Test
    void zeroEligibleCandidatesReturnsEmptyResponseWithoutCallingGeminiRanker() {
        BriefParser parser = mock(BriefParser.class);
        VenueFilterBuilder filter = mock(VenueFilterBuilder.class);
        VenueMatchAvailabilityFilter availability = mock(VenueMatchAvailabilityFilter.class);
        VenueRanker ranker = mock(VenueRanker.class);
        ParsedBrief parsed = ParsedBriefFixtures.empty();
        when(parser.parseBrief("brief")).thenReturn(parsed);
        when(filter.findCandidateBatch(parsed, 0)).thenReturn(new VenueFilterBuilder.CandidateBatch(List.of(), 0, false));
        when(availability.retainAvailable(List.of(), parsed)).thenReturn(List.of());

        MatchResponse result = new GeminiAiService(parser, filter, availability, ranker).searchVenues("brief");

        assertThat(result.venues()).isEmpty();
        assertThat(result.totalMatches()).isZero();
        verifyNoInteractions(ranker);
    }

    @Test
    void availabilityRemovalSkipsRankingAndIsNotRelaxed() {
        BriefParser parser = mock(BriefParser.class);
        VenueFilterBuilder filter = mock(VenueFilterBuilder.class);
        VenueMatchAvailabilityFilter availability = mock(VenueMatchAvailabilityFilter.class);
        VenueRanker ranker = mock(VenueRanker.class);
        ParsedBrief parsed = ParsedBriefFixtures.fromJson("""
                {"requestedDate":"2026-08-25","requestedStartTime":"10:00:00",
                 "requestedEndTime":"16:00:00"}
                """);
        Venue candidate = mock(Venue.class);
        when(parser.parseBrief("brief")).thenReturn(parsed);
        when(filter.findCandidateBatch(parsed, 0)).thenReturn(new VenueFilterBuilder.CandidateBatch(List.of(candidate), 0, false));
        when(availability.retainAvailable(List.of(candidate), parsed)).thenReturn(List.of());

        MatchResponse result = new GeminiAiService(parser, filter, availability, ranker).searchVenues("brief");

        assertThat(result.venues()).isEmpty();
        verifyNoInteractions(ranker);
    }

    @Test
    void onlyAvailableCandidatesReachRanking() {
        BriefParser parser = mock(BriefParser.class);
        VenueFilterBuilder filter = mock(VenueFilterBuilder.class);
        VenueMatchAvailabilityFilter availability = mock(VenueMatchAvailabilityFilter.class);
        VenueRanker ranker = mock(VenueRanker.class);
        ParsedBrief parsed = ParsedBriefFixtures.empty();
        Venue available = mock(Venue.class);
        Venue unavailable = mock(Venue.class);
        when(parser.parseBrief("brief")).thenReturn(parsed);
        when(filter.findCandidateBatch(parsed, 0)).thenReturn(new VenueFilterBuilder.CandidateBatch(List.of(available, unavailable), 0, false));
        when(availability.retainAvailable(List.of(available, unavailable), parsed)).thenReturn(List.of(available));
        when(ranker.rank("brief", parsed, List.of(available)))
                .thenReturn(new MatchResponse(List.of(), "brief", 0));

        new GeminiAiService(parser, filter, availability, ranker).searchVenues("brief");

        verify(ranker).rank("brief", parsed, List.of(available));
    }
}
