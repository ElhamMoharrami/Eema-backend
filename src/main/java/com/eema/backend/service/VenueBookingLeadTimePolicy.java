package com.eema.backend.service;

import com.eema.backend.entity.Venue;
import com.eema.backend.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
public class VenueBookingLeadTimePolicy {
    private final Clock clock;

    public VenueBookingLeadTimePolicy(Clock clock) { this.clock = clock; }

    public boolean isSatisfied(Venue venue, LocalDate date, LocalTime startTime) {
        return isSatisfied(venue, date, startTime, clock.instant());
    }

    public boolean isSatisfied(Venue venue, LocalDate date, LocalTime startTime, Instant now) {
        return !bookingStart(venue, date, startTime).isBefore(earliestStart(venue, now));
    }

    public void validate(Venue venue, LocalDate date, LocalTime startTime) {
        if (!isSatisfied(venue, date, startTime)) throw notMet(venue);
    }

    public Instant earliestStart(Venue venue) { return earliestStart(venue, clock.instant()); }

    public Instant earliestStart(Venue venue, Instant now) {
        return now.plus(Duration.ofMinutes(requiredMinutes(venue)));
    }

    public Instant bookingStart(Venue venue, LocalDate date, LocalTime startTime) {
        if (date == null || startTime == null) throw new IllegalArgumentException("Booking start is required");
        try { return ZonedDateTime.of(date, startTime, ZoneId.of(venue.getTimezone())).toInstant(); }
        catch (DateTimeException | NullPointerException exception) {
            throw new IllegalStateException("Venue timezone is invalid", exception);
        }
    }

    public MinimumLeadTimeNotMetException notMet(Venue venue) {
        int minutes = requiredMinutes(venue);
        String notice = minutes % 60 == 0 ? (minutes / 60) + " hour" + (minutes == 60 ? "" : "s")
                : minutes + " minutes";
        return new MinimumLeadTimeNotMetException(
                "This venue requires bookings to be requested at least " + notice + " before the start time");
    }

    private int requiredMinutes(Venue venue) {
        Integer minutes = venue.getMinimumLeadTimeMinutes();
        if (minutes == null || minutes < 60 || minutes > 43_200)
            throw new IllegalStateException("Active venue has no valid minimum lead time");
        return minutes;
    }

    public static class MinimumLeadTimeNotMetException extends ConflictException {
        public MinimumLeadTimeNotMetException(String message) { super(message); }
    }
}
