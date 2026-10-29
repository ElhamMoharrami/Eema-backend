package com.eema.backend.service;

import com.eema.backend.dto.request.*;
import com.eema.backend.dto.response.AvailabilityIntervalDto;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.AvailabilityOverrideType;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VenueAvailabilityServiceTest {
    private final VenueRepository venues = mock(VenueRepository.class);
    private final VenueOperatingHoursRepository weekly = mock(VenueOperatingHoursRepository.class);
    private final VenueAvailabilityOverrideRepository overrides = mock(VenueAvailabilityOverrideRepository.class);
    private final CurrentUserService currentUsers = mock(CurrentUserService.class);
    private final VenueService venueService = mock(VenueService.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final UUID venueId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private Venue venue;
    private VenueAvailabilityService service;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(ownerId).role(UserRole.OWNER).build();
        venue = Venue.builder().id(venueId).owner(owner).timezone("Europe/Amsterdam").build();
        service = new VenueAvailabilityService(venues, weekly, overrides, currentUsers, venueService, bookings);
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));
        when(venues.findByIdForUpdate(venueId)).thenReturn(Optional.of(venue));
        when(weekly.findByVenueIdOrderByDayOfWeekAscStartTimeAsc(venueId)).thenReturn(List.of());
        when(overrides.findByVenueIdOrderByDateAscStartTimeAsc(venueId)).thenReturn(List.of());
    }

    @Test
    void ownerMayManageOwnVenueAndTimezoneIsValidated() {
        when(currentUsers.getCurrentUser()).thenReturn(User.builder().id(ownerId).role(UserRole.OWNER).build());
        assertEquals("Europe/Paris", service.updateSettings(venueId,
                new UpdateVenueAvailabilitySettingsRequest("Europe/Paris")).timezone());
        assertThrows(BadRequestException.class, () -> service.updateSettings(venueId,
                new UpdateVenueAvailabilitySettingsRequest("UTC+Amsterdam")));
    }

    @Test
    void ownerCannotManageAnotherOwnersVenue() {
        when(currentUsers.getCurrentUser()).thenReturn(User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).build());
        assertThrows(ForbiddenException.class, () -> service.updateSettings(venueId,
                new UpdateVenueAvailabilitySettingsRequest("Europe/Amsterdam")));
    }

    @Test
    void scoutCannotMutateAvailability() {
        when(currentUsers.getCurrentUser()).thenReturn(User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build());
        assertThrows(ForbiddenException.class, () -> service.replaceWeekly(venueId,
                new UpdateWeeklyAvailabilityRequest(List.of())));
    }

    @Test
    void rejectsInvalidAndOverlappingWeeklyIntervals() {
        when(currentUsers.getCurrentUser()).thenReturn(User.builder().id(ownerId).role(UserRole.OWNER).build());
        assertThrows(BadRequestException.class, () -> service.replaceWeekly(venueId,
                weekly(new WeeklyIntervalRequest(DayOfWeek.MONDAY, time(17), time(9)))));
        assertThrows(BadRequestException.class, () -> service.replaceWeekly(venueId,
                weekly(new WeeklyIntervalRequest(DayOfWeek.MONDAY, time(8), time(12)),
                        new WeeklyIntervalRequest(DayOfWeek.MONDAY, time(11), time(15)))));
    }

    @Test
    void calculatesWeeklyAndEmptyDay() {
        LocalDate monday = LocalDate.of(2026, 8, 24);
        when(weekly.findByVenueIdAndDayOfWeekOrderByStartTime(venueId, DayOfWeek.MONDAY))
                .thenReturn(List.of(hours(DayOfWeek.MONDAY, 9, 17)));
        when(overrides.findByVenueIdAndDateOrderByStartTime(venueId, monday)).thenReturn(List.of());
        assertEquals(List.of(interval(9, 17)), service.getAvailableIntervals(venueId, monday));

        LocalDate tuesday = monday.plusDays(1);
        when(weekly.findByVenueIdAndDayOfWeekOrderByStartTime(venueId, DayOfWeek.TUESDAY)).thenReturn(List.of());
        when(overrides.findByVenueIdAndDateOrderByStartTime(venueId, tuesday)).thenReturn(List.of());
        assertTrue(service.getAvailableIntervals(venueId, tuesday).isEmpty());
    }

    @Test
    void blockedOverrideSubtractsPartialRange() {
        LocalDate date = LocalDate.of(2026, 8, 25);
        when(weekly.findByVenueIdAndDayOfWeekOrderByStartTime(venueId, date.getDayOfWeek()))
                .thenReturn(List.of(hours(date.getDayOfWeek(), 9, 17)));
        when(overrides.findByVenueIdAndDateOrderByStartTime(venueId, date))
                .thenReturn(List.of(override(date, AvailabilityOverrideType.BLOCKED, 12, 14)));
        assertEquals(List.of(interval(9, 12), interval(14, 17)), service.getAvailableIntervals(venueId, date));
    }

    @Test
    void availableOverrideAddsAndNormalizesOutsideWeeklyHours() {
        LocalDate date = LocalDate.of(2026, 8, 27);
        when(weekly.findByVenueIdAndDayOfWeekOrderByStartTime(venueId, date.getDayOfWeek()))
                .thenReturn(List.of(hours(date.getDayOfWeek(), 9, 17)));
        when(overrides.findByVenueIdAndDateOrderByStartTime(venueId, date))
                .thenReturn(List.of(override(date, AvailabilityOverrideType.AVAILABLE, 7, 23)));
        assertEquals(List.of(interval(7, 23)), service.getAvailableIntervals(venueId, date));
    }

    @Test
    void blocksPartOfSpecialAvailabilityWhenNoWeeklyHoursExist() {
        LocalDate date = LocalDate.of(2026, 8, 30);
        when(weekly.findByVenueIdAndDayOfWeekOrderByStartTime(venueId, date.getDayOfWeek())).thenReturn(List.of());
        when(overrides.findByVenueIdAndDateOrderByStartTime(venueId, date)).thenReturn(List.of(
                override(date, AvailabilityOverrideType.AVAILABLE, 8, 20),
                override(date, AvailabilityOverrideType.BLOCKED, 10, 11)));
        assertEquals(List.of(interval(8, 10), interval(11, 20)), service.getAvailableIntervals(venueId, date));
    }

    @Test
    void confirmedBookingSplitsAvailabilityAndCancelledBookingDoesNotBlock() {
        LocalDate date = LocalDate.of(2026, 9, 7);
        when(weekly.findByVenueIdAndDayOfWeekOrderByStartTime(venueId, date.getDayOfWeek()))
                .thenReturn(List.of(hours(date.getDayOfWeek(), 8, 20)));
        when(overrides.findByVenueIdAndDateOrderByStartTime(venueId, date)).thenReturn(List.of());
        Booking confirmed = Booking.builder().status(com.eema.backend.entity.enums.BookingStatus.CONFIRMED)
                .startTime(time(12)).endTime(time(15)).build();
        Booking cancelled = Booking.builder().status(com.eema.backend.entity.enums.BookingStatus.CANCELLED)
                .startTime(time(9)).endTime(time(10)).build();
        when(bookings.findBlockingBookings(venueId, date)).thenReturn(List.of(cancelled, confirmed));
        assertEquals(List.of(interval(8, 12), interval(15, 20)), service.getAvailableIntervals(venueId, date));
    }

    @Test
    void rejectsExcessiveDateRange() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        assertThrows(BadRequestException.class, () -> service.getAvailability(venueId, from, from.plusDays(93)));
        verifyNoInteractions(venueService);
    }

    private UpdateWeeklyAvailabilityRequest weekly(WeeklyIntervalRequest... values) { return new UpdateWeeklyAvailabilityRequest(List.of(values)); }
    private VenueOperatingHours hours(DayOfWeek day, int start, int end) {
        return VenueOperatingHours.builder().venue(venue).dayOfWeek(day).startTime(time(start)).endTime(time(end)).build();
    }
    private VenueAvailabilityOverride override(LocalDate date, AvailabilityOverrideType type, int start, int end) {
        return VenueAvailabilityOverride.builder().venue(venue).date(date).type(type).startTime(time(start)).endTime(time(end)).build();
    }
    private AvailabilityIntervalDto interval(int start, int end) { return new AvailabilityIntervalDto(time(start), time(end)); }
    private LocalTime time(int hour) { return LocalTime.of(hour, 0); }
}
