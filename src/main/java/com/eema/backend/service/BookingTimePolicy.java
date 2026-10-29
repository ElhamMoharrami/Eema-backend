package com.eema.backend.service;

import com.eema.backend.dto.response.AvailabilityIntervalDto;
import com.eema.backend.exception.BadRequestException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/** Eema v1 local-time authority: gaps are invalid, overlaps use the earlier offset. */
public final class BookingTimePolicy {
    private BookingTimePolicy() {}

    public static OffsetDateTime resolve(LocalDate date, LocalTime time, ZoneId zone) {
        if (date == null || time == null) throw new BadRequestException("Booking date and time are required");
        LocalDateTime local = date.atTime(time);
        var offsets = zone.getRules().getValidOffsets(local);
        if (offsets.isEmpty()) throw new BadRequestException(
                "That local time does not exist because the clocks change on this date. Please choose another time.");
        // ZoneRules orders the overlap offsets with the earlier occurrence first.
        return OffsetDateTime.of(local, offsets.get(0));
    }

    public static ResolvedInterval interval(LocalDate date, LocalTime start, LocalTime end, ZoneId zone) {
        BookingOverlapRule.requireValid(start, end);
        var resolvedStart = resolve(date, start, zone);
        var resolvedEnd = resolve(date, end, zone);
        if (!resolvedEnd.toInstant().isAfter(resolvedStart.toInstant()))
            throw new BadRequestException("The selected interval must end after it starts. Please choose another time.");
        return new ResolvedInterval(resolvedStart, resolvedEnd);
    }

    /** Split wall-clock availability so neither endpoint nor any selectable portion lies in a gap. */
    public static List<AvailabilityIntervalDto> available(LocalDate date, LocalTime start, LocalTime end, ZoneId zone) {
        var parts = new ArrayList<AvailabilityIntervalDto>();
        parts.add(new AvailabilityIntervalDto(start, end));
        Instant cursor = date.atStartOfDay().toInstant(ZoneOffset.UTC).minus(Duration.ofDays(2));
        Instant limit = cursor.plus(Duration.ofDays(5));
        for (var change = zone.getRules().nextTransition(cursor); change != null && change.getInstant().isBefore(limit);
             change = zone.getRules().nextTransition(change.getInstant().plusNanos(1))) {
            if (!change.isGap()) continue;
            LocalDateTime gapStart = change.getDateTimeBefore(), gapEnd = change.getDateTimeAfter();
            var next = new ArrayList<AvailabilityIntervalDto>();
            for (var part : parts) {
                LocalDateTime a = date.atTime(part.startTime()), b = date.atTime(part.endTime());
                if (b.isBefore(gapStart) || !a.isBefore(gapEnd)) { next.add(part); continue; }
                if (a.isBefore(gapStart)) {
                    LocalDateTime safeEnd = gapStart.minusNanos(1);
                    if (safeEnd.isAfter(a)) next.add(new AvailabilityIntervalDto(part.startTime(), safeEnd.toLocalTime()));
                }
                if (b.isAfter(gapEnd)) next.add(new AvailabilityIntervalDto(gapEnd.toLocalTime(), part.endTime()));
            }
            parts = next;
        }
        return List.copyOf(parts);
    }

    public record ResolvedInterval(OffsetDateTime start, OffsetDateTime end) {}
}
