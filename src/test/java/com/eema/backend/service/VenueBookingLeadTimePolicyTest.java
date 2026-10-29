package com.eema.backend.service;

import com.eema.backend.entity.Venue;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VenueBookingLeadTimePolicyTest {
    private final Venue venue = Venue.builder().timezone("Europe/Amsterdam").minimumLeadTimeMinutes(1440).build();
    private final Instant now = Instant.parse("2026-08-27T08:00:00Z"); // 10:00 Amsterdam
    private final VenueBookingLeadTimePolicy policy = new VenueBookingLeadTimePolicy(Clock.fixed(now, ZoneOffset.UTC));

    @Test void allowsTwentyFiveHoursAndExactBoundary() {
        assertThat(policy.isSatisfied(venue, LocalDate.of(2026, 8, 28), LocalTime.of(11, 0))).isTrue();
        assertThat(policy.isSatisfied(venue, LocalDate.of(2026, 8, 28), LocalTime.of(10, 0))).isTrue();
    }

    @Test void rejectsOneMinuteInsideBoundary() {
        assertThat(policy.isSatisfied(venue, LocalDate.of(2026, 8, 28), LocalTime.of(9, 59))).isFalse();
        assertThatThrownBy(() -> policy.validate(venue, LocalDate.of(2026, 8, 28), LocalTime.of(9, 59)))
                .isInstanceOf(VenueBookingLeadTimePolicy.MinimumLeadTimeNotMetException.class)
                .hasMessageContaining("24 hours");
    }

    @Test void usesElapsedTimeAcrossAmsterdamDstChange() {
        Instant beforeSpringChange = Instant.parse("2026-03-28T09:00:00Z"); // 10:00 CET
        VenueBookingLeadTimePolicy dstPolicy = new VenueBookingLeadTimePolicy(Clock.fixed(beforeSpringChange, ZoneOffset.UTC));
        assertThat(dstPolicy.isSatisfied(venue, LocalDate.of(2026, 3, 29), LocalTime.of(10, 0))).isFalse();
        assertThat(dstPolicy.isSatisfied(venue, LocalDate.of(2026, 3, 29), LocalTime.of(11, 0))).isTrue();
    }
}
