package com.eema.backend.service;

import com.eema.backend.dto.response.AvailabilityIntervalDto;
import com.eema.backend.entity.Venue;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ConflictException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingAvailabilityValidatorTest {
    private final VenueAvailabilityService availability = mock(VenueAvailabilityService.class);
    private final VenueBookingLeadTimePolicy leadTime = mock(VenueBookingLeadTimePolicy.class);
    private final BookingAvailabilityValidator validator = new BookingAvailabilityValidator(availability, leadTime,
            java.time.Clock.systemUTC());
    private final UUID venueId = UUID.randomUUID();
    private final Venue venue = Venue.builder().id(venueId).timezone("Europe/Amsterdam").build();

    @Test void acceptsSlotContainedInEffectiveAvailability() {
        LocalDate date = LocalDate.now(java.time.ZoneId.of("Europe/Amsterdam")).plusDays(1);
        when(availability.getAvailableIntervalsExcludingBooking(venueId, date, null)).thenReturn(List.of(interval(9, 17)));
        assertDoesNotThrow(() -> validator.validate(venue, date, time(10), time(12)));
    }

    @Test void rejectsSlotOutsideEffectiveAvailability() {
        LocalDate date = LocalDate.now(java.time.ZoneId.of("Europe/Amsterdam")).plusDays(1);
        when(availability.getAvailableIntervalsExcludingBooking(venueId, date, null)).thenReturn(List.of(interval(9, 17)));
        assertThrows(ConflictException.class, () -> validator.validate(venue, date, time(8), time(12)));
    }

    @Test void rejectsInvalidVenueTimezone() {
        venue.setTimezone("Amsterdam/+1");
        assertThrows(BadRequestException.class, () -> validator.validate(venue, LocalDate.now(), time(9), time(10)));
    }

    private AvailabilityIntervalDto interval(int start, int end) { return new AvailabilityIntervalDto(time(start), time(end)); }
    private LocalTime time(int hour) { return LocalTime.of(hour, 0); }
}
