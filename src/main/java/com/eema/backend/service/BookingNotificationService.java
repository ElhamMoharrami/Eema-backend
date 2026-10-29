package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.BookingContactSnapshot;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.event.BookingLifecycleEvent;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final EmailService emailService;
    private final AppProperties properties;

    @Transactional(readOnly = true)
    public void sendPaymentReceived(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.CONFIRMED) {
            log.warn("Payment email skipped: booking missing or not confirmed, bookingId={}", bookingId);
            return;
        }
        String venueName = venueRepository.findById(booking.getVenueId()).map(Venue::getName).orElse("your selected venue");
        String url = bookingUrl(bookingId);
        attempt("PAYMENT_RECEIVED", "scout", bookingId, () -> sendScout(booking, venueName, url));
        attempt("PAYMENT_RECEIVED", "owner", bookingId, () -> sendOwner(booking, venueName, url));
    }

    @Transactional(readOnly = true)
    public void sendLatePaymentRefundInitiated(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.EXPIRED
                || booking.getLatePaymentDetectedAt() == null
                || booking.getRefundStatus() == com.eema.backend.entity.enums.RefundStatus.NONE) {
            log.warn("Late-payment email skipped: booking state is not eligible, bookingId={}", bookingId);
            return;
        }
        BookingContactSnapshot scout = booking.getScoutContact();
        String venueName = venueRepository.findById(booking.getVenueId()).map(Venue::getName)
                .orElse("your selected venue");
        String url = bookingUrl(bookingId);
        String message = "Your payment was received after the payment window expired. "
                + "The booking was not confirmed, and a full refund has been initiated.";
        String text = "Hello %s,\n\n%s\n\n%s\n\nView booking: %s"
                .formatted(value(scout.getName()), message, details(booking, venueName), url);
        attempt("LATE_PAYMENT_REFUND", "scout", bookingId, () -> emailService.send(scout.getEmail(),
                "Your late Eema payment is being refunded", text,
                html("Your payment is being refunded", scout.getName(), message,
                        detailsHtml(booking, venueName), "View booking", url)));
    }

    @Transactional(readOnly = true)
    public void sendCancellation(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.CANCELLED
                || booking.getCancellationPolicyTier() == null) return;
        String venueName = venueRepository.findById(booking.getVenueId()).map(Venue::getName)
                .orElse("your selected venue");
        String url = bookingUrl(bookingId);
        boolean refundInitiated = booking.getRefundStatus()
                != com.eema.backend.entity.enums.RefundStatus.NONE;
        String refundLine = refundInitiated
                ? "A refund of €" + money(booking.getCancellationRefundAmountEur())
                + " has been initiated. It is not yet completed."
                : "No refund is due under the cancellation policy.";
        String actorLine = switch (booking.getCancelledByActorType()) {
            case OWNER -> "The venue owner cancelled this booking. We are sorry for the disruption. ";
            case ADMIN -> "Eema cancelled this booking for the recorded operational reason. ";
            default -> "Your booking has been cancelled. ";
        };
        String scoutMessage = actorLine + refundLine;
        attempt("BOOKING_CANCELLED", "scout", bookingId, () -> emailService.send(
                booking.getScoutContact().getEmail(), "Your Eema booking was cancelled",
                text(booking.getScoutContact().getName(), scoutMessage, details(booking, venueName),
                        "View booking", url),
                html("Booking cancelled", booking.getScoutContact().getName(), scoutMessage,
                        detailsHtml(booking, venueName), "View booking", url)));

        if (booking.getCancelledByActorType() == com.eema.backend.entity.enums.ActorType.SCOUT) {
            String ownerMessage = "The scout cancelled this booking under policy tier "
                    + booking.getCancellationPolicyTier()
                    + ". Your cancellation settlement is provisional until provider fee reconciliation.";
            attempt("BOOKING_CANCELLED", "owner", bookingId, () -> emailService.send(
                    booking.getOwnerContact().getEmail(), "A scout cancelled an Eema booking",
                    text(booking.getOwnerContact().getName(), ownerMessage, details(booking, venueName),
                            "View booking", url),
                    html("Scout cancellation", booking.getOwnerContact().getName(), ownerMessage,
                            detailsHtml(booking, venueName), "View booking", url)));
        }
    }

    @Transactional(readOnly = true)
    public void sendCancellationRefundCompleted(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.CANCELLED
                || booking.getRefundStatus() != com.eema.backend.entity.enums.RefundStatus.REFUNDED) return;
        String venueName = venueRepository.findById(booking.getVenueId()).map(Venue::getName)
                .orElse("your selected venue");
        String message = "Your refund of €" + money(booking.getRefundAmountEur())
                + " has been processed. Bank settlement timing may vary.";
        String url = bookingUrl(bookingId);
        attempt("CANCELLATION_REFUND_COMPLETED", "scout", bookingId, () -> emailService.send(
                booking.getScoutContact().getEmail(), "Your Eema refund has been processed",
                text(booking.getScoutContact().getName(), message, details(booking, venueName),
                        "View booking", url),
                html("Refund processed", booking.getScoutContact().getName(), message,
                        detailsHtml(booking, venueName), "View booking", url)));
    }

    @Transactional(readOnly = true)
    public void sendLifecycle(UUID bookingId, BookingLifecycleEvent.Type type) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            log.warn("Booking lifecycle notification skipped: bookingId={}, notificationType={}", bookingId, type);
            return;
        }
        Venue venue = venueRepository.findById(booking.getVenueId()).orElse(null);
        String venueName = venue == null ? "your selected venue" : venue.getName();
        String timezone = venue == null ? "Venue local time" : venue.getTimezone();
        String url = bookingUrl(bookingId);
        BookingStatus expectedStatus = switch (type) {
            case REQUEST_SUBMITTED -> BookingStatus.REQUESTED;
            case REQUEST_ACCEPTED -> BookingStatus.AWAITING_PAYMENT;
            case REQUEST_DECLINED -> BookingStatus.DECLINED;
            case REQUEST_EXPIRED -> BookingStatus.EXPIRED;
            case REQUEST_BECAME_UNAVAILABLE -> BookingStatus.UNAVAILABLE;
            case APPROVAL_EXPIRED -> BookingStatus.EXPIRED;
        };
        if (booking.getStatus() != expectedStatus) {
            log.info("Stale booking notification skipped: bookingId={}, notificationType={}", bookingId, type);
            return;
        }
        switch (type) {
            case REQUEST_SUBMITTED -> sendRequestSubmitted(booking, venueName, timezone, url);
            case REQUEST_ACCEPTED -> sendRequestAccepted(booking, venueName, timezone, url);
            case REQUEST_DECLINED -> sendRequestDeclined(booking, venueName, timezone, url);
            case REQUEST_EXPIRED -> sendRequestExpired(booking, venueName, timezone, url);
            case REQUEST_BECAME_UNAVAILABLE -> sendRequestUnavailable(booking, venueName, timezone, url);
            case APPROVAL_EXPIRED -> sendApprovalExpired(booking, venueName, timezone, url);
        }
    }

    private void sendRequestSubmitted(Booking booking, String venue, String timezone, String url) {
        String bookingDetails = lifecycleDetails(booking, venue, timezone, "REQUESTED");
        String deadline = formatDeadline(booking.getOwnerResponseExpiresAt(), timezone);
        String acknowledgement = "Your request was sent to the venue owner. This is not yet a confirmed or paid booking. "
                + "The owner has until " + deadline + " to respond.";
        attempt("REQUEST_SUBMITTED", "scout", booking.getId(), () -> emailService.send(
                booking.getScoutContact().getEmail(), "Your request was sent",
                text(booking.getScoutContact().getName(), acknowledgement, bookingDetails, "View booking", url),
                html("Your request was sent", booking.getScoutContact().getName(), acknowledgement,
                        lifecycleDetailsHtml(booking, venue, timezone, "REQUESTED"), "View booking", url)));

        BookingContactSnapshot scout = booking.getScoutContact();
        String scoutLine = "Scout: %s (%s%s)".formatted(value(scout.getName()), value(scout.getEmail()),
                scout.getPhone() == null || scout.getPhone().isBlank() ? "" : ", " + scout.getPhone());
        String brief = "Brief: " + (booking.getRawBrief() == null || booking.getRawBrief().isBlank()
                ? "No brief provided" : booking.getRawBrief());
        String ownerMessage = "A scout submitted a new booking request. Please respond by " + deadline
                + ". The requested time is not reserved until a request is accepted.";
        String extraHtml = lifecycleDetailsHtml(booking, venue, timezone, "REQUESTED")
                + rowTable("Response deadline", deadline) + "<p>" + esc(scoutLine) + "</p><p>" + esc(brief) + "</p>";
        attempt("REQUEST_SUBMITTED", "owner", booking.getId(), () -> emailService.send(
                booking.getOwnerContact().getEmail(), "New booking request",
                text(booking.getOwnerContact().getName(), ownerMessage,
                        bookingDetails + "\nResponse deadline: " + deadline + "\n" + scoutLine + "\n" + brief, "Review booking", url),
                html("New booking request", booking.getOwnerContact().getName(), ownerMessage,
                        extraHtml, "Review booking", url)));
    }

    private void sendRequestAccepted(Booking booking, String venue, String timezone, String url) {
        String deadline = booking.getPaymentExpiresAt() == null ? "See booking for deadline"
                : booking.getPaymentExpiresAt().atZoneSameInstant(ZoneId.of(timezone))
                .format(DateTimeFormatter.ofPattern("d MMMM uuuu 'at' HH:mm z", Locale.ENGLISH));
        String message = "Your booking request was accepted. Pay before %s to secure the booking; checkout starts from the booking page."
                .formatted(deadline);
        deliverScout(booking, "REQUEST_ACCEPTED", "Your booking request was accepted", message,
                lifecycleDetails(booking, venue, timezone, "AWAITING_PAYMENT") + "\nPayment deadline: " + deadline,
                lifecycleDetailsHtml(booking, venue, timezone, "AWAITING_PAYMENT") + rowTable("Payment deadline", deadline),
                "Pay now", url);
    }

    private void sendRequestDeclined(Booking booking, String venue, String timezone, String url) {
        deliverScout(booking, "REQUEST_DECLINED", "Your booking request was declined",
                "The venue owner declined this booking request. No payment was taken.",
                lifecycleDetails(booking, venue, timezone, "DECLINED"),
                lifecycleDetailsHtml(booking, venue, timezone, "DECLINED"), "View booking", url);
    }

    private void sendApprovalExpired(Booking booking, String venue, String timezone, String url) {
        deliverScout(booking, "APPROVAL_EXPIRED", "Your payment window expired",
                "The booking was not confirmed and the slot was released. You may make a new request if it is still available.",
                lifecycleDetails(booking, venue, timezone, "EXPIRED"),
                lifecycleDetailsHtml(booking, venue, timezone, "EXPIRED"), "View booking", url);
    }

    private void sendRequestExpired(Booking booking, String venue, String timezone, String url) {
        deliverScout(booking, "REQUEST_EXPIRED", "Your booking request expired",
                "The owner did not respond before the request deadline. No booking was created or confirmed, and you may request the slot again if it is available.",
                lifecycleDetails(booking, venue, timezone, "EXPIRED"),
                lifecycleDetailsHtml(booking, venue, timezone, "EXPIRED"), "View availability", url);
    }

    private void sendRequestUnavailable(Booking booking, String venue, String timezone, String url) {
        deliverScout(booking, "REQUEST_BECAME_UNAVAILABLE", "The requested time is no longer available",
                "Another booking reserved this time. The owner did not explicitly decline your request; please choose another available time.",
                lifecycleDetails(booking, venue, timezone, "UNAVAILABLE"),
                lifecycleDetailsHtml(booking, venue, timezone, "UNAVAILABLE"), "View availability", url);
    }

    private String formatDeadline(java.time.OffsetDateTime deadline, String timezone) {
        return deadline == null ? "the deadline shown in the booking"
                : deadline.atZoneSameInstant(ZoneId.of(timezone))
                .format(DateTimeFormatter.ofPattern("d MMMM uuuu 'at' HH:mm z", Locale.ENGLISH));
    }

    private void deliverScout(Booking booking, String type, String subject, String message,
                              String details, String detailsHtml, String action, String url) {
        attempt(type, "scout", booking.getId(), () -> emailService.send(booking.getScoutContact().getEmail(), subject,
                text(booking.getScoutContact().getName(), message, details, action, url),
                html(subject, booking.getScoutContact().getName(), message, detailsHtml, action, url)));
    }

    private String lifecycleDetails(Booking b, String venue, String timezone, String status) {
        return "Venue: %s\nBooking reference: %s\nShoot date: %s\nTime: %s–%s (%s)\nStatus: %s"
                .formatted(venue, b.getId(), b.getShootDate().format(DATE), b.getStartTime().format(TIME),
                        b.getEndTime().format(TIME), timezone, status);
    }

    private String lifecycleDetailsHtml(Booking b, String venue, String timezone, String status) {
        return "<table role=\"presentation\" style=\"border-collapse:collapse;width:100%\">" +
                row("Venue", venue) + row("Booking reference", b.getId().toString()) +
                row("Shoot date", b.getShootDate().format(DATE)) +
                row("Time", b.getStartTime().format(TIME) + "–" + b.getEndTime().format(TIME) + " (" + timezone + ")") +
                row("Status", status) + "</table>";
    }

    private String rowTable(String label, String value) {
        return "<table role=\"presentation\" style=\"border-collapse:collapse;width:100%\">" + row(label, value) + "</table>";
    }

    private String text(String name, String message, String details, String action, String url) {
        return "Hello %s,\n\n%s\n\n%s\n\n%s: %s".formatted(value(name), message, details, action, url);
    }

    private void attempt(String notificationType, String recipientType, UUID bookingId, Runnable delivery) {
        try {
            delivery.run();
            log.info("Booking notification processed: bookingId={}, notificationType={}, recipientType={}",
                    bookingId, notificationType, recipientType);
        } catch (RuntimeException exception) {
            log.error("Booking notification failed: bookingId={}, notificationType={}, recipientType={}, errorType={}",
                    bookingId, notificationType, recipientType, exception.getClass().getSimpleName());
        }
    }

    private void sendScout(Booking b, String venue, String url) {
        BookingContactSnapshot scout = b.getScoutContact();
        String message = "Your payment has been received and your booking is confirmed.";
        String text = "Hello %s,\n\n%s\n\n%s\n\nView booking: %s"
                .formatted(value(scout.getName()), message, details(b, venue), url);
        emailService.send(scout.getEmail(), "Your Eema booking is confirmed", text,
                html("Your booking is confirmed", scout.getName(),
                        message, detailsHtml(b, venue), "View booking", url));
    }

    private void sendOwner(Booking b, String venue, String url) {
        BookingContactSnapshot owner = b.getOwnerContact();
        BookingContactSnapshot scout = b.getScoutContact();
        String scoutDetails = "Scout: %s (%s%s)".formatted(value(scout.getName()), value(scout.getEmail()),
                scout.getPhone() == null || scout.getPhone().isBlank() ? "" : ", " + scout.getPhone());
        String message = "The scout has paid and the booking is confirmed. No further acceptance is required.";
        String text = "Hello %s,\n\n%s\n\n%s\n%s\n\nView booking: %s"
                .formatted(value(owner.getName()), message, details(b, venue), scoutDetails, url);
        emailService.send(owner.getEmail(), "Confirmed booking for your Eema venue", text,
                html("Booking confirmed", owner.getName(), message,
                        detailsHtml(b, venue) + "<p>" + esc(scoutDetails) + "</p>", "View booking", url));
    }

    private String details(Booking b, String venue) {
        return "Venue: %s\nBooking reference: %s\nShoot date: %s\nTime: %s–%s\nAmount: €%s\nStatus: %s"
                .formatted(venue, b.getId(), b.getShootDate().format(DATE), b.getStartTime().format(TIME),
                        b.getEndTime().format(TIME), money(b.getTotalPaidEur()), b.getStatus());
    }

    private String detailsHtml(Booking b, String venue) {
        return "<table role=\"presentation\" style=\"border-collapse:collapse;width:100%\">" +
                row("Venue", venue) + row("Booking reference", b.getId().toString()) +
                row("Shoot date", b.getShootDate().format(DATE)) +
                row("Time", b.getStartTime().format(TIME) + "–" + b.getEndTime().format(TIME)) +
                row("Amount", "€" + money(b.getTotalPaidEur())) + row("Status", b.getStatus().name()) + "</table>";
    }

    private String html(String heading, String name, String intro, String details, String action, String url) {
        return """
                <!doctype html><html><body style="margin:0;background:#f5f5f5;font-family:Arial,sans-serif;color:#222">
                <div style="max-width:600px;margin:auto;background:white;padding:24px"><h1 style="font-size:24px">%s</h1>
                <p>Hello %s,</p><p>%s</p>%s
                <p style="margin-top:28px"><a href="%s" style="background:#222;color:white;padding:12px 18px;text-decoration:none;border-radius:4px">%s</a></p>
                <p>Best regards,<br>Eema Team</p></div></body></html>
                """.formatted(esc(heading), esc(value(name)), esc(intro), details, esc(url), esc(action));
    }

    private String row(String label, String value) {
        return "<tr><th style=\"text-align:left;padding:6px\">" + esc(label) + "</th><td style=\"padding:6px\">" + esc(value) + "</td></tr>";
    }

    private String bookingUrl(UUID id) {
        String base = properties.frontendBaseUrl();
        if (base == null || base.isBlank()) throw new IllegalStateException("APP_FRONTEND_BASE_URL is missing");
        return base.replaceAll("/+$", "") + "/bookings/" + id;
    }

    private String money(BigDecimal amount) { return amount.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private String value(String value) { return value == null || value.isBlank() ? "there" : value; }
    private String esc(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
}
