package com.eema.backend.service;

import com.eema.backend.exception.BadRequestException;
import java.time.LocalTime;

public final class BookingOverlapRule {
    private BookingOverlapRule() {}

    public static boolean overlaps(LocalTime candidateStart, LocalTime candidateEnd,
                                   LocalTime existingStart, LocalTime existingEnd) {
        requireValid(candidateStart, candidateEnd);
        requireValid(existingStart, existingEnd);
        return candidateStart.isBefore(existingEnd) && candidateEnd.isAfter(existingStart);
    }

    public static void requireValid(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BadRequestException("startTime must be before endTime");
        }
    }
}
