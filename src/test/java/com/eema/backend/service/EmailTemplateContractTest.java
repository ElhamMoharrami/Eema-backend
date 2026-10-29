package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.config.AuthRecoveryProperties;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.event.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Existing EMAIL contracts: actual composers, synthetic data, no provider or persistence. */
class EmailTemplateContractTest {
    static final String BASE = "https://app.eema.example";
    static final String MARKUP = "Élodie نور <img src=x onerror=alert(1)> & Co";
    static final String PRIVATE = "SYNTHETIC_PRIVATE_BANK_MARKER";
    static final UUID BOOKING = new UUID(0, 1), VENUE = new UUID(0, 2), INQUIRY = new UUID(0, 3),
            VERIFICATION = new UUID(0, 4), DESTINATION = new UUID(0, 5);
    record Mail(String to, String subject, String text, String html) {}

    static class Fixture {
        final EmailService email = mock(EmailService.class);
        final BookingRepository bookings = mock(BookingRepository.class);
        final VenueRepository venues = mock(VenueRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final AvailabilityInquiryRepository inquiries = mock(AvailabilityInquiryRepository.class);
        final OwnerVerificationRepository verifications = mock(OwnerVerificationRepository.class);
        final OwnerPayoutDestinationRepository destinations = mock(OwnerPayoutDestinationRepository.class);
        final List<Mail> sent = new ArrayList<>();
        final User owner = User.builder().id(new UUID(0, 6)).email("current-owner@example.test").fullName(MARKUP).role(UserRole.OWNER).build();
        final User scout = User.builder().id(new UUID(0, 7)).email("current-scout@example.test").fullName(MARKUP).role(UserRole.SCOUT).build();
        final Venue venue = Venue.builder().id(VENUE).owner(owner).name(MARKUP).timezone("Europe/Amsterdam").build();
        final Booking booking = Booking.builder().id(BOOKING).venueId(VENUE).status(BookingStatus.CONFIRMED)
                .shootDate(LocalDate.of(2026, 8, 25)).startTime(LocalTime.of(9, 15)).endTime(LocalTime.of(13, 45))
                .totalPaidEur(new BigDecimal("125.00")).rawBrief(MARKUP)
                .ownerResponseExpiresAt(OffsetDateTime.parse("2026-08-24T18:00:00+02:00"))
                .paymentExpiresAt(OffsetDateTime.parse("2026-08-24T18:00:00+02:00"))
                .scoutContact(new BookingContactSnapshot(MARKUP, "snapshot-scout@example.test", null))
                .ownerContact(new BookingContactSnapshot(MARKUP, "snapshot-owner@example.test", null)).build();
        final AvailabilityInquiry inquiry = AvailabilityInquiry.builder().id(INQUIRY).venueId(VENUE).scoutId(scout.getId())
                .requestedDate(LocalDate.of(2026, 8, 25)).requestedStartTime(LocalTime.of(9, 15)).requestedEndTime(LocalTime.of(13, 45))
                .offeredDate(LocalDate.of(2026, 8, 26)).offeredStartTime(LocalTime.of(10, 0)).offeredEndTime(LocalTime.of(12, 0))
                .offerExpiresAt(OffsetDateTime.parse("2026-08-24T18:00:00+02:00")).message(MARKUP).build();
        final AppProperties app = new AppProperties(null, null, null, null, null, null, null, BASE + "///", null);
        final BookingNotificationService bookingMail = new BookingNotificationService(bookings, venues, email, app);
        final AvailabilityInquiryNotificationService inquiryMail = new AvailabilityInquiryNotificationService(inquiries, venues, users, email, app);
        final TrustNotificationService trustMail = new TrustNotificationService(verifications, venues, destinations, users, email, app);
        final AccountEmailNotificationService accountMail = new AccountEmailNotificationService(users, email, app, new AuthRecoveryProperties(24, 60, 3_600_000));
        Fixture() {
            when(bookings.findById(BOOKING)).thenReturn(Optional.of(booking));
            when(venues.findById(VENUE)).thenReturn(Optional.of(venue));
            when(users.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(users.findById(scout.getId())).thenReturn(Optional.of(scout));
            when(inquiries.findById(INQUIRY)).thenReturn(Optional.of(inquiry));
            when(verifications.findById(VERIFICATION)).thenReturn(Optional.of(OwnerVerification.builder().id(VERIFICATION).owner(owner).build()));
            when(destinations.findById(DESTINATION)).thenReturn(Optional.of(OwnerPayoutDestination.builder().id(DESTINATION)
                    .ownerId(owner.getId()).accountHolderName(PRIVATE).ibanEncrypted(PRIVATE).ibanFingerprint(PRIVATE).ibanLast4("0000").build()));
            doAnswer(i -> { sent.add(new Mail(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3))); return null; })
                    .when(email).send(anyString(), anyString(), anyString(), anyString());
        }
        Mail only(String recipient, String subject, String path) {
            var matches = sent.stream().filter(m -> m.to().equals(recipient)).toList();
            assertEquals(1, matches.size());
            Mail mail = matches.getFirst(); assertEquals(subject, mail.subject());
            assertTrue(mail.text().contains(BASE + path)); assertTrue(mail.html().contains("href=\"" + BASE + path + "\""));
            for (Mail m : sent) {
                assertFalse(m.html().contains("<img")); assertFalse(m.html().contains(PRIVATE)); assertFalse(m.text().contains(PRIVATE));
                assertFalse(m.text().toLowerCase(Locale.ROOT).contains("payout has been transferred"));
                assertFalse(m.text().contains("password123"));
            }
            return mail;
        }
    }

    @ParameterizedTest(name = "EMAIL-{0} account token link and expiry")
    @ValueSource(ints = {1, 2})
    void accountLinksEncodeTokensAndUseCurrentRecipient(int id) {
        Fixture f = new Fixture(); String token = "synthetic+/token<&";
        if (id == 1) f.accountMail.sendVerification(f.scout.getId(), token); else f.accountMail.sendPasswordReset(f.scout.getId(), token);
        Mail m = f.only(f.scout.getEmail(), id == 1 ? "Verify your Eema email" : "Reset your Eema password",
                (id == 1 ? "/verify-email" : "/reset-password") + "?token=synthetic%2B%2Ftoken%3C%26");
        assertTrue(m.text().contains(id == 1 ? "24 hours" : "60 minutes"));
        assertTrue(m.html().contains(id == 1 ? "24 hours" : "60 minutes"));
        assertFalse(m.html().contains(token)); assertEquals(1, f.sent.size());
    }

    @ParameterizedTest
    @EnumSource(BookingLifecycleEvent.Type.class)
    void requestTemplatesPreserveSnapshotRecipientsEscapeUnicodeAndUseExistingBookingRoute(BookingLifecycleEvent.Type type) {
        Fixture f = new Fixture();
        BookingStatus state = switch (type) {
            case REQUEST_SUBMITTED -> BookingStatus.REQUESTED;
            case REQUEST_ACCEPTED -> BookingStatus.AWAITING_PAYMENT;
            case REQUEST_DECLINED -> BookingStatus.DECLINED;
            case REQUEST_BECAME_UNAVAILABLE -> BookingStatus.UNAVAILABLE;
            default -> BookingStatus.EXPIRED;
        };
        String subject = switch (type) {
            case REQUEST_SUBMITTED -> "Your request was sent";
            case REQUEST_ACCEPTED -> "Your booking request was accepted";
            case REQUEST_DECLINED -> "Your booking request was declined";
            case REQUEST_EXPIRED -> "Your booking request expired";
            case REQUEST_BECAME_UNAVAILABLE -> "The requested time is no longer available";
            case APPROVAL_EXPIRED -> "Your payment window expired";
        };
        f.booking.setStatus(state); f.bookingMail.sendLifecycle(BOOKING, type);
        Mail m = f.only("snapshot-scout@example.test", subject, "/bookings/" + BOOKING);
        assertTrue(m.text().contains(state.name())); assertTrue(m.text().contains(MARKUP));
        assertTrue(org.springframework.web.util.HtmlUtils.htmlUnescape(m.html()).contains(MARKUP)); assertTrue(m.text().contains("Europe/Amsterdam"));
        assertEquals(type == BookingLifecycleEvent.Type.REQUEST_SUBMITTED ? 2 : 1, f.sent.size());
        if (type == BookingLifecycleEvent.Type.REQUEST_SUBMITTED) {
            Mail owner = f.only("snapshot-owner@example.test", "New booking request", "/bookings/" + BOOKING);
            assertTrue(owner.text().contains("not reserved")); assertTrue(owner.text().contains("18:00 CEST"));
            assertTrue(owner.text().contains("Brief: " + MARKUP)); assertTrue(m.text().contains("not yet a confirmed or paid booking"));
        }
        if (type == BookingLifecycleEvent.Type.REQUEST_ACCEPTED) { assertTrue(m.html().contains(">Pay now</a>")); assertTrue(m.text().contains("18:00 CEST")); }
    }

    @Test void confirmedAndLatePaymentAndCompletedRefundHaveDifferentMoneyClaims() {
        Fixture f = new Fixture(); f.bookingMail.sendPaymentReceived(BOOKING);
        Mail scout = f.only("snapshot-scout@example.test", "Your Eema booking is confirmed", "/bookings/" + BOOKING);
        Mail owner = f.only("snapshot-owner@example.test", "Confirmed booking for your Eema venue", "/bookings/" + BOOKING);
        assertEquals(2, f.sent.size()); assertTrue(scout.text().contains("€125.00")); assertTrue(owner.text().contains("booking is confirmed"));
        assertFalse(owner.text().toLowerCase(Locale.ROOT).contains("payout"));
        f.sent.clear(); f.booking.setStatus(BookingStatus.EXPIRED); f.booking.setLatePaymentDetectedAt(OffsetDateTime.parse("2026-08-24T19:00:00+02:00")); f.booking.setRefundStatus(RefundStatus.CREATING);
        f.bookingMail.sendLatePaymentRefundInitiated(BOOKING);
        Mail late = f.only("snapshot-scout@example.test", "Your late Eema payment is being refunded", "/bookings/" + BOOKING);
        assertEquals(1, f.sent.size()); assertTrue(late.text().contains("booking was not confirmed")); assertTrue(late.text().contains("full refund has been initiated")); assertFalse(late.text().contains("has been processed"));
        f.sent.clear(); f.booking.setStatus(BookingStatus.CANCELLED); f.booking.setRefundAmountEur(new BigDecimal("62.50"));
        f.bookingMail.sendCancellationRefundCompleted(BOOKING); assertTrue(f.sent.isEmpty());
        f.booking.setRefundStatus(RefundStatus.REFUNDED); f.bookingMail.sendCancellationRefundCompleted(BOOKING);
        Mail completed = f.only("snapshot-scout@example.test", "Your Eema refund has been processed", "/bookings/" + BOOKING);
        assertTrue(completed.text().contains("€62.50")); assertTrue(completed.text().contains("Bank settlement timing may vary"));
    }

    @ParameterizedTest
    @EnumSource(value = ActorType.class, names = {"SCOUT", "OWNER", "ADMIN"})
    void cancellationRoutesByActorAndDoesNotClaimRefundCompletion(ActorType actor) {
        Fixture f = new Fixture(); f.booking.setStatus(BookingStatus.CANCELLED); f.booking.setCancelledByActorType(actor);
        f.booking.setCancellationPolicyTier(actor == ActorType.SCOUT ? CancellationPolicyTier.PARTIAL_REFUND : actor == ActorType.OWNER ? CancellationPolicyTier.OWNER_FULL_REFUND : CancellationPolicyTier.ADMIN_FULL_REFUND);
        f.booking.setCancellationReason(MARKUP); f.booking.setRefundStatus(RefundStatus.QUEUED);
        BigDecimal refund = new BigDecimal(actor == ActorType.SCOUT ? "62.50" : "125.00");
        f.booking.setCancellationRefundAmountEur(refund); f.booking.setCancellationRetainedAmountEur(new BigDecimal("125.00").subtract(refund));
        f.bookingMail.sendCancellation(BOOKING);
        Mail scout = f.only("snapshot-scout@example.test", "Your Eema booking was cancelled", "/bookings/" + BOOKING);
        assertTrue(scout.text().contains("refund amount is €" + refund)); assertFalse(scout.text().contains("has been processed"));
        assertTrue(scout.html().contains("&lt;img")); assertEquals(actor == ActorType.SCOUT ? 2 : 1, f.sent.size());
        if (actor == ActorType.SCOUT) {
            Mail owner = f.only("snapshot-owner@example.test", "A scout cancelled an Eema booking", "/bookings/" + BOOKING);
            assertTrue(owner.text().contains("retained amount is €62.50")); assertTrue(owner.text().contains("View the booking for your payout details"));
        } else assertTrue(scout.text().contains(actor == ActorType.OWNER ? "venue owner cancelled" : "Eema cancelled"));
    }

    @Test void nonRefundableCancellationNeverClaimsMoneyIsBeingReturned() {
        Fixture f = new Fixture(); f.booking.setStatus(BookingStatus.CANCELLED); f.booking.setCancelledByActorType(ActorType.SCOUT);
        f.booking.setCancellationPolicyTier(CancellationPolicyTier.NON_REFUNDABLE); f.booking.setRefundStatus(RefundStatus.NONE);
        f.booking.setCancellationRefundAmountEur(new BigDecimal("0.00")); f.booking.setCancellationRetainedAmountEur(new BigDecimal("125.00"));
        f.bookingMail.sendCancellation(BOOKING);
        Mail m = f.only("snapshot-scout@example.test", "Your Eema booking was cancelled", "/bookings/" + BOOKING);
        assertTrue(m.text().contains("No refund is due")); assertFalse(m.text().contains("has been processed"));
    }

    @ParameterizedTest
    @EnumSource(AvailabilityInquiryLifecycleEvent.Type.class)
    void inquiryMessagesUseCurrentRecipientAndEscapeAllUntrustedContent(AvailabilityInquiryLifecycleEvent.Type type) {
        Fixture f = new Fixture(); f.inquiry.setStatus(switch (type) { case CREATED -> AvailabilityInquiryStatus.OPEN; case OFFERED -> AvailabilityInquiryStatus.OFFERED; case DECLINED -> AvailabilityInquiryStatus.DECLINED; case OFFER_EXPIRED -> AvailabilityInquiryStatus.EXPIRED; });
        String subject = switch (type) { case CREATED -> "New availability inquiry"; case OFFERED -> "The owner offered you a time"; case DECLINED -> "Your availability inquiry was declined"; case OFFER_EXPIRED -> "Your availability offer expired"; };
        f.inquiryMail.sendLifecycle(INQUIRY, type);
        Mail m = f.only(type == AvailabilityInquiryLifecycleEvent.Type.CREATED ? f.owner.getEmail() : f.scout.getEmail(), subject,
                type == AvailabilityInquiryLifecycleEvent.Type.CREATED ? "/owner/inquiries" : "/scout/inquiries");
        assertEquals(1, f.sent.size()); assertTrue(m.text().contains(MARKUP)); assertTrue(org.springframework.web.util.HtmlUtils.htmlUnescape(m.html()).contains(MARKUP));
        assertTrue(m.text().contains("Europe/Amsterdam"));
        if (type == AvailabilityInquiryLifecycleEvent.Type.OFFERED) { assertTrue(m.text().contains("18:00 CEST")); assertTrue(m.text().contains("Original request")); assertTrue(m.html().contains(">Review offer</a>")); }
    }

    @ParameterizedTest
    @EnumSource(value = TrustEventType.class, names = {"OWNER_VERIFICATION_SUBMITTED", "OWNER_VERIFICATION_APPROVED", "OWNER_VERIFICATION_REJECTED", "VENUE_SUBMITTED", "VENUE_APPROVED", "VENUE_CHANGES_REQUESTED", "VENUE_SUSPENDED", "VENUE_REACTIVATED", "PAYOUT_DESTINATION_SUBMITTED", "PAYOUT_DESTINATION_VERIFIED", "PAYOUT_DESTINATION_REJECTED"})
    void trustTemplatesUseManagementRouteAndNeverIncludeBankFields(TrustEventType type) {
        Fixture f = new Fixture(); boolean destination = type.name().startsWith("PAYOUT_DESTINATION_"); boolean verification = type.name().startsWith("OWNER_VERIFICATION_");
        String subject = switch (type) {
            case OWNER_VERIFICATION_SUBMITTED -> "Owner verification submitted";
            case OWNER_VERIFICATION_APPROVED -> "Your Eema owner verification was approved";
            case OWNER_VERIFICATION_REJECTED -> "Your Eema owner verification needs attention";
            case VENUE_SUBMITTED -> "Venue submitted for review"; case VENUE_APPROVED -> "Your venue is live";
            case VENUE_CHANGES_REQUESTED -> "Changes requested for your venue"; case VENUE_SUSPENDED -> "Your venue was suspended"; case VENUE_REACTIVATED -> "Your venue is live again";
            case PAYOUT_DESTINATION_SUBMITTED -> "Payout destination submitted"; case PAYOUT_DESTINATION_VERIFIED -> "Payout destination verified"; case PAYOUT_DESTINATION_REJECTED -> "Your payout destination needs attention";
            default -> throw new IllegalArgumentException();
        };
        f.trustMail.send(new TrustNotificationEvent(type, destination ? DESTINATION : verification ? VERIFICATION : VENUE, MARKUP, UUID.randomUUID()));
        Mail m = f.only(f.owner.getEmail(), subject, destination ? "/owner/payout-destination" : verification ? "/owner/verification" : "/owner/venues/" + VENUE);
        assertEquals(1, f.sent.size());
        if (m.text().contains(MARKUP)) assertTrue(org.springframework.web.util.HtmlUtils.htmlUnescape(m.html()).contains(MARKUP));
        if (type == TrustEventType.PAYOUT_DESTINATION_VERIFIED) assertTrue(m.text().contains("future eligible payouts"));
        if (type == TrustEventType.VENUE_SUSPENDED) assertFalse(m.text().contains("bookings cancelled"));
    }

    @ParameterizedTest
    @EnumSource(value = TrustEventType.class, names = {"PAYOUT_DESTINATION_REPLACED", "PAYOUT_DESTINATION_REVEALED", "PAYOUT_DESTINATION_SNAPSHOTTED", "INCIDENT_REPORTED", "INCIDENT_REVIEW_STARTED", "INCIDENT_RESOLVED", "INCIDENT_CLOSED"})
    void unsupportedTrustEventsHaveNoDedicatedEmail(TrustEventType type) {
        Fixture f = new Fixture(); f.trustMail.send(new TrustNotificationEvent(type, type.name().startsWith("PAYOUT") ? DESTINATION : VENUE, null, UUID.randomUUID()));
        assertTrue(f.sent.isEmpty()); verifyNoInteractions(f.email);
    }
}
