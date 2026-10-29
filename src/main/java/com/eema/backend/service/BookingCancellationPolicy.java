package com.eema.backend.service;

import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.ActorType;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.CancellationPolicyTier;
import com.eema.backend.entity.enums.ProviderCostResponsibility;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class BookingCancellationPolicy {
    public static final String VERSION = "EEMA_V1";
    public static final BigDecimal PARTIAL_PERCENTAGE = new BigDecimal("50.00");
    private static final BigDecimal FULL = new BigDecimal("100.00");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final Duration GRACE = Duration.ofHours(24);
    private static final Duration FULL_WINDOW = Duration.ofDays(7);
    private static final Duration PARTIAL_WINDOW = Duration.ofHours(48);
    private final BookingFinancials financials;

    public BookingCancellationPolicy(BookingFinancials financials) {
        this.financials = financials;
    }

    public void snapshotAtConfirmation(Booking booking, ZoneId venueZone, OffsetDateTime confirmedAt) {
        OffsetDateTime shootStart = booking.getShootDate().atTime(booking.getStartTime())
                .atZone(venueZone).toOffsetDateTime();
        booking.setCancellationPolicyVersion(VERSION);
        booking.setGracePeriodEndsAt(confirmedAt.plus(GRACE));
        booking.setFullRefundUntil(shootStart.minus(FULL_WINDOW));
        booking.setPartialRefundUntil(shootStart.minus(PARTIAL_WINDOW));
        booking.setPartialRefundPercentage(PARTIAL_PERCENTAGE);
    }

    public CancellationDecision evaluate(Booking booking, ActorType actor, OffsetDateTime cancelledAt) {
        if (booking.getStatus() == BookingStatus.REQUESTED
                || booking.getStatus() == BookingStatus.AWAITING_PAYMENT) {
            return decision(CancellationPolicyTier.PRE_PAYMENT_WITHDRAWAL, ZERO,
                    booking, ProviderCostResponsibility.NONE);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Booking is not cancellable");
        }
        if (!VERSION.equals(booking.getCancellationPolicyVersion())
                || booking.getGracePeriodEndsAt() == null
                || booking.getFullRefundUntil() == null
                || booking.getPartialRefundUntil() == null) {
            throw new IllegalStateException("Confirmed booking has no cancellation policy snapshot");
        }
        if (actor == ActorType.OWNER) {
            return decision(CancellationPolicyTier.OWNER_FULL_REFUND, FULL,
                    booking, ProviderCostResponsibility.OWNER);
        }
        if (actor == ActorType.ADMIN) {
            return decision(CancellationPolicyTier.ADMIN_FULL_REFUND, FULL,
                    booking, ProviderCostResponsibility.EEMA);
        }
        if (actor != ActorType.SCOUT) {
            throw new IllegalArgumentException("Unsupported cancellation actor");
        }

        boolean moreThan48Hours = cancelledAt.isBefore(booking.getPartialRefundUntil());
        if (!cancelledAt.isAfter(booking.getGracePeriodEndsAt()) && moreThan48Hours) {
            return decision(CancellationPolicyTier.GRACE_PERIOD, FULL,
                    booking, ProviderCostResponsibility.OWNER_SETTLEMENT);
        }
        if (cancelledAt.isBefore(booking.getFullRefundUntil())) {
            return decision(CancellationPolicyTier.FLEXIBLE_FULL_REFUND, FULL,
                    booking, ProviderCostResponsibility.OWNER_SETTLEMENT);
        }
        if (!cancelledAt.isAfter(booking.getPartialRefundUntil())) {
            return decision(CancellationPolicyTier.PARTIAL_REFUND, booking.getPartialRefundPercentage(),
                    booking, ProviderCostResponsibility.OWNER_SETTLEMENT);
        }
        return decision(CancellationPolicyTier.NON_REFUNDABLE, ZERO,
                booking, ProviderCostResponsibility.OWNER_SETTLEMENT);
    }

    private CancellationDecision decision(CancellationPolicyTier tier, BigDecimal percentage,
                                          Booking booking, ProviderCostResponsibility responsibility) {
        BigDecimal paid = financials.money(booking.getTotalPaidEur());
        BigDecimal refund = paid.multiply(percentage)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal retained = financials.money(paid.subtract(refund));
        BigDecimal commission = financials.commission(retained, booking.getCommissionPercentage());
        BigDecimal ownerGross = financials.money(retained.subtract(commission));
        if (tier == CancellationPolicyTier.OWNER_FULL_REFUND
                || tier == CancellationPolicyTier.ADMIN_FULL_REFUND) {
            ownerGross = financials.money(BigDecimal.ZERO);
        }
        return new CancellationDecision(tier, percentage.setScale(2), refund, retained,
                commission, ownerGross, responsibility);
    }

    public record CancellationDecision(
            CancellationPolicyTier policyTier,
            BigDecimal refundPercentage,
            BigDecimal refundAmountEur,
            BigDecimal retainedAmountEur,
            BigDecimal eemaCommissionAfterCancellationEur,
            BigDecimal ownerGrossSettlementEur,
            ProviderCostResponsibility providerCostResponsibility
    ) {
        public boolean refundable() {
            return refundAmountEur.signum() > 0;
        }
    }
}
