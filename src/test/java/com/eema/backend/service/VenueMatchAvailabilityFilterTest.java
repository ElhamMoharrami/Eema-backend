package com.eema.backend.service;

import com.eema.backend.dto.response.AvailabilityIntervalDto;
import com.eema.backend.entity.Venue;
import com.eema.backend.service.ai.ParsedBrief;
import com.eema.backend.service.ai.ParsedBriefFixtures;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VenueMatchAvailabilityFilterTest {

    private final VenueAvailabilityService availabilityService = mock(VenueAvailabilityService.class);
    private final VenueMatchAvailabilityFilter filter = new VenueMatchAvailabilityFilter(availabilityService);
    private final LocalDate date = LocalDate.of(2026, 8, 25);

    @Test
    void completeIntervalInsideAvailabilityPasses() {
        Venue venue = venue();
        when(availabilityService.getAvailableIntervals(venue.getId(), date))
                .thenReturn(List.of(interval(9, 13)));

        assertThat(filter.retainAvailable(List.of(venue), exact(10, 12)))
                .containsExactly(venue);
    }

    @Test
    void partialOverlapAndFullyUnavailableDateAreRejected() {
        Venue partial = venue();
        Venue closed = venue();
        when(availabilityService.getAvailableIntervals(partial.getId(), date))
                .thenReturn(List.of(interval(9, 13)));
        when(availabilityService.getAvailableIntervals(closed.getId(), date))
                .thenReturn(List.of());

        assertThat(filter.retainAvailable(List.of(partial, closed), exact(12, 14))).isEmpty();
    }

    @Test
    void noCompleteTimingPreservesCandidatesAndDoesNotQueryAvailability() {
        Venue first = venue();
        Venue second = venue();
        ParsedBrief ambiguous = ParsedBriefFixtures.fromJson(
                "{\"timingDatePreferences\":\"Friday afternoon\"}"
        );

        assertThat(filter.retainAvailable(List.of(first, second), ambiguous))
                .containsExactly(first, second);
        verifyNoInteractions(availabilityService);
    }

    private Venue venue() {
        return Venue.builder()
                .id(UUID.randomUUID())
                .timezone("Europe/Amsterdam")
                .build();
    }

    private ParsedBrief exact(int start, int end) {
        return ParsedBriefFixtures.fromJson("""
                {"requestedDate":"2026-08-25","requestedStartTime":"%02d:00:00",
                 "requestedEndTime":"%02d:00:00"}
                """.formatted(start, end));
    }

    private AvailabilityIntervalDto interval(int start, int end) {
        return new AvailabilityIntervalDto(LocalTime.of(start, 0), LocalTime.of(end, 0));
    }
}
