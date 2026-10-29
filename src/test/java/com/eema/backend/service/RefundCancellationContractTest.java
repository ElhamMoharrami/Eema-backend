package com.eema.backend.service;

import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Domain validation against constructed provider facts; does not prove live Mollie behavior. */
class RefundCancellationContractTest {
    final BookingRepository bookings = mock(BookingRepository.class);
    final BookingEventRepository events = mock(BookingEventRepository.class);
    final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    final OwnerPayoutService payouts = mock(OwnerPayoutService.class);
    final Instant now = Instant.parse("2026-06-01T12:00:00Z");
    Booking booking;
    RefundCoordinator coordinator;
    RefundCoordinator.Claim claim;

    @BeforeEach void setup() {
        booking = Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CANCELLED)
                .molliePaymentId("tr_original").totalPaidEur(new BigDecimal("1000.00"))
                .cancellationPolicyTier(CancellationPolicyTier.PARTIAL_REFUND)
                .cancellationRefundAmountEur(new BigDecimal("500.00"))
                .cancellationRetainedAmountEur(new BigDecimal("500.00"))
                .cancelledAt(OffsetDateTime.ofInstant(now, ZoneOffset.UTC)).build();
        when(bookings.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));
        when(bookings.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookings.findIdByMolliePaymentId("tr_original")).thenReturn(Optional.of(booking.getId()));
        coordinator = coordinator(now);
        claim = coordinator.claimCancellation(booking.getId(), new BigDecimal("500.00"), UUID.randomUUID(), "Scout", ActorType.SCOUT);
    }
    RefundCoordinator coordinator(Instant instant) {
        return new RefundCoordinator(bookings, events, new BookingFinancials(), new BookingStateMachine(), publisher,
                payouts, Clock.fixed(instant, ZoneOffset.UTC));
    }
    MollieRefundResponse response(String status, String amount, String currency) {
        return new MollieRefundResponse("re_original", status, "tr_original", new MolliePaymentResponse.Amount(amount, currency));
    }
    @Test void recreatedServiceResumesOriginalIdentityAndAmount() {
        var resumed = coordinator(now.plusSeconds(30)).resumableCancellationClaim(booking.getId()).orElseThrow();
        assertEquals(claim, resumed);
        assertThrows(ConflictException.class, () -> coordinator.claimCancellation(booking.getId(), new BigDecimal("1000.00"), UUID.randomUUID(), "Scout", ActorType.SCOUT));
        assertEquals(claim, coordinator.resumableCancellationClaim(booking.getId()).orElseThrow());
    }
    @ParameterizedTest @ValueSource(ints={3599,3600,3601}) void boundedReplayAtProviderCacheExpiry(int seconds) {
        if (seconds < 3600) assertEquals(claim, coordinator(now.plusSeconds(seconds)).resumableCancellationClaim(booking.getId()).orElseThrow());
        else assertThrows(ConflictException.class, () -> coordinator(now.plusSeconds(seconds)).resumableCancellationClaim(booking.getId()));
        assertEquals(claim.idempotencyKey(), booking.getRefundIdempotencyKey());
    }
    @ParameterizedTest @CsvSource(value={"5.00,EUR","500.001,EUR","500,EUR","500.000,EUR","-500.00,EUR","bad,EUR","500.00,USD","500.00,NULL","NULL,EUR"}, nullValues="NULL")
    void malformedOrUnrelatedAmountNeverCompletes(String amount, String currency) {
        assertThrows(IllegalStateException.class, () -> coordinator.recordProviderState(claim, response("refunded", amount, currency)));
        assertEquals(RefundStatus.CREATING, booking.getRefundStatus());
        assertNull(booking.getMollieRefundId()); assertNull(booking.getRefundedAt());
    }
    @Test void wrongPaymentAndClaimIdentityCannotAttach() {
        assertThrows(IllegalStateException.class, () -> coordinator.recordProviderState(claim,
                new MollieRefundResponse("re_wrong", "refunded", "tr_other", new MolliePaymentResponse.Amount("500.00", "EUR"))));
        var stale = new RefundCoordinator.Claim(booking.getId(), claim.paymentId(), claim.amount(), UUID.randomUUID());
        assertThrows(IllegalStateException.class, () -> coordinator.recordProviderState(stale, response("refunded", "500.00", "EUR")));
        assertNull(booking.getMollieRefundId());
    }
    @ParameterizedTest @NullSource @ValueSource(strings={"none","creating","unknown",""})
    void invalidStatusCannotAttach(String status) {
        assertThrows(IllegalStateException.class, () -> coordinator.recordProviderState(claim, response(status, "500.00", "EUR")));
        assertNull(booking.getMollieRefundId());
    }
    @Test void reconciliationRequiresSameRefundId() {
        coordinator.recordProviderState(claim, response("pending", "500.00", "EUR"));
        assertThrows(IllegalStateException.class, () -> coordinator.reconcile("tr_original",
                new MollieRefundResponse("re_other", "refunded", "tr_original", new MolliePaymentResponse.Amount("500.00", "EUR"))));
        assertEquals(RefundStatus.PENDING, booking.getRefundStatus());
    }
    static Stream<Arguments> transitions() {
        Map<RefundStatus, Set<RefundStatus>> allowed = Map.of(
                RefundStatus.CREATING, EnumSet.of(RefundStatus.QUEUED,RefundStatus.PENDING,RefundStatus.PROCESSING,RefundStatus.REFUNDED,RefundStatus.FAILED,RefundStatus.CANCELED),
                RefundStatus.QUEUED, EnumSet.of(RefundStatus.PENDING,RefundStatus.PROCESSING,RefundStatus.REFUNDED,RefundStatus.FAILED,RefundStatus.CANCELED),
                RefundStatus.PENDING, EnumSet.of(RefundStatus.PROCESSING,RefundStatus.REFUNDED,RefundStatus.FAILED,RefundStatus.CANCELED),
                RefundStatus.PROCESSING, EnumSet.of(RefundStatus.REFUNDED,RefundStatus.FAILED));
        return Arrays.stream(RefundStatus.values()).flatMap(from -> Stream.of(RefundStatus.QUEUED,RefundStatus.PENDING,
                RefundStatus.PROCESSING,RefundStatus.REFUNDED,RefundStatus.FAILED,RefundStatus.CANCELED)
                .map(to -> Arguments.of(from,to,allowed.getOrDefault(from,Set.of()).contains(to))));
    }
    @ParameterizedTest @MethodSource("transitions") void statusMatrixAndImmutableEconomics(RefundStatus from, RefundStatus to, boolean allowed) {
        booking.setRefundStatus(from); booking.setMollieRefundId("re_original");
        var historical = OffsetDateTime.ofInstant(now.minusSeconds(60), ZoneOffset.UTC);
        if (from == RefundStatus.REFUNDED) booking.setRefundedAt(historical);
        coordinator.reconcile("tr_original", response(to.name().toLowerCase(), "500.00", "EUR"));
        assertEquals(allowed ? to : from, booking.getRefundStatus());
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(new BigDecimal("500.00"), booking.getCancellationRefundAmountEur());
        assertEquals(new BigDecimal("500.00"), booking.getCancellationRetainedAmountEur());
        assertEquals(now, booking.getCancelledAt().toInstant());
        if (from == RefundStatus.REFUNDED) assertEquals(historical, booking.getRefundedAt());
        else if (allowed && to == RefundStatus.REFUNDED) assertEquals(now, booking.getRefundedAt().toInstant());
        else assertNull(booking.getRefundedAt());
    }
}
