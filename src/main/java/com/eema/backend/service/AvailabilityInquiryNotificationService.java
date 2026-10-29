package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.AvailabilityInquiry;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.event.AvailabilityInquiryLifecycleEvent;
import com.eema.backend.repository.AvailabilityInquiryRepository;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityInquiryNotificationService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("d MMMM uuuu 'at' HH:mm z", Locale.ENGLISH);

    private final AvailabilityInquiryRepository inquiries;
    private final VenueRepository venues;
    private final UserRepository users;
    private final EmailService emailService;
    private final AppProperties properties;

    @Transactional(readOnly = true)
    public void sendLifecycle(UUID inquiryId, AvailabilityInquiryLifecycleEvent.Type type) {
        AvailabilityInquiry inquiry = inquiries.findById(inquiryId).orElse(null);
        if (inquiry == null) {
            log.warn("Inquiry notification skipped: inquiryId={}, notificationType={}", inquiryId, type);
            return;
        }
        Venue venue = venues.findById(inquiry.getVenueId()).orElse(null);
        User scout = users.findById(inquiry.getScoutId()).orElse(null);
        if (venue == null || scout == null) {
            log.warn("Inquiry notification skipped: inquiryId={}, notificationType={}, missingReference=true", inquiryId, type);
            return;
        }
        com.eema.backend.entity.enums.AvailabilityInquiryStatus expectedStatus = switch (type) {
            case CREATED -> com.eema.backend.entity.enums.AvailabilityInquiryStatus.OPEN;
            case OFFERED -> com.eema.backend.entity.enums.AvailabilityInquiryStatus.OFFERED;
            case DECLINED -> com.eema.backend.entity.enums.AvailabilityInquiryStatus.DECLINED;
            case OFFER_EXPIRED -> com.eema.backend.entity.enums.AvailabilityInquiryStatus.EXPIRED;
        };
        if (inquiry.getStatus() != expectedStatus) {
            log.info("Stale inquiry notification skipped: inquiryId={}, notificationType={}", inquiryId, type);
            return;
        }
        switch (type) {
            case CREATED -> notifyOwner(inquiry, venue, scout);
            case OFFERED -> notifyScoutOffer(inquiry, venue, scout);
            case DECLINED -> notifyScout(inquiry, venue, scout, "INQUIRY_DECLINED",
                    "Your availability inquiry was declined",
                    "The venue owner declined your availability inquiry.", requested(inquiry, venue),
                    requestedHtml(inquiry, venue), "View inquiries", scoutUrl());
            case OFFER_EXPIRED -> notifyScout(inquiry, venue, scout, "INQUIRY_OFFER_EXPIRED",
                    "Your availability offer expired",
                    "The one-off availability offer expired and can no longer be accepted.", offered(inquiry, venue),
                    offeredHtml(inquiry, venue), "View inquiries", scoutUrl());
        }
    }

    private void notifyOwner(AvailabilityInquiry inquiry, Venue venue, User scout) {
        User owner = venue.getOwner();
        if (owner == null) {
            log.warn("Inquiry notification skipped: inquiryId={}, notificationType=INQUIRY_CREATED, missingOwner=true", inquiry.getId());
            return;
        }
        String scoutLine = "Scout: %s (%s%s)".formatted(name(scout), scout.getEmail(),
                scout.getPhone() == null || scout.getPhone().isBlank() ? "" : ", " + scout.getPhone());
        String details = requested(inquiry, venue) + "\n" + scoutLine + "\nMessage: " + inquiry.getMessage();
        String detailsHtml = requestedHtml(inquiry, venue) + "<p>" + esc(scoutLine)
                + "</p><p><strong>Message:</strong> " + esc(inquiry.getMessage()) + "</p>";
        attempt(inquiry.getId(), "INQUIRY_CREATED", "owner", () -> emailService.send(owner.getEmail(),
                "New availability inquiry",
                text(name(owner), "A scout asked about a time outside the venue's current availability.",
                        details, "Review inquiries", ownerUrl()),
                html("New availability inquiry", name(owner),
                        "A scout asked about a time outside the venue's current availability.",
                        detailsHtml, "Review inquiries", ownerUrl())));
    }

    private void notifyScoutOffer(AvailabilityInquiry inquiry, Venue venue, User scout) {
        String expiry = inquiry.getOfferExpiresAt().atZoneSameInstant(ZoneId.of(venue.getTimezone())).format(DEADLINE);
        String details = offered(inquiry, venue) + "\nOffer expires: " + expiry
                + "\nOriginal request: " + interval(inquiry.getRequestedDate(), inquiry.getRequestedStartTime(), inquiry.getRequestedEndTime());
        String detailsHtml = offeredHtml(inquiry, venue) + "<table role=\"presentation\" style=\"border-collapse:collapse;width:100%\">"
                + tableRow("Offer expires", expiry)
                + tableRow("Original request", interval(inquiry.getRequestedDate(), inquiry.getRequestedStartTime(), inquiry.getRequestedEndTime()))
                + "</table>";
        notifyScout(inquiry, venue, scout, "INQUIRY_OFFERED", "The owner offered you a time",
                "The venue owner offered a one-off time. Review and accept it before the deadline if it works for you.",
                details, detailsHtml, "Review offer", scoutUrl());
    }

    private void notifyScout(AvailabilityInquiry inquiry, Venue venue, User scout, String type, String subject,
                             String message, String details, String detailsHtml, String action, String url) {
        attempt(inquiry.getId(), type, "scout", () -> emailService.send(scout.getEmail(), subject,
                text(name(scout), message, details, action, url),
                html(subject, name(scout), message, detailsHtml, action, url)));
    }

    private String requested(AvailabilityInquiry i, Venue v) {
        return "Venue: %s\nInquiry reference: %s\nRequested: %s\nTimezone: %s\nStatus: %s"
                .formatted(v.getName(), i.getId(), interval(i.getRequestedDate(), i.getRequestedStartTime(), i.getRequestedEndTime()),
                        v.getTimezone(), i.getStatus());
    }

    private String offered(AvailabilityInquiry i, Venue v) {
        return "Venue: %s\nInquiry reference: %s\nOffered: %s\nTimezone: %s\nStatus: %s"
                .formatted(v.getName(), i.getId(), interval(i.getOfferedDate(), i.getOfferedStartTime(), i.getOfferedEndTime()),
                        v.getTimezone(), i.getStatus());
    }

    private String requestedHtml(AvailabilityInquiry i, Venue v) {
        return table(v.getName(), i.getId(), "Requested",
                interval(i.getRequestedDate(), i.getRequestedStartTime(), i.getRequestedEndTime()), v.getTimezone(), i.getStatus().name());
    }

    private String offeredHtml(AvailabilityInquiry i, Venue v) {
        return table(v.getName(), i.getId(), "Offered",
                interval(i.getOfferedDate(), i.getOfferedStartTime(), i.getOfferedEndTime()), v.getTimezone(), i.getStatus().name());
    }

    private String table(String venue, UUID id, String intervalLabel, String interval, String timezone, String status) {
        return "<table role=\"presentation\" style=\"border-collapse:collapse;width:100%\">"
                + tableRow("Venue", venue) + tableRow("Inquiry reference", id.toString())
                + tableRow(intervalLabel, interval) + tableRow("Timezone", timezone)
                + tableRow("Status", status) + "</table>";
    }

    private String tableRow(String label, String value) {
        return "<tr><th style=\"text-align:left;padding:6px\">" + esc(label)
                + "</th><td style=\"padding:6px\">" + esc(value) + "</td></tr>";
    }

    private String interval(java.time.LocalDate date, java.time.LocalTime start, java.time.LocalTime end) {
        return date.format(DATE) + " · " + start.format(TIME) + "–" + end.format(TIME);
    }

    private String text(String name, String message, String details, String action, String url) {
        return "Hello %s,\n\n%s\n\n%s\n\n%s: %s".formatted(name, message, details, action, url);
    }

    private String html(String heading, String name, String intro, String details, String action, String url) {
        return """
                <!doctype html><html><body style="margin:0;background:#f5f5f5;font-family:Arial,sans-serif;color:#222">
                <div style="max-width:600px;margin:auto;background:white;padding:24px"><h1 style="font-size:24px">%s</h1>
                <p>Hello %s,</p><p>%s</p>%s
                <p style="margin-top:28px"><a href="%s" style="background:#222;color:white;padding:12px 18px;text-decoration:none;border-radius:4px">%s</a></p>
                <p>Best regards,<br>Eema Team</p></div></body></html>
                """.formatted(esc(heading), esc(name), esc(intro), details, esc(url), esc(action));
    }

    private void attempt(UUID inquiryId, String type, String recipientType, Runnable delivery) {
        try {
            delivery.run();
            log.info("Inquiry notification processed: inquiryId={}, notificationType={}, recipientType={}",
                    inquiryId, type, recipientType);
        } catch (RuntimeException exception) {
            log.error("Inquiry notification failed: inquiryId={}, notificationType={}, recipientType={}, errorType={}",
                    inquiryId, type, recipientType, exception.getClass().getSimpleName());
        }
    }

    private String baseUrl() {
        if (properties.frontendBaseUrl() == null || properties.frontendBaseUrl().isBlank())
            throw new IllegalStateException("APP_FRONTEND_BASE_URL is missing");
        return properties.frontendBaseUrl().replaceAll("/+$", "");
    }

    private String ownerUrl() { return baseUrl() + "/owner/inquiries"; }
    private String scoutUrl() { return baseUrl() + "/scout/inquiries"; }
    private String name(User user) { return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName(); }
    private String esc(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }
}
