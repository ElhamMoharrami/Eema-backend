package com.eema.backend.service;

import com.eema.backend.dto.response.AvailabilityIntervalDto;
import com.eema.backend.entity.Venue;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class BookingAvailabilityValidator {
    private final VenueAvailabilityService availabilityService;
    private final VenueBookingLeadTimePolicy leadTimePolicy;
    private final Clock clock;

    public void validate(Venue venue, LocalDate date, LocalTime start, LocalTime end) {
        validate(venue, date, start, end, null);
    }

    public void validate(Venue venue, LocalDate date, LocalTime start, LocalTime end, java.util.UUID excludedBookingId) {
        BookingOverlapRule.requireValid(start, end);
        leadTimePolicy.validate(venue, date, start);
        ZoneId zone;
        try { zone = ZoneId.of(venue.getTimezone()); }
        catch (DateTimeException | NullPointerException exception) {
            throw new BadRequestException("Venue timezone is invalid");
        }
        if (date == null || date.isBefore(LocalDate.now(clock.withZone(zone)))) {
            throw new BadRequestException("shootDate must be today or later in the venue timezone");
        }
        boolean contained = availabilityService.getAvailableIntervalsExcludingBooking(venue.getId(), date, excludedBookingId).stream()
                .anyMatch(interval -> contains(interval, start, end));
        if (!contained) throw new ConflictException("Requested time is not available for this venue");
    }

    private boolean contains(AvailabilityIntervalDto interval, LocalTime start, LocalTime end) {
        return !start.isBefore(interval.startTime()) && !end.isAfter(interval.endTime());
    }
}
