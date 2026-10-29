package com.eema.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class BookingPriceCalculatorTest {
    private final BookingPriceCalculator calculator = new BookingPriceCalculator();

    @Test
    void appliesMinimumExactMinutesAndFullDayCap() {
        assertPrice("700.00", "175.00", 240, "1200.00", 9, 11);
        assertPrice("700.00", "175.00", 240, "1200.00", 9, 13);
        assertPrice("875.00", "175.00", 240, "1200.00", 9, 14);
        assertPrice("1200.00", "175.00", 240, "1200.00", 9, 16);
        assertPrice("1225.00", "175.00", 240, null, 9, 16);
        assertPrice("787.50", "175.00", 240, null, LocalTime.of(9, 0), LocalTime.of(13, 30));
    }

    @Test
    void capAlsoAppliesToMinimumDerivedTotalAndRoundsHalfUpToCents() {
        assertPrice("1200.00", "500.00", 240, "1200.00", 9, 11);
        assertPrice("0.17", "10.00", 1, null, LocalTime.of(9, 0), LocalTime.of(9, 1));
    }

    @Test
    void exposesAuthoritativeQuoteDetails() {
        var quote = calculator.calculate(new BigDecimal("175"), 240, new BigDecimal("1200"),
                LocalTime.of(9, 0), LocalTime.of(11, 0));
        assertEquals(120, quote.durationMinutes());
        assertEquals(240, quote.billableMinutes());
        assertEquals(new BigDecimal("700.00"), quote.calculatedHourlyTotal());
        assertEquals(new BigDecimal("700.00"), quote.finalPriceEur());
    }

    @Test
    void rejectsInvalidConfigurationAndIntervals() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(BigDecimal.ZERO, 60, null,
                LocalTime.NOON, LocalTime.of(13, 0)));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(BigDecimal.ONE, 0, null,
                LocalTime.NOON, LocalTime.of(13, 0)));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(BigDecimal.ONE, 60, BigDecimal.ZERO,
                LocalTime.NOON, LocalTime.of(13, 0)));
        assertThrows(RuntimeException.class, () -> calculator.calculate(BigDecimal.ONE, 60, null,
                LocalTime.NOON, LocalTime.NOON));
    }

    private void assertPrice(String expected, String hourly, int minimum, String cap, int start, int end) {
        assertPrice(expected, hourly, minimum, cap, LocalTime.of(start, 0), LocalTime.of(end, 0));
    }

    private void assertPrice(String expected, String hourly, int minimum, String cap,
                             LocalTime start, LocalTime end) {
        var quote = calculator.calculate(new BigDecimal(hourly), minimum,
                cap == null ? null : new BigDecimal(cap), start, end);
        assertEquals(new BigDecimal(expected), quote.finalPriceEur());
    }
}
