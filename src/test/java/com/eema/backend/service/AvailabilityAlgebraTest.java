package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import com.eema.backend.dto.response.AvailabilityIntervalDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AvailabilityAlgebraTest {
    final UUID id=UUID.randomUUID();
    final Venue venue=Venue.builder().id(id).timezone("Europe/Amsterdam").minimumLeadTimeMinutes(120).build();
    final VenueRepository venues=mock(VenueRepository.class);
    final VenueOperatingHoursRepository weekly=mock(VenueOperatingHoursRepository.class);
    final VenueAvailabilityOverrideRepository overrides=mock(VenueAvailabilityOverrideRepository.class);
    final BookingRepository bookings=mock(BookingRepository.class);
    VenueBookingLeadTimePolicy lead;
    VenueAvailabilityService service;
    void at(String now) {
        lead=new VenueBookingLeadTimePolicy(Clock.fixed(Instant.parse(now),ZoneOffset.UTC));
        service=new VenueAvailabilityService(venues,weekly,overrides,mock(CurrentUserService.class),mock(VenueService.class),bookings,lead);
        when(venues.findById(id)).thenReturn(Optional.of(venue));
    }
    static LocalTime t(String value){return LocalTime.parse(value);}
    static AvailabilityIntervalDto interval(String a,String b){return new AvailabilityIntervalDto(t(a),t(b));}
    void base(LocalDate date,String a,String b) {when(weekly.findByVenueIdAndDayOfWeekOrderByStartTime(id,date.getDayOfWeek())).thenReturn(List.of(VenueOperatingHours.builder().startTime(t(a)).endTime(t(b)).build()));}
    static Stream<Arguments> roundingCases(){return Stream.of(
        Arguments.of("2026-09-10T08:00:00Z","2026-09-10","12:00","17:00"),
        Arguments.of("2026-09-10T08:00:30Z","2026-09-10","12:01","17:00"),
        Arguments.of("2026-09-10T19:59:30Z","2026-09-10",null,null),
        Arguments.of("2026-10-24T23:30:00Z","2026-10-25","03:00","17:00"),
        Arguments.of("2026-10-24T22:30:00Z","2026-10-25","02:30","17:00"),
        Arguments.of("2026-03-29T00:30:00Z","2026-03-29","04:30","17:00"),
        Arguments.of("2026-09-10T19:59:30Z","2026-09-11","00:00","17:00"));}
    @ParameterizedTest @MethodSource("roundingCases")
    void displayedLeadBoundaryNeverExposesAnInvalidStart(String now,String day,String start,String end){
        at(now);LocalDate date=LocalDate.parse(day);base(date,"00:00","17:00");
        var actual=service.getAvailableIntervals(id,date);
        assertEquals(start==null?List.of():List.of(interval(start,end)),actual);
        for(var span:actual)for(LocalTime minute=span.startTime();minute.isBefore(span.endTime());minute=minute.plusMinutes(1))
            assertTrue(lead.isSatisfied(venue,date,minute),"Exposed invalid start: "+minute);
    }

    static Stream<Arguments> shapes(){
        return Arrays.stream(BookingStatus.values()).flatMap(status->Stream.of(
            Arguments.of(status,"11:00","13:00",List.of(interval("09:00","11:00"),interval("13:00","17:00"))),
            Arguments.of(status,"09:00","17:00",List.of()),
            Arguments.of(status,"08:00","18:00",List.of()),
            Arguments.of(status,"08:00","10:00",List.of(interval("10:00","17:00"))),
            Arguments.of(status,"16:00","18:00",List.of(interval("09:00","16:00"))),
            Arguments.of(status,"07:00","09:00",List.of(interval("09:00","17:00"))),
            Arguments.of(status,"17:00","19:00",List.of(interval("09:00","17:00")))));
    }
    @ParameterizedTest @MethodSource("shapes")
    void statusAndHalfOpenSubtraction(BookingStatus status,String start,String end,List<AvailabilityIntervalDto> blocked){
        at("2026-01-01T00:00:00Z");LocalDate date=LocalDate.of(2026,9,14);base(date,"09:00","17:00");
        when(bookings.findBlockingBookings(id,date)).thenReturn(List.of(Booking.builder().status(status).startTime(t(start)).endTime(t(end)).build()));
        boolean reserves=Set.of(BookingStatus.AWAITING_PAYMENT,BookingStatus.CONFIRMED).contains(status);
        assertEquals(reserves,BookingReservationPolicy.blocksAvailability(status));
        assertEquals(reserves?blocked:List.of(interval("09:00","17:00")),service.getAvailableIntervals(id,date));
    }
    VenueAvailabilityOverride override(AvailabilityOverrideType type,String a,String b){return VenueAvailabilityOverride.builder().type(type).startTime(t(a)).endTime(t(b)).build();}
    @Test void combinedAlgebraAndMultipleBookingsNormalizeWithoutMutation(){
        at("2026-09-14T04:30:00Z");LocalDate date=LocalDate.of(2026,9,14);base(date,"09:00","17:00");
        var rows=List.of(override(AvailabilityOverrideType.AVAILABLE,"08:00","10:00"),override(AvailabilityOverrideType.AVAILABLE,"16:00","18:00"),override(AvailabilityOverrideType.BLOCKED,"11:00","12:00"),override(AvailabilityOverrideType.BLOCKED,"16:00","19:00"));
        when(overrides.findByVenueIdAndDateOrderByStartTime(id,date)).thenReturn(rows);
        when(bookings.findBlockingBookings(id,date)).thenReturn(List.of(
            Booking.builder().status(BookingStatus.CONFIRMED).startTime(t("10:00")).endTime(t("10:30")).build(),
            Booking.builder().status(BookingStatus.AWAITING_PAYMENT).startTime(t("13:00")).endTime(t("14:00")).build(),
            Booking.builder().status(BookingStatus.REQUESTED).startTime(t("14:00")).endTime(t("15:00")).build()));
        assertEquals(List.of(interval("08:30","10:00"),interval("10:30","11:00"),interval("12:00","13:00"),interval("14:00","16:00")),service.getAvailableIntervals(id,date));
        verify(weekly,never()).save(any());verify(overrides,never()).save(any());verify(bookings,never()).save(any());
    }
    @ParameterizedTest @CsvSource({"2026-03-28T09:00:00Z,2026-03-29,11:00,10:59","2026-10-24T08:00:00Z,2026-10-25,09:00,08:59"})
    void elapsedDayAcrossBothDstTransitions(String now,String day,String boundary,String tooSoon){
        at(now);venue.setMinimumLeadTimeMinutes(1440);LocalDate date=LocalDate.parse(day);
        assertEquals(Duration.ofHours(24),Duration.between(Instant.parse(now),lead.bookingStart(venue,date,t(boundary))));
        assertTrue(lead.isSatisfied(venue,date,t(boundary)));assertFalse(lead.isSatisfied(venue,date,t(tooSoon)));
        base(date,"00:00","17:00");assertEquals(List.of(interval(boundary,"17:00")),service.getAvailableIntervals(id,date));
    }
    @Test void exactMinuteBoundaryAndJavaGapOverlapResolutionAreExplicit(){
        at("2026-09-14T08:00:00Z");LocalDate d=LocalDate.of(2026,9,14);
        assertTrue(lead.isSatisfied(venue,d,t("12:00")));assertFalse(lead.isSatisfied(venue,d,t("11:59")));
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"),lead.bookingStart(venue,LocalDate.of(2026,3,29),t("02:30")));
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"),lead.bookingStart(venue,LocalDate.of(2026,10,25),t("02:30")));
    }
}
