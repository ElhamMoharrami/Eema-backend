package com.eema.backend.service;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class BookingOverlapRuleTest {
    @Test void detectsOverlappingStart() { assertTrue(overlaps(8, 10, 9, 12)); }
    @Test void detectsOverlappingEnd() { assertTrue(overlaps(11, 13, 9, 12)); }
    @Test void detectsCandidateContainingBooking() { assertTrue(overlaps(8, 15, 9, 12)); }
    @Test void detectsCandidateInsideBooking() { assertTrue(overlaps(10, 11, 9, 12)); }
    @Test void permitsBackToBackBefore() { assertFalse(overlaps(9, 12, 12, 15)); }
    @Test void permitsBackToBackAfter() { assertFalse(overlaps(12, 15, 9, 12)); }
    @Test void permitsSeparatedBooking() { assertFalse(overlaps(15, 16, 9, 12)); }

    private boolean overlaps(int candidateStart, int candidateEnd, int existingStart, int existingEnd) {
        return BookingOverlapRule.overlaps(time(candidateStart), time(candidateEnd), time(existingStart), time(existingEnd));
    }
    private LocalTime time(int hour) { return LocalTime.of(hour, 0); }
}
