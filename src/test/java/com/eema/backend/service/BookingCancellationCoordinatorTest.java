package com.eema.backend.service;

import com.eema.backend.entity.Booking;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.repository.BookingEventRepository;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BookingCancellationCoordinatorTest {
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final VenueRepository venues = mock(VenueRepository.class);
    private final BookingEventRepository events = mock(BookingEventRepository.class);
    private final RefundCoordinator refunds = mock(RefundCoordinator.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final OwnerPayoutService payouts = mock(OwnerPayoutService.class);
    private final BookingFinancials financials = new BookingFinancials();
    private final BookingCancellationPolicy policy = new BookingCancellationPolicy(financials);
    private final BookingCancellationCoordinator coordinator = new BookingCancellationCoordinator(
            bookings, venues, events, policy, financials, new BookingStateMachine(), refunds, publisher, payouts);
    private final UUID bookingId = UUID.randomUUID();
    private final UUID scoutId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = Booking.builder().id(bookingId).venueId(venueId).scoutId(scoutId)
                .status(BookingStatus.REQUESTED).totalPaidEur(new BigDecimal("0.00"))
                .commissionPercentage(new BigDecimal("20.00")).mollieFeeEur(BigDecimal.ZERO)
                .build();
        when(bookings.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(venues.findById(venueId)).thenReturn(Optional.of(Venue.builder().id(venueId)
                .owner(User.builder().id(ownerId).role(UserRole.OWNER).build()).build()));
    }

    @Test
    void scoutWithdrawsRequestedAndAwaitingPaymentWithoutRefund() {
        User scout = user(scoutId, UserRole.SCOUT);
        assertThat(coordinator.cancel(bookingId, scout, null, now).refundClaim()).isNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancellationPolicyTier())
                .isEqualTo(CancellationPolicyTier.PRE_PAYMENT_WITHDRAWAL);
        verifyNoInteractions(refunds);

        booking.setStatus(BookingStatus.AWAITING_PAYMENT);
        assertThat(coordinator.cancel(bookingId, scout, null, now).refundClaim()).isNull();
    }

    @Test
    void ownerCanCancelOwnConfirmedBookingButNotAnotherVenue() {
        prepareConfirmed();
        RefundCoordinator.Claim claim = new RefundCoordinator.Claim(
                bookingId, "tr_1", new BigDecimal("1000.00"), UUID.randomUUID());
        when(refunds.claimCancellation(eq(bookingId), eq(new BigDecimal("1000.00")),
                eq(ownerId), anyString(), eq(ActorType.OWNER))).thenReturn(claim);
        assertThat(coordinator.cancel(bookingId, user(ownerId, UserRole.OWNER), "Venue unavailable", now)
                .refundClaim()).isSameAs(claim);
        assertThat(booking.getOwnerPayoutEur()).isZero();
        assertThat(booking.getProviderCostResponsibility()).isEqualTo(ProviderCostResponsibility.OWNER);

        booking.setStatus(BookingStatus.CONFIRMED);
        assertThrows(ForbiddenException.class, () -> coordinator.cancel(
                bookingId, user(UUID.randomUUID(), UserRole.OWNER), "Reason", now));
    }

    @Test
    void adminRequiresReasonAndReceivesFullRefundPolicy() {
        prepareConfirmed();
        User admin = user(UUID.randomUUID(), UserRole.ADMIN);
        assertThrows(ConflictException.class, () -> coordinator.cancel(bookingId, admin, " ", now));
        when(refunds.claimCancellation(eq(bookingId), any(), eq(admin.getId()), anyString(), eq(ActorType.ADMIN)))
                .thenReturn(new RefundCoordinator.Claim(bookingId, "tr_1", new BigDecimal("1000.00"), UUID.randomUUID()));
        coordinator.cancel(bookingId, admin, "Safety issue", now);
        assertThat(booking.getCancellationPolicyTier()).isEqualTo(CancellationPolicyTier.ADMIN_FULL_REFUND);
        assertThat(booking.getProviderCostResponsibility()).isEqualTo(ProviderCostResponsibility.EEMA);
    }

    private void prepareConfirmed() {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setTotalPaidEur(new BigDecimal("1000.00"));
        booking.setMolliePaymentId("tr_1");
        booking.setShootDate(now.plusDays(10).atZoneSameInstant(ZoneId.of("Europe/Amsterdam")).toLocalDate());
        booking.setStartTime(LocalTime.of(12, 0));
        policy.snapshotAtConfirmation(booking, ZoneId.of("Europe/Amsterdam"), now.minusDays(30));
    }

    private User user(UUID id, UserRole role) {
        return User.builder().id(id).email(role.name().toLowerCase() + "@example.com").role(role).build();
    }
}
