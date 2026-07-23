package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.BookingEvent;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.BookingEventType;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.BookingEventRepository;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.repository.VenueRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Service for handling Mollie webhooks.
 */
@Service
@Slf4j
public class WebhookService {

    private static final String MOLLIE_API_BASE = "https://api.mollie.com/v2";
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final BookingRepository bookingRepository;
    private final BookingEventRepository bookingEventRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    public WebhookService(RestTemplate restController,
                          AppProperties appProperties,
                          BookingRepository bookingRepository,
                          BookingEventRepository bookingEventRepository,
                          UserRepository userRepository,
                          VenueRepository venueRepository,
                          ObjectMapper objectMapper,
                          EmailService emailService) {
        this.restTemplate = restController;
        this.appProperties = appProperties;
        this.bookingRepository = bookingRepository;
        this.bookingEventRepository = bookingEventRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository;
        this.objectMapper = objectMapper;
        this.emailService = emailService;
    }

    /**
     * Main entry point for processing a Mollie webhook.
     *
     * @param rawBody   the raw request body
     * @param signature the value of the X-Mollie-Signature header
     */
    public void handleWebhook(String rawBody, String signature) {
        if (!verifySignature(rawBody, signature)) {
            log.warn("Invalid Mollie webhook signature");
            throw new InvalidSignatureException("Invalid webhook signature");
        }

        String paymentId;
        try {
            paymentId = extractPaymentId(rawBody);
        } catch (IllegalArgumentException e) {
            log.warn("Could not extract payment ID from webhook body: {}", e.getMessage());
            // Do not proceed further - invalid payload
            return;
        }

        if (ObjectUtils.isEmpty(paymentId)) {
            log.warn("Empty payment ID in webhook");
            return;
        }

        com.eema.backend.dto.response.MolliePaymentResponse payment;
        try {
            payment = fetchPaymentFromMollie(paymentId);
        } catch (RestClientException e) {
            log.error("Failed to fetch payment {} from Mollie", paymentId, e);
            // Let Mollie retry by throwing an exception
            throw e;
        }

        if (!"paid".equals(payment.status())) {
            log.info("Payment {} is not paid (status: {}) - ignoring", paymentId, payment.status());
            return;
        }

        // Extract booking ID from metadata (trusted because we got it from Mollie API)
        String bookingIdStr = payment.metadata().get("bookingId");
        if (org.springframework.util.ObjectUtils.isEmpty(bookingIdStr)) {
            log.warn("Payment {} has no bookingId in metadata", paymentId);
            return;
        }

        java.util.UUID bookingId;
        try {
            bookingId = java.util.UUID.fromString(bookingIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Payment {} has invalid bookingId: {}", paymentId, bookingIdStr);
            return;
        }

        // Process the booking update in a transaction
        bookingRepository.findById(bookingId).ifPresentOrElse(
                this::processPaidBooking,
                () -> log.warn("Booking not found for id: {} (from payment {})", bookingId, paymentId)
        );
    }

    /**
     * Process a booking that has been paid according to Mollie.
     * This method is transactional.
     *
     * @param booking the booking to update
     * @param payment the payment details from Mollie
     */
    @Transactional
    public void processPaidBooking(final com.eema.backend.entity.Booking booking, final com.eema.backend.dto.response.MolliePaymentResponse payment) {
        // Idempotency check: if already paid, do nothing
        if (booking.getStatus() == com.eema.backend.entity.enums.BookingStatus.PAID) {
            log.info("Booking {} is already paid - skipping", booking.getId());
            return;
        }

        // Update financial fields from Mollie payment
        // Amount charged to customer (should include venue price + service fee)
        BigDecimal amountPaid = new BigDecimal(payment.amount().value());
        booking.setTotalPaidEur(amountPaid);

        // For now, we'll set mollie fee to 0 and calculate based on our known fee structure
        // In a more advanced implementation, we could extract the actual fee from Mollie webhook data
        // or from the payment details if Mollie provides it
        booking.setMollieFeeEur(BigDecimal.ZERO);

        // Recalculate commission and payout based on the venue price (not the total paid)
        // This assumes the venue price is the base amount before our service fee
        BigDecimal venuePriceEur = booking.getVenuePriceEur(); // This was set when booking was created
        BigDecimal commissionPercentage = booking.getCommissionPercentage(); // This was set when booking was created
        BigDecimal commissionEur = venuePriceEur.multiply(commissionPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        booking.setCommissionEur(commissionEur);

        // Owner payout = venue price - commission - mollie fee
        BigDecimal ownerPayoutEur = venuePriceEur.subtract(commissionEur).subtract(booking.getMollieFeeEur());
        booking.setOwnerPayoutEur(ownerPayoutEur);

        // Update booking
        booking.setStatus(com.eema.backend.entity.enums.BookingStatus.PAID);
        booking.setPaidAt(java.time.OffsetDateTime.now());
        bookingRepository.save(booking);
        log.info("Booking {} marked as PAID", booking.getId());

        // Create booking event
        com.eema.backend.entity.BookingEvent event = com.eema.backend.entity.BookingEvent.builder()
                .bookingId(booking.getId())
                .eventType(com.eema.backend.entity.enums.BookingEventType.PAYMENT_SUCCEEDED)
                .build();
        bookingEventRepository.save(event);
        log.info("Created PAYMENT_SUCCEEDED event for booking {}", booking.getId());

        // Extract data needed for email sending
        User scout = userRepository.findById(booking.getScoutId())
                .orElseThrow(() -> new IllegalStateException("Scout not found for booking " + booking.getId()));
        String scoutEmail = scout.getEmail();

        Venue venue = venueRepository.findById(booking.getVenueId())
                .orElseThrow(() -> new IllegalStateException("Venue not found for booking " + booking.getId()));
        String venueName = venue.getName();
        String venueAddress = venue.getNeighbourhood(); // Use neighbourhood as address
        String venueDetails = String.format("%s, %s", venueName, venueAddress);

        final String ownerEmail = venue.getOwner().getEmail();
        final String ownerName = venue.getOwner().getFullName();
        final String ownerContact = (venue.getOwner().getPhone() != null && !venue.getOwner().getPhone().isBlank())
                ? ownerName + " (" + venue.getOwner().getPhone() + ")"
                                : ownerName;

        String calendlyLink = appProperties.getCalendly().getBaseUrl() + "/" + booking.getId();

        String scoutContact = booking.getScoutContact(); // JSON string from booking
        String confirmationDeadline = LocalDateTime.now().plusHours(24).toString(); // 24 hours from now

        // Register transaction synchronization to send emails after commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendBookingConfirmationToScout(scoutEmail, venueDetails, ownerContact, calendlyLink);
                    emailService.sendBookingNotificationToOwner(ownerEmail, scoutContact, confirmationDeadline);
                    log.info("Sent payment confirmation emails for booking {}", booking.getId());
                } catch (Exception e) {
                    log.error("Failed to send payment confirmation emails after commit for booking {}", booking.getId(), e);
                    // Do not rethrow - transaction is already committed
                }
            }
        });
    }

    /**
     * Verify the Mollie webhook HMAC-SHA256 signature.
     *
     * @param rawBody   the raw request body
     * @param signature the value of the X-Mollie-Signature header (hex encoded)
     * @return true if signature is valid
     */
    private boolean verifySignature(String rawBody, String signatureHeader) {
        try {
            String webhookSecret = appProperties.getMollie().getWebhookSecret();
            byte[] secretBytes = webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(rawBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Convert the signature header (hex string) to byte array
            byte[] providedSigBytes = hexStringToByteArray(signatureHeader);
            return MessageDigest.isEqual(hmacBytes, providedSigBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error during webhook signature verification", e);
            return false;
        }
    }

    /**
     * Extract the payment ID from the webhook JSON body.
     * We only trust this ID to fetch the payment from Mollie - we do not trust any other fields.
     *
     * @param rawBody the raw request body
     * @return the payment ID
     * @throws IllegalArgumentException if the JSON is invalid or missing the "id" field
     */
    private String extractPaymentId(String rawBody) {
        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(rawBody);
            com.fasterxml.jackson.databind.JsonNode idNode = rootNode.get("id");
            if (idNode == null || idNode.isNull()) {
                throw new IllegalArgumentException("Missing 'id' field in webhook payload");
            }
            return idNode.asText();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON in webhook body", e);
        }
    }

    /**
     * Fetch the payment from Mollie API using the payment ID.
     *
     * @param paymentId the Mollie payment ID
     * @return the payment response from Mollie
     */
    private com.eema.backend.dto.response.MolliePaymentResponse fetchPaymentFromMollie(String paymentId) {
        String url = MOLLIE_API_BASE + "/payments/" + paymentId;

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer " + appProperties.getMollie().getApiKey());
        headers.set("Accept", "application/json");

        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

        org.springframework.http.ResponseEntity<com.eema.backend.dto.response.MolliePaymentResponse> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                com.eema.backend.dto.response.MolliePaymentResponse.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("Mollie returned empty response for payment " + paymentId);
        }
        return response.getBody();
    }

    /**
     * Converts a hex string to a byte array.
     *
     * @param s the hex string
     * @return byte array
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Exception thrown when webhook signature is invalid.
     */
    public static class InvalidSignatureException extends RuntimeException {
        public InvalidSignatureException(String message) {
            super(message);
        }
    }
}