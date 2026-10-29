package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.BookingContactSnapshot;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        when(venues.findById(booking.getVenueId())).thenReturn(Optional.of(Venue.builder().name("Canal Studio").build()));
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
}
