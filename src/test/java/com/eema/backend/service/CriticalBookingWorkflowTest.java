package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.config.BookingWorkflowProperties;
import com.eema.backend.dto.request.CreateBookingRequest;
import com.eema.backend.dto.request.ScoutContactRequest;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.dto.response.MollieRefundResponse;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.event.BookingLifecycleEvent;
import com.eema.backend.event.BookingPaidEvent;
import com.eema.backend.event.LatePaymentDetectedEvent;
import com.eema.backend.event.LatePaymentRefundInitiatedEvent;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CriticalBookingWorkflowTest {
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final VenueRepository venues = mock(VenueRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final BookingEventRepository audit = mock(BookingEventRepository.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BookingAvailabilityValidator availability = mock(BookingAvailabilityValidator.class);
    private final MollieProviderClient mollie = mock(MollieProviderClient.class);
    private final RefundService refunds = mock(RefundService.class);
    private final OwnerPayoutService payouts = mock(OwnerPayoutService.class);
    private final UUID bookingId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final User scout = User.builder().id(UUID.randomUUID()).email("scout@example.test")
            .fullName("Scout").role(UserRole.SCOUT).build();
    private final User owner = User.builder().id(UUID.randomUUID()).email("owner@example.test")
            .fullName("Owner").role(UserRole.OWNER).build();
    private final AtomicReference<Booking> stored = new AtomicReference<>();
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(null, null, null, null, null, null,
                new AppProperties.CommissionProperties(new BigDecimal("20.00")), null, null);
        bookingService = new BookingService(bookings, venues, users, audit, properties, current,
                new BookingStateMachine(), new BookingFinancials(), new BookingPriceCalculator(), events, availability,
                new BookingWorkflowProperties(24, 24, 60000),
                new BookingCancellationPolicy(new BookingFinancials()), payouts);
        Venue venue = Venue.builder().id(venueId).owner(owner).name("Workflow Studio")
                .timezone("Europe/Amsterdam").status(VenueStatus.ACTIVE)
                .hourlyRateEur(new BigDecimal("25.00"))
                .minimumBookingMinutes(240).fullDayRateEur(new BigDecimal("100.00")).build();
        when(venues.findById(venueId)).thenReturn(Optional.of(venue));
        when(users.findById(scout.getId())).thenReturn(Optional.of(scout));
        when(bookings.saveAndFlush(any())).thenAnswer(invocation -> {
            Booking value = invocation.getArgument(0);
            value.setId(bookingId);
            stored.set(value);
            return value;
        });
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookings.findByIdForUpdate(bookingId)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(bookings.findById(bookingId)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(venues.findByIdForUpdate(venueId)).thenReturn(Optional.of(venue));
    }

    @Test
    void requestApprovalCheckoutAndAuthoritativePaymentCompleteOneReservedBooking() {
        Booking booking = createRequest();
        assertEquals(BookingStatus.REQUESTED, booking.getStatus());
        assertFalse(BookingReservationPolicy.blocksAvailability(booking.getStatus()));

        when(current.getCurrentUser()).thenReturn(owner);
        bookingService.acceptRequest(bookingId);
        assertEquals(BookingStatus.AWAITING_PAYMENT, booking.getStatus());
        assertTrue(BookingReservationPolicy.blocksAvailability(booking.getStatus()));

        PaymentCreationCoordinator coordinator = new PaymentCreationCoordinator(bookings, new BookingFinancials());
        PaymentService payments = new PaymentService(coordinator, mollie);
        MolliePaymentResponse created = payment("open", true);
        when(mollie.createPayment(eq(bookingId), eq(new BigDecimal("100.00")), any())).thenReturn(created);
        var checkout = payments.createPayment(bookingId, scout.getId());
        assertEquals("tr_workflow", checkout.molliePaymentId());
        assertEquals(PaymentCreationStatus.CREATED, booking.getPaymentCreationStatus());

        MolliePaymentResponse paid = payment("paid", false);
        when(mollie.getPayment("tr_workflow")).thenReturn(paid);
        WebhookService webhook = new WebhookService(mollie, bookingService, refunds);
        webhook.handlePaymentWebhook("tr_workflow");
        webhook.handlePaymentWebhook("tr_workflow");

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertTrue(BookingReservationPolicy.blocksAvailability(booking.getStatus()));
        verify(events).publishEvent(new BookingLifecycleEvent(bookingId, BookingLifecycleEvent.Type.REQUEST_SUBMITTED));
        verify(events).publishEvent(new BookingLifecycleEvent(bookingId, BookingLifecycleEvent.Type.REQUEST_ACCEPTED));
        verify(events, times(1)).publishEvent(new BookingPaidEvent(bookingId));
        verify(audit, atLeastOnce()).save(argThat(event -> event.getEventType() == BookingEventType.PAYMENT_SUCCEEDED));
        verify(audit, atLeastOnce()).save(argThat(event -> event.getEventType() == BookingEventType.BOOKING_CONFIRMED));
    }

    @Test
    void declineAndExpiryReleaseSlotsAndRejectCheckout() {
        Booking declined = createRequest();
        when(current.getCurrentUser()).thenReturn(owner);
        bookingService.declineRequest(bookingId);
        assertEquals(BookingStatus.DECLINED, declined.getStatus());
        assertFalse(BookingReservationPolicy.blocksAvailability(declined.getStatus()));
        PaymentCreationCoordinator coordinator = new PaymentCreationCoordinator(bookings, new BookingFinancials());
        assertThrows(com.eema.backend.exception.BadRequestException.class,
                () -> coordinator.claim(bookingId, scout.getId()));
        verify(events).publishEvent(new BookingLifecycleEvent(bookingId, BookingLifecycleEvent.Type.REQUEST_DECLINED));

        resetForNewRequest();
        Booking expired = createRequest();
        when(current.getCurrentUser()).thenReturn(owner);
        bookingService.acceptRequest(bookingId);
        bookingService.expireApproval(bookingId, expired.getPaymentExpiresAt().plusSeconds(1));
        assertEquals(BookingStatus.EXPIRED, expired.getStatus());
        assertFalse(BookingReservationPolicy.blocksAvailability(expired.getStatus()));
        assertThrows(com.eema.backend.exception.BadRequestException.class,
                () -> coordinator.claim(bookingId, scout.getId()));
        verify(events).publishEvent(new BookingLifecycleEvent(bookingId, BookingLifecycleEvent.Type.APPROVAL_EXPIRED));
    }

    @Test
    void paidCallbackAfterDeadlineKeepsSlotReleasedAndInitiatesOneRefund() {
        Booking booking = createRequest();
        when(current.getCurrentUser()).thenReturn(owner);
        bookingService.acceptRequest(bookingId);
        PaymentCreationCoordinator paymentCoordinator = new PaymentCreationCoordinator(bookings, new BookingFinancials());
        when(mollie.createPayment(eq(bookingId), any(), any())).thenReturn(payment("open", true));
        new PaymentService(paymentCoordinator, mollie).createPayment(bookingId, scout.getId());
        booking.setPaymentExpiresAt(OffsetDateTime.now().minusSeconds(1));

        RefundCoordinator refundCoordinator = new RefundCoordinator(bookings, audit, new BookingFinancials(),
                new BookingStateMachine(), events, payouts);
        RefundService refundService = new RefundService(current, refundCoordinator, mollie, bookingService);
        MollieRefundResponse queued = new MollieRefundResponse("re_late", "queued", "tr_workflow",
                new MolliePaymentResponse.Amount("100.00", "EUR"));
        when(mollie.getPayment("tr_workflow")).thenReturn(payment("paid", false));
        when(mollie.createRefund(eq("tr_workflow"), eq(bookingId), eq(new BigDecimal("100.00")), any()))
                .thenReturn(queued);
        when(bookings.findByMolliePaymentId("tr_workflow")).thenReturn(Optional.of(booking));
        when(mollie.getRefund("tr_workflow", "re_late")).thenReturn(queued);

        WebhookService webhook = new WebhookService(mollie, bookingService, refundService);
        webhook.handlePaymentWebhook("tr_workflow");
        webhook.handlePaymentWebhook("tr_workflow");

        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        assertEquals(RefundStatus.QUEUED, booking.getRefundStatus());
        assertFalse(BookingReservationPolicy.blocksAvailability(booking.getStatus()));
        verify(mollie, times(1)).createRefund(eq("tr_workflow"), eq(bookingId), any(), any());
        verify(events, never()).publishEvent(any(BookingPaidEvent.class));
        verify(events, times(1)).publishEvent(new LatePaymentDetectedEvent(bookingId));
        verify(events, times(1)).publishEvent(new LatePaymentRefundInitiatedEvent(bookingId));
    }

    private Booking createRequest() {
        var dto = bookingService.createBooking(new CreateBookingRequest(venueId, LocalDate.now().plusDays(5),
                LocalTime.of(9, 0), LocalTime.of(17, 0), "Production workflow",
                new ScoutContactRequest("Scout", "scout@example.test", null)), scout.getId());
        assertEquals(bookingId, dto.id());
        return stored.get();
    }

    private void resetForNewRequest() { stored.set(null); }

    private MolliePaymentResponse payment(String status, boolean checkout) {
        return new MolliePaymentResponse("tr_workflow", status,
                new MolliePaymentResponse.Amount("100.00", "EUR"),
                checkout ? new MolliePaymentResponse.MollieLinks(
                        new MolliePaymentResponse.MollieLink("https://mollie.test/checkout")) : null,
                Map.of("bookingId", bookingId.toString()));
    }
}
