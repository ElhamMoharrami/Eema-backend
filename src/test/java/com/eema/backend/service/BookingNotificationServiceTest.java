package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.BookingContactSnapshot;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.RefundStatus;
import com.eema.backend.event.BookingLifecycleEvent;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BookingNotificationServiceTest {
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final VenueRepository venues = mock(VenueRepository.class);
    private final EmailService email = mock(EmailService.class);
    private final UUID bookingId = UUID.randomUUID();
    private BookingNotificationService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(null, null, null, null, null, null, null,
                "https://app.eema.test/", null);
        service = new BookingNotificationService(bookings, venues, email, properties);
        Booking booking = Booking.builder().id(bookingId).venueId(UUID.randomUUID()).status(BookingStatus.PAID)
                .shootDate(LocalDate.of(2026, 8, 25)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0))
                .totalPaidEur(new BigDecimal("125.00"))
                .scoutContact(new BookingContactSnapshot("Scout Name", "snapshot-scout@example.test", "+31 6 123"))
                .ownerContact(new BookingContactSnapshot("Owner Name", "snapshot-owner@example.test", null)).build();
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        when(venues.findById(booking.getVenueId())).thenReturn(Optional.of(
                Venue.builder().name("Canal Studio").timezone("Europe/Amsterdam").build()));
    }

    @Test
    void usesSnapshotRecipientsAndRendersPaidDetailsWithoutConfirmedWording() {
        service.sendPaymentReceived(bookingId);

        ArgumentCaptor<String> recipient = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(email, times(2)).send(recipient.capture(), subject.capture(), text.capture(), html.capture());

        assertTrue(recipient.getAllValues().contains("snapshot-scout@example.test"));
        assertTrue(recipient.getAllValues().contains("snapshot-owner@example.test"));
        String all = String.join(" ", subject.getAllValues()) + String.join(" ", text.getAllValues()) + String.join(" ", html.getAllValues());
        assertTrue(all.contains("25 August 2026"));
        assertTrue(all.contains("09:00–17:00"));
        assertTrue(all.contains("https://app.eema.test/bookings/" + bookingId));
        assertTrue(all.contains("Status: PAID") || all.contains(">PAID<"));
        assertFalse(all.toLowerCase().contains("booking is confirmed"));
        assertFalse(all.toLowerCase().contains("owner payout"));
    }

    @Test
    void scoutFailureStillAttemptsOwner() {
        doThrow(new EmailService.EmailDeliveryException("failed", new RuntimeException()))
                .when(email).send(eq("snapshot-scout@example.test"), eq("Payment received for your Eema booking"), anyString(), anyString());
        service.sendPaymentReceived(bookingId);
        verify(email).send(eq("snapshot-owner@example.test"), eq("New paid booking for your Eema venue"), anyString(), anyString());
    }

    @Test
    void latePaymentMessageSaysNotConfirmedAndRefundInitiated() {
        Booking late = bookings.findById(bookingId).orElseThrow();
        late.setStatus(BookingStatus.EXPIRED);
        late.setLatePaymentDetectedAt(java.time.OffsetDateTime.now());
        late.setRefundStatus(RefundStatus.CREATING);

        service.sendLatePaymentRefundInitiated(bookingId);

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(email).send(eq("snapshot-scout@example.test"),
                eq("Your late Eema payment is being refunded"), text.capture(), anyString());
        assertTrue(text.getValue().contains("booking was not confirmed"));
        assertTrue(text.getValue().contains("full refund has been initiated"));
        assertFalse(text.getValue().contains("refund has been completed"));
    }

    @Test
    void ownerAndScoutAreNotifiedWhenRequestIsSubmitted() {
        Booking booking = bookings.findById(bookingId).orElseThrow();
        booking.setStatus(BookingStatus.REQUESTED);
        booking.setRawBrief("Editorial shoot with a small crew");
        booking.setOwnerResponseExpiresAt(OffsetDateTime.parse("2026-08-26T18:00:00+02:00"));

        service.sendLifecycle(bookingId, BookingLifecycleEvent.Type.REQUEST_SUBMITTED);

        verify(email).send(eq("snapshot-owner@example.test"), eq("New booking request"),
                contains("Editorial shoot"), contains("https://app.eema.test/bookings/" + bookingId));
        verify(email).send(eq("snapshot-scout@example.test"), eq("Your request was sent"), anyString(), anyString());
        verify(email).send(eq("snapshot-scout@example.test"), eq("Your request was sent"),
                contains("owner has until 26 August 2026 at 18:00 CEST"), anyString());
    }

    @Test
    void scoutAcceptanceEmailContainsDeadlinePayCtaAndDeepLink() {
        Booking booking = bookings.findById(bookingId).orElseThrow();
        booking.setStatus(BookingStatus.AWAITING_PAYMENT);
        booking.setPaymentExpiresAt(OffsetDateTime.parse("2026-08-26T18:00:00+02:00"));

        service.sendLifecycle(bookingId, BookingLifecycleEvent.Type.REQUEST_ACCEPTED);

        verify(email).send(eq("snapshot-scout@example.test"), eq("Your booking request was accepted"),
                contains("26 August 2026 at 18:00 CEST"), contains(">Pay now</a>"));
    }

    @Test
    void scoutReceivesDeclineAndApprovalExpiryMessages() {
        Booking booking = bookings.findById(bookingId).orElseThrow();
        booking.setStatus(BookingStatus.DECLINED);
        service.sendLifecycle(bookingId, BookingLifecycleEvent.Type.REQUEST_DECLINED);
        verify(email).send(eq("snapshot-scout@example.test"), eq("Your booking request was declined"),
                contains("Status: DECLINED"), anyString());

        clearInvocations(email);
        booking.setStatus(BookingStatus.EXPIRED);
        service.sendLifecycle(bookingId, BookingLifecycleEvent.Type.APPROVAL_EXPIRED);
        verify(email).send(eq("snapshot-scout@example.test"), eq("Your payment window expired"),
                contains("slot was released"), anyString());
    }

    @Test
    void lifecycleEmailFailureIsContained() {
        Booking booking = bookings.findById(bookingId).orElseThrow();
        booking.setStatus(BookingStatus.DECLINED);
        doThrow(new EmailService.EmailDeliveryException("failed", new RuntimeException()))
                .when(email).send(anyString(), anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> service.sendLifecycle(bookingId, BookingLifecycleEvent.Type.REQUEST_DECLINED));
    }

    @Test
    void requestExpiryAndUnavailableUseDistinctNonDeclineWording() {
        Booking booking = bookings.findById(bookingId).orElseThrow();
        booking.setStatus(BookingStatus.EXPIRED);
        service.sendLifecycle(bookingId, BookingLifecycleEvent.Type.REQUEST_EXPIRED);
        verify(email).send(eq("snapshot-scout@example.test"), eq("Your booking request expired"),
                contains("owner did not respond"), anyString());

        clearInvocations(email);
        booking.setStatus(BookingStatus.UNAVAILABLE);
        service.sendLifecycle(bookingId, BookingLifecycleEvent.Type.REQUEST_BECAME_UNAVAILABLE);
        verify(email).send(eq("snapshot-scout@example.test"), eq("The requested time is no longer available"),
                argThat(body -> body.contains("Another booking reserved") && body.contains("did not explicitly decline")), anyString());
    }
}
