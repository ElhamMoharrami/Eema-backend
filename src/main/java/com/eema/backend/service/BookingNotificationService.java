package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.BookingContactSnapshot;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.BookingStatus;
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
        if (booking == null || (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.CONFIRMED)) {
            log.warn("Payment email skipped: booking missing or no longer paid, bookingId={}", bookingId);
            return;
        }
        String venueName = venueRepository.findById(booking.getVenueId()).map(Venue::getName).orElse("your selected venue");
        String url = bookingUrl(bookingId);
        attempt("scout", bookingId, () -> sendScout(booking, venueName, url));
        attempt("owner", bookingId, () -> sendOwner(booking, venueName, url));
    }

    private void attempt(String recipientType, UUID bookingId, Runnable delivery) {
        try {
            delivery.run();
            log.info("Payment notification processed: bookingId={}, recipientType={}", bookingId, recipientType);
        } catch (RuntimeException exception) {
            log.error("Payment notification failed: bookingId={}, recipientType={}, errorType={}",
                    bookingId, recipientType, exception.getClass().getSimpleName());
        }
    }

    private void sendScout(Booking b, String venue, String url) {
        BookingContactSnapshot scout = b.getScoutContact();
        String text = "Hello %s,\n\nYour payment has been received. Your booking is now paid and may still be awaiting venue confirmation.\n\n%s\n\nView booking: %s"
                .formatted(value(scout.getName()), details(b, venue), url);
        emailService.send(scout.getEmail(), "Payment received for your Eema booking", text,
                html("Your payment has been received", scout.getName(),
                        "Your booking is now paid and may still be awaiting venue confirmation.", detailsHtml(b, venue), "View booking", url));
    }

    private void sendOwner(Booking b, String venue, String url) {
        BookingContactSnapshot owner = b.getOwnerContact();
        BookingContactSnapshot scout = b.getScoutContact();
        String scoutDetails = "Scout: %s (%s%s)".formatted(value(scout.getName()), value(scout.getEmail()),
                scout.getPhone() == null || scout.getPhone().isBlank() ? "" : ", " + scout.getPhone());
        String text = "Hello %s,\n\nA new paid booking requires your review.\n\n%s\n%s\n\nReview booking: %s"
                .formatted(value(owner.getName()), details(b, venue), scoutDetails, url);
        emailService.send(owner.getEmail(), "New paid booking for your Eema venue", text,
                html("New paid booking", owner.getName(), "A new paid booking requires your review.",
                        detailsHtml(b, venue) + "<p>" + esc(scoutDetails) + "</p>", "Review booking", url));
    }

    private String details(Booking b, String venue) {
        return "Venue: %s\nBooking reference: %s\nShoot date: %s\nTime: %s–%s\nAmount: €%s\nStatus: PAID"
                .formatted(venue, b.getId(), b.getShootDate().format(DATE), b.getStartTime().format(TIME),
                        b.getEndTime().format(TIME), money(b.getTotalPaidEur()));
    }

    private String detailsHtml(Booking b, String venue) {
        return "<table role=\"presentation\" style=\"border-collapse:collapse;width:100%\">" +
                row("Venue", venue) + row("Booking reference", b.getId().toString()) +
                row("Shoot date", b.getShootDate().format(DATE)) +
                row("Time", b.getStartTime().format(TIME) + "–" + b.getEndTime().format(TIME)) +
                row("Amount", "€" + money(b.getTotalPaidEur())) + row("Status", "PAID") + "</table>";
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
