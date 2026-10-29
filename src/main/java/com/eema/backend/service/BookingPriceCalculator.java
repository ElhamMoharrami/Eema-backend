package com.eema.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/** Authoritative exact-minute venue booking price calculation. */
@Component
public class BookingPriceCalculator {
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);

    public PricingQuote calculate(BigDecimal hourlyRateEur, Integer minimumBookingMinutes,
                                  BigDecimal fullDayRateEur, LocalTime startTime, LocalTime endTime) {
        if (hourlyRateEur == null || hourlyRateEur.signum() <= 0) {
            throw new IllegalArgumentException("hourlyRateEur must be greater than zero");
        }
        if (minimumBookingMinutes == null || minimumBookingMinutes <= 0) {
            throw new IllegalArgumentException("minimumBookingMinutes must be greater than zero");
        }
        if (fullDayRateEur != null && fullDayRateEur.signum() <= 0) {
            throw new IllegalArgumentException("fullDayRateEur must be greater than zero when provided");
        }
        BookingOverlapRule.requireValid(startTime, endTime);
        long duration = Duration.between(startTime, endTime).toMinutes();
        if (duration < 1) throw new IllegalArgumentException("Booking duration must be at least one minute");
        if (duration > Integer.MAX_VALUE) throw new IllegalArgumentException("Booking duration is too large");
        int durationMinutes = (int) duration;
        int billableMinutes = Math.max(durationMinutes, minimumBookingMinutes);
        BigDecimal hourlyTotal = hourlyRateEur.multiply(BigDecimal.valueOf(billableMinutes))
                .divide(SIXTY, 2, RoundingMode.HALF_UP);
        BigDecimal normalizedHourlyRate = hourlyRateEur.setScale(2, RoundingMode.HALF_UP);
        BigDecimal normalizedFullDay = fullDayRateEur == null ? null
                : fullDayRateEur.setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalPrice = normalizedFullDay == null || hourlyTotal.compareTo(normalizedFullDay) <= 0
                ? hourlyTotal : normalizedFullDay;
        // Booking monetary columns are NUMERIC(10,2); reject overflow before persistence.
        if (finalPrice.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new IllegalArgumentException("Calculated booking price exceeds the supported amount");
        return new PricingQuote(durationMinutes, billableMinutes, normalizedHourlyRate,
                minimumBookingMinutes, normalizedFullDay, hourlyTotal, finalPrice);
    }

    public record PricingQuote(int durationMinutes, int billableMinutes, BigDecimal hourlyRateEur,
            int minimumBookingMinutes, BigDecimal fullDayRateEur,
            BigDecimal calculatedHourlyTotal, BigDecimal finalPriceEur) {}
}
