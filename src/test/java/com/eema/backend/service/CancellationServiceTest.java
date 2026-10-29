package com.eema.backend.service;

import com.eema.backend.dto.request.CancelBookingRequest;
import com.eema.backend.dto.response.CancellationPreviewDto;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.CancellationPolicyTier;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.repository.BookingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class CancellationServiceTest {
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingCancellationCoordinator coordinator = mock(BookingCancellationCoordinator.class);
    private final RefundService refunds = mock(RefundService.class);
    private final MollieProviderClient mollie = mock(MollieProviderClient.class);
    private final BookingService bookingService = mock(BookingService.class);
    private final CancellationClock clock = mock(CancellationClock.class);
    private final CancellationService service = new CancellationService(
            current, bookings, coordinator, refunds, mollie, bookingService, clock);
    private final UUID bookingId = UUID.randomUUID();
    private final User scout = User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-01T10:00:00Z");

    @Test
    void openAwaitingPaymentIsCancelledAtMollieBeforeWithdrawal() {
        Booking booking = Booking.builder().id(bookingId).status(BookingStatus.AWAITING_PAYMENT)
                .molliePaymentId("tr_open").build();
        arrange(booking);
        when(mollie.getPayment("tr_open")).thenReturn(payment("tr_open", "open"));
        when(mollie.cancelPayment("tr_open")).thenReturn(payment("tr_open", "canceled"));
        when(coordinator.cancel(bookingId, scout, null, null))
                .thenReturn(new BookingCancellationCoordinator.CancellationResult(bookingId, null));

        service.cancel(bookingId, null);

        verify(mollie).cancelPayment("tr_open");
        verifyNoInteractions(refunds);
    }

    @Test
    void paidProviderStateIsAppliedBeforeConfirmedCancellationPolicy() {
        Booking booking = Booking.builder().id(bookingId).status(BookingStatus.AWAITING_PAYMENT)
                .molliePaymentId("tr_paid").build();
        arrange(booking);
        MolliePaymentResponse paid = payment("tr_paid", "paid");
        when(mollie.getPayment("tr_paid")).thenReturn(paid);
        RefundCoordinator.Claim claim = new RefundCoordinator.Claim(
                bookingId, "tr_paid", new BigDecimal("100.00"), UUID.randomUUID());
        when(coordinator.cancel(bookingId, scout, null, null))
                .thenReturn(new BookingCancellationCoordinator.CancellationResult(bookingId, claim));

        service.cancel(bookingId, null);

        var order = inOrder(bookingService, coordinator, refunds);
        order.verify(bookingService).processPaidPayment(paid);
        order.verify(coordinator).cancel(bookingId, scout, null, null);
        order.verify(refunds).requestCancellationRefund(claim);
    }

    @Test
    void zeroRefundCancellationNeverCallsRefundProvider() {
        Booking booking = Booking.builder().id(bookingId).status(BookingStatus.CONFIRMED).build();
        arrange(booking);
        when(coordinator.cancel(bookingId, scout, null, null))
                .thenReturn(new BookingCancellationCoordinator.CancellationResult(bookingId, null));

        service.cancel(bookingId, new CancelBookingRequest(null, null));

        verifyNoInteractions(mollie, refunds);
    }

    @Test
    void duplicateCancellationResumesAmbiguousRefundWithPersistedClaim() {
        Booking booking = Booking.builder().id(bookingId).status(BookingStatus.CANCELLED).build();
        when(current.getCurrentUser()).thenReturn(scout);
        when(clock.now()).thenReturn(now);
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        when(coordinator.preview(bookingId, scout, now)).thenReturn(new CancellationPreviewDto(
                bookingId, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, false, null));

        service.cancel(bookingId, null);

        verify(refunds).resumeCancellationRefund(bookingId);
        verify(coordinator, never()).cancel(any(), any(), any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"canceled", "expired", "failed"})
    void alreadyTerminalPaymentNeedsNoProviderCancellation(String status) {
        Booking booking = Booking.builder().id(bookingId).status(BookingStatus.AWAITING_PAYMENT).molliePaymentId("tr_open").build();
        arrange(booking);
        when(mollie.getPayment("tr_open")).thenReturn(payment("tr_open", status));
        when(coordinator.cancel(bookingId, scout, null, null)).thenReturn(new BookingCancellationCoordinator.CancellationResult(bookingId, null));
        service.cancel(bookingId, null);
        verify(mollie, never()).cancelPayment(anyString());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"timeout", "rejection", "stillOpen", "wrongPayment", "missing"})
    void unsuccessfulProviderCancellationCannotWithdraw(String variant) {
        Booking booking = Booking.builder().id(bookingId).status(BookingStatus.AWAITING_PAYMENT).molliePaymentId("tr_open").build();
        arrange(booking);
        when(mollie.getPayment("tr_open")).thenReturn(payment("tr_open", "open"));
        switch (variant) {
            case "timeout" -> when(mollie.cancelPayment("tr_open")).thenThrow(new org.springframework.web.client.ResourceAccessException("QA timeout"));
            case "rejection" -> when(mollie.cancelPayment("tr_open")).thenThrow(org.springframework.web.client.HttpClientErrorException.create(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,"QA rejected",org.springframework.http.HttpHeaders.EMPTY,new byte[0],null));
            case "stillOpen" -> when(mollie.cancelPayment("tr_open")).thenReturn(payment("tr_open", "open"));
            case "wrongPayment" -> when(mollie.cancelPayment("tr_open")).thenReturn(payment("tr_other", "canceled"));
        }
        org.junit.jupiter.api.Assertions.assertThrows(com.eema.backend.exception.BadRequestException.class, () -> service.cancel(bookingId, null));
        verify(coordinator, never()).cancel(any(), any(), any(), any());
        verifyNoInteractions(refunds);
    }

    private void arrange(Booking booking) {
        when(current.getCurrentUser()).thenReturn(scout);
        when(clock.now()).thenReturn(now);
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        when(coordinator.preview(bookingId, scout, now)).thenReturn(new CancellationPreviewDto(
                bookingId, CancellationPolicyTier.PRE_PAYMENT_WITHDRAWAL,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, true, null));
    }

    private MolliePaymentResponse payment(String id, String status) {
        return new MolliePaymentResponse(id, status,
                new MolliePaymentResponse.Amount("100.00", "EUR"), null,
                Map.of("bookingId", bookingId.toString()));
    }
}
