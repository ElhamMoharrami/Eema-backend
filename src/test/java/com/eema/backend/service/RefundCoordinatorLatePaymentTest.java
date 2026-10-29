package com.eema.backend.service;

import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.dto.response.MollieRefundResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.RefundStatus;
import com.eema.backend.event.LatePaymentRefundInitiatedEvent;
import com.eema.backend.repository.BookingEventRepository;
import com.eema.backend.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundCoordinatorLatePaymentTest {
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingEventRepository events = mock(BookingEventRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final UUID bookingId = UUID.randomUUID();
    private final Booking booking = Booking.builder().id(bookingId).status(BookingStatus.EXPIRED)
            .molliePaymentId("tr_late").totalPaidEur(new BigDecimal("100.00"))
            .ownerPayoutEur(new BigDecimal("80.00")).latePaymentDetectedAt(OffsetDateTime.now()).build();
    private RefundCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new RefundCoordinator(bookings, events, new BookingFinancials(),
                new BookingStateMachine(), publisher);
        when(bookings.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
    }

    @Test
    void duplicateClaimReusesOneProviderIdempotencyKeyAndOneNotification() {
        var first = coordinator.claimLatePayment(bookingId).orElseThrow();
        var duplicate = coordinator.claimLatePayment(bookingId).orElseThrow();

        assertEquals(first.idempotencyKey(), duplicate.idempotencyKey());
        assertEquals(RefundStatus.CREATING, booking.getRefundStatus());
        verify(publisher, times(1)).publishEvent(new LatePaymentRefundInitiatedEvent(bookingId));
    }

    @Test
    void authoritativeRefundCompletionKeepsBookingExpired() {
        var claim = coordinator.claimLatePayment(bookingId).orElseThrow();
        coordinator.recordProviderState(claim, new MollieRefundResponse("re_1", "refunded", "tr_late",
                new MolliePaymentResponse.Amount("100.00", "EUR")));

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(RefundStatus.REFUNDED, booking.getRefundStatus());
        assertNotNull(booking.getRefundedAt());
        assertEquals(new BigDecimal("0.00"), booking.getOwnerPayoutEur());
        assertTrue(coordinator.claimLatePayment(bookingId).isEmpty());
    }
}
