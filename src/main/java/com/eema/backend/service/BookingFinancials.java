package com.eema.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class BookingFinancials {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public BigDecimal money(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money amount must be non-negative");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal commission(BigDecimal venuePrice, BigDecimal percentage) {
        BigDecimal price = money(venuePrice);
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("Commission percentage must be between 0 and 100");
        }
        return price.multiply(percentage).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal ownerPayout(BigDecimal venuePrice, BigDecimal commission, BigDecimal mollieFee) {
        BigDecimal payout = money(venuePrice).subtract(money(commission)).subtract(money(mollieFee));
        if (payout.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Owner payout cannot be negative");
        }
        return money(payout);
    }
}
