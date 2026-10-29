package com.eema.backend.service;

import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.ActorType;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.CancellationPolicyTier;
import com.eema.backend.entity.enums.ProviderCostResponsibility;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

class BookingCancellationPolicyTest {
    private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");
    private final BookingCancellationPolicy policy = new BookingCancellationPolicy(new BookingFinancials());

    @Test
    void gracePeriodRequiresConfirmationWithin24HoursAndShootMoreThan48HoursAway() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        Booking insideGrace = confirmed(now.minusHours(1), now.plusDays(5));
        Booking outsideGrace = confirmed(now.minusHours(25), now.plusDays(5));

        assertDecision(insideGrace, now, CancellationPolicyTier.GRACE_PERIOD, "100.00");
        assertDecision(outsideGrace, now, CancellationPolicyTier.PARTIAL_REFUND, "50.00");
    }

    @Test
    void exactSevenDaysAndExact48HoursUsePartialTier() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        assertDecision(confirmed(now.minusDays(30), now.plusDays(8)), now,
                CancellationPolicyTier.FLEXIBLE_FULL_REFUND, "100.00");
        assertDecision(confirmed(now.minusDays(30), now.plusDays(7)), now,
                CancellationPolicyTier.PARTIAL_REFUND, "50.00");
        assertDecision(confirmed(now.minusDays(30), now.plusHours(48)), now,
                CancellationPolicyTier.PARTIAL_REFUND, "50.00");
        assertDecision(confirmed(now.minusDays(30), now.plusHours(48).minusSeconds(1)), now,
                CancellationPolicyTier.NON_REFUNDABLE, "0.00");
    }

    @Test
    void cancellationEconomicsUseRetainedAmountAndCommissionSnapshot() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        var full = policy.evaluate(confirmed(now.minusDays(30), now.plusDays(8)), ActorType.SCOUT, now);
        assertThat(full.refundAmountEur()).isEqualByComparingTo("1000.00");
        assertThat(full.retainedAmountEur()).isEqualByComparingTo("0.00");
        assertThat(full.eemaCommissionAfterCancellationEur()).isEqualByComparingTo("0.00");
        assertThat(full.ownerGrossSettlementEur()).isEqualByComparingTo("0.00");

        var partial = policy.evaluate(confirmed(now.minusDays(30), now.plusDays(5)), ActorType.SCOUT, now);
        assertThat(partial.refundAmountEur()).isEqualByComparingTo("500.00");
        assertThat(partial.retainedAmountEur()).isEqualByComparingTo("500.00");
        assertThat(partial.eemaCommissionAfterCancellationEur()).isEqualByComparingTo("100.00");
        assertThat(partial.ownerGrossSettlementEur()).isEqualByComparingTo("400.00");

        var none = policy.evaluate(confirmed(now.minusDays(30), now.plusHours(24)), ActorType.SCOUT, now);
        assertThat(none.refundAmountEur()).isEqualByComparingTo("0.00");
        assertThat(none.eemaCommissionAfterCancellationEur()).isEqualByComparingTo("200.00");
        assertThat(none.ownerGrossSettlementEur()).isEqualByComparingTo("800.00");
    }

    @Test
    void ownerAndAdminAlwaysProduceFullCustomerRefundAndZeroSettlement() {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        Booking booking = confirmed(now.minusDays(30), now.plusHours(1));

        var owner = policy.evaluate(booking, ActorType.OWNER, now);
        assertThat(owner.policyTier()).isEqualTo(CancellationPolicyTier.OWNER_FULL_REFUND);
        assertThat(owner.refundAmountEur()).isEqualByComparingTo("1000.00");
        assertThat(owner.ownerGrossSettlementEur()).isZero();
        assertThat(owner.providerCostResponsibility()).isEqualTo(ProviderCostResponsibility.OWNER);

        var admin = policy.evaluate(booking, ActorType.ADMIN, now);
        assertThat(admin.policyTier()).isEqualTo(CancellationPolicyTier.ADMIN_FULL_REFUND);
        assertThat(admin.refundAmountEur()).isEqualByComparingTo("1000.00");
        assertThat(admin.ownerGrossSettlementEur()).isZero();
        assertThat(admin.providerCostResponsibility()).isEqualTo(ProviderCostResponsibility.EEMA);
    }

    @Test
    void snapshotUsesVenueTimezoneAcrossAmsterdamDstTransition() {
        Booking booking = baseBooking(LocalDate.of(2026, 3, 29), LocalTime.of(10, 0));
        OffsetDateTime confirmed = OffsetDateTime.parse("2026-03-01T12:00:00Z");
        policy.snapshotAtConfirmation(booking, AMSTERDAM, confirmed);
        Instant shootInstant = booking.getShootDate().atTime(booking.getStartTime())
                .atZone(AMSTERDAM).toInstant();

        assertThat(Duration.between(booking.getPartialRefundUntil().toInstant(), shootInstant))
                .isEqualTo(Duration.ofHours(48));
        assertThat(Duration.between(booking.getFullRefundUntil().toInstant(), shootInstant))
                .isEqualTo(Duration.ofDays(7));
    }

    @Test
    void moneyRoundingIsHalfUpAtTwoDecimals() {
        Booking booking = baseBooking(LocalDate.of(2026, 6, 6), LocalTime.of(10, 0));
        booking.setTotalPaidEur(new BigDecimal("100.01"));
        OffsetDateTime now = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        policy.snapshotAtConfirmation(booking, AMSTERDAM, now.minusDays(30));

        var decision = policy.evaluate(booking, ActorType.SCOUT, now);
        assertThat(decision.refundAmountEur()).isEqualByComparingTo("50.01");
        assertThat(decision.retainedAmountEur()).isEqualByComparingTo("50.00");
        assertThat(decision.eemaCommissionAfterCancellationEur()).isEqualByComparingTo("10.00");
    }

    private Booking confirmed(OffsetDateTime confirmedAt, OffsetDateTime shootInstant) {
        ZonedDateTime localShoot = shootInstant.atZoneSameInstant(AMSTERDAM);
        Booking booking = baseBooking(localShoot.toLocalDate(), localShoot.toLocalTime());
        booking.setConfirmedAt(confirmedAt);
        policy.snapshotAtConfirmation(booking, AMSTERDAM, confirmedAt);
        return booking;
    }

    private Booking baseBooking(LocalDate date, LocalTime time) {
        return Booking.builder().status(BookingStatus.CONFIRMED).shootDate(date).startTime(time)
                .totalPaidEur(new BigDecimal("1000.00"))
                .commissionPercentage(new BigDecimal("20.00")).build();
    }

    private void assertDecision(Booking booking, OffsetDateTime now,
                                CancellationPolicyTier tier, String percentage) {
        var decision = policy.evaluate(booking, ActorType.SCOUT, now);
        assertThat(decision.policyTier()).isEqualTo(tier);
        assertThat(decision.refundPercentage()).isEqualByComparingTo(percentage);
    }
}
