package com.eema.backend.service;

import java.math.BigDecimal;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.*;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class BookingFinancials {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /** Validate the final quote or stored snapshot; never clamp or recalculate it. */
    public void requireBookablePrice(BigDecimal price) {
        if (price == null || price.compareTo(new BigDecimal("0.01")) < 0) {
            throw new com.eema.backend.exception.BadRequestException(
                    "Booking total is below Eema's minimum bookable amount of EUR 0.01. Free bookings are not supported.");
        }
    }

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
        return allocateProviderCost(money(venuePrice).subtract(money(commission)), mollieFee,
                ProviderCostResponsibility.OWNER_SETTLEMENT).ownerPayout();
    }

    /** The persisted pre-fee entitlement, never a reconstruction from today's listing price. */
    public BigDecimal ownerEntitlementBeforeProviderCost(Booking booking) {
        if (booking.getStatus() == BookingStatus.REFUNDED || booking.getLatePaymentDetectedAt() != null)
            return money(BigDecimal.ZERO);
        if (booking.getOwnerCancellationSettlementEur() != null)
            return money(booking.getOwnerCancellationSettlementEur().max(BigDecimal.ZERO));
        BigDecimal gross = booking.getCancellationRetainedAmountEur() != null
                ? booking.getCancellationRetainedAmountEur() : booking.getVenuePriceEur();
        return money(money(gross).subtract(money(booking.getCommissionEur())).max(BigDecimal.ZERO));
    }

    /** Eema v1: only OWNER permits recourse beyond unpaid proceeds. NULL is the existing normal-booking allocation. */
    public ProviderCostAllocation allocateProviderCost(BigDecimal entitlement, BigDecimal providerCost,
                                                       ProviderCostResponsibility responsibility) {
        BigDecimal available = money(java.util.Objects.requireNonNull(entitlement).max(BigDecimal.ZERO));
        BigDecimal cost = money(providerCost);
        BigDecimal zero = money(BigDecimal.ZERO);
        boolean ownerProceeds = responsibility == null || responsibility == ProviderCostResponsibility.OWNER_SETTLEMENT
                || responsibility == ProviderCostResponsibility.OWNER;
        BigDecimal applied = ownerProceeds ? cost.min(available) : zero;
        BigDecimal excess = cost.subtract(applied);
        BigDecimal liability = responsibility == ProviderCostResponsibility.OWNER ? excess : zero;
        return new ProviderCostAllocation(available.subtract(applied), applied, excess.subtract(liability), liability);
    }

    public record ProviderCostAllocation(BigDecimal ownerPayout, BigDecimal appliedToSettlement,
                                         BigDecimal eemaProviderCost, BigDecimal ownerLiability) {}
}
