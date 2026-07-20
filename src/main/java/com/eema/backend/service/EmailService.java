package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.resend.core.exception.ResendException;
import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import java.util.Collections;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final Resend resendClient;
    private final String fromEmail;

    /**
     * Constructor used in production.
     */
    public EmailService(AppProperties appProperties) {
        String apiKey = appProperties.getResend().getApiKey();
        this.resendClient = new Resend(apiKey);
        this.fromEmail = appProperties.getResend().getFromEmail();
    }

    /**
     * Constructor for testing, allows injecting a mock Resend client.
     */
    EmailService(AppProperties appProperties, Resend resendClient) {
        this.fromEmail = appProperties.getResend().getFromEmail();
        this.resendClient = resendClient;
    }

    /**
     * Sends booking confirmation email to scout.
     *
     * @param scoutEmail    the scout's email address
     * @param venueDetails  details of the venue (name, address, etc.)
     * @param ownerContact  contact information of the venue owner
     * @param calendlyLink  link to schedule a call via Calendly
     * @throws ResendException if sending fails
     */
    public void sendBookingConfirmationToScout(String scoutEmail, String venueDetails, String ownerContact, String calendlyLink) throws ResendException {
        SendEmailRequest request = SendEmailRequest.builder()
                .from(fromEmail)
                .to(Collections.singletonList(scoutEmail))
                .subject("Your Eema booking is confirmed!")
                .html(buildScoutEmailHtml(venueDetails, ownerContact, calendlyLink))
                .build();

        SendEmailResponse response = resendClient.emails().send(request);
        log.info("Booking confirmation email sent to scout: {}, emailId: {}", scoutEmail, response.getId());
    }

    /**
     * Sends booking notification email to venue owner.
     *
     * @param ownerEmail           the venue owner's email address
     * @param scoutContact         contact information of the scout
     * @param confirmationDeadline deadline for the owner to confirm (e.g., timestamp)
     * @throws ResendException if sending fails
     */
    public void sendBookingNotificationToOwner(String ownerEmail, String scoutContact, String confirmationDeadline) throws ResendException {
        SendEmailRequest request = SendEmailRequest.builder()
                .from(fromEmail)
                .to(Collections.singletonList(ownerEmail))
                .subject("New booking request for your venue")
                .html(buildOwnerEmailHtml(scoutContact, confirmationDeadline))
                .build();

        SendEmailResponse response = resendClient.emails().send(request);
        log.info("Booking notification email sent to owner: {}, emailId: {}", ownerEmail, response.getId());
    }

    private String buildScoutEmailHtml(String venueDetails, String ownerContact, String calendlyLink) {
        return """
                <h2>Your Eema booking is confirmed!</h2>
                <p>Hello,</p>
                <p>Your booking for the following venue has been confirmed:</p>
                <p><strong>Venue Details:</strong> %s</p>
                <p><strong>Owner Contact:</strong> %s</p>
                <p>You can schedule a pre-shoot call via Calendly: <a href="%s">%s</a></p>
                <p>Best regards,<br/>Eema Team</p>
                """.formatted(venueDetails, ownerContact, calendlyLink, calendlyLink);
    }

    private String buildOwnerEmailHtml(String scoutContact, String confirmationDeadline) {
        return """
                <h2>New booking request for your venue</h2>
                <p>Hello,</p>
                <p>You have a new booking request from a scout with the following contact information:</p>
                <p><strong>Scout Contact:</strong> %s</p>
                <p>Please confirm this booking within 24 hours (by %s).</p>
                <p>Best regards,<br/>Eema Team</p>
                """.formatted(scoutContact, confirmationDeadline);
    }

    /**
     * Legacy method for compatibility. Does nothing.
     *
     * @param bookingId the booking ID
     */
    public void sendPaymentConfirmationEmail(UUID bookingId) {
        // Do nothing - kept for compatibility
    }
}