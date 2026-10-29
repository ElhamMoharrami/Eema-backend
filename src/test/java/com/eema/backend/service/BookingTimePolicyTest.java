package com.eema.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class BookingTimePolicyTest {
    final ZoneId amsterdam=ZoneId.of("Europe/Amsterdam");
    @ParameterizedTest @CsvSource({"2027-03-28,02:00", "2027-03-28,02:30", "2027-03-28,02:59:59.999999999"})
    void gapIsInvalid(String date,String time){assertThrows(com.eema.backend.exception.BadRequestException.class,()->BookingTimePolicy.resolve(LocalDate.parse(date),LocalTime.parse(time),amsterdam));}
    @Test void normalAndEarlierOverlap(){assertEquals(Instant.parse("2026-10-25T00:30:00Z"),BookingTimePolicy.resolve(LocalDate.of(2026,10,25),LocalTime.of(2,30),amsterdam).toInstant());assertEquals(Instant.parse("2027-03-28T01:00:00Z"),BookingTimePolicy.resolve(LocalDate.of(2027,3,28),LocalTime.of(3,0),amsterdam).toInstant());}
    @Test void splitAvailabilityHasOnlyValidEndpointsAndMinutes(){var date=LocalDate.of(2027,3,28);var parts=BookingTimePolicy.available(date,LocalTime.MIN,LocalTime.of(6,0),amsterdam);assertEquals(2,parts.size());assertEquals(LocalTime.of(1,59,59,999999999),parts.get(0).endTime());assertEquals(LocalTime.of(3,0),parts.get(1).startTime());for(var part:parts){BookingTimePolicy.interval(date,part.startTime(),part.endTime(),amsterdam);for(var t=part.startTime();t.isBefore(part.endTime());t=t.plusMinutes(1))assertFalse(amsterdam.getRules().getValidOffsets(date.atTime(t)).isEmpty());}}
    @Test void gapOnlyAndSkippedDayHaveNoAvailability(){assertTrue(BookingTimePolicy.available(LocalDate.of(2027,3,28),LocalTime.of(2,0),LocalTime.of(2,30),amsterdam).isEmpty());assertTrue(BookingTimePolicy.available(LocalDate.of(2011,12,30),LocalTime.MIN,LocalTime.MAX,ZoneId.of("Pacific/Apia")).isEmpty());}
    @Test void halfHourGapAndOverlap(){var zone=ZoneId.of("Australia/Lord_Howe");assertThrows(com.eema.backend.exception.BadRequestException.class,()->BookingTimePolicy.resolve(LocalDate.of(2026,10,4),LocalTime.of(2,15),zone));assertEquals(ZoneOffset.ofHours(11),BookingTimePolicy.resolve(LocalDate.of(2026,4,5),LocalTime.of(1,45),zone).getOffset());}
    @Test void orderedPositiveIntervalAndWallPricing(){assertThrows(com.eema.backend.exception.BadRequestException.class,()->BookingTimePolicy.interval(LocalDate.of(2027,1,1),LocalTime.NOON,LocalTime.NOON,amsterdam));var interval=BookingTimePolicy.interval(LocalDate.of(2026,10,25),LocalTime.of(2,30),LocalTime.of(3,30),amsterdam);assertEquals(120,Duration.between(interval.start(),interval.end()).toMinutes());var quote=new BookingPriceCalculator().calculate(new java.math.BigDecimal("60"),1,null,LocalTime.of(2,30),LocalTime.of(3,30));assertEquals(60,quote.durationMinutes());assertEquals(new java.math.BigDecimal("60.00"),quote.finalPriceEur());}
}
