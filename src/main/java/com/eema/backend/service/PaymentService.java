package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.request.CheckoutRequest;
import com.eema.backend.dto.response.CheckoutResponse;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private static final String MOLLIE_API_BASE = "https://api.mollie.com/v2";
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public PaymentService(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    /**
     * Create a Mollie payment order for a booking.
     * 
     * Steps:
     * 1. Calculate total amount (dayRate + serviceFee)
     * 2. Call Mollie API to create payment
     * 3. Store molliePaymentId on the booking
     * 4. Return CheckoutResponse with Mollie redirect URL
     * 
     * @param bookingId The booking UUID (already created in PENDING state)
     * @param amount    Total EUR amount to charge
     * @return CheckoutResponse with checkoutUrl (Mollie payment page)
     * @throws BadRequestException if Mollie API call fails
     */
    public CheckoutResponse createPayment(UUID bookingId, BigDecimal amount) {
        log.info("Creating Mollie payment for booking {}. Amount: €{}", bookingId, amount);
        
        try {
            // Call Mollie API
            MolliePaymentResponse mollieResponse = callMollieCreatePayment(bookingId, amount);
            
            // Extract checkout URL
            String checkoutUrl = mollieResponse.links().checkout().href();
            
            log.info("Mollie payment created. Payment ID: {}. Redirect URL: {}", 
                mollieResponse.id(), checkoutUrl);
            
            // Return checkout response
            return new CheckoutResponse(
                bookingId,
                checkoutUrl,
                mollieResponse.id(),
                amount.doubleValue(),
                300L  // expires in 5 minutes (300 seconds)
            );
        } catch (RestClientException e) {
            log.error("Mollie API call failed", e);
            throw new BadRequestException("Could not initiate payment. Please try again.");
        }
    }

    /**
     * Call the Mollie Create Payment API.
     * 
     * POST https://api.mollie.com/v2/payments
     * 
     * Request body:
     * {
     *   "amount": { "value": "100.00", "currency": "EUR" },
     *   "description": "Eema booking #{bookingId}",
     *   "redirectUrl": "https://eema.co/booked",
     *   "webhookUrl": "https://eema.co/api/webhook",
     *   "method": "ideal"  (or null to allow all methods)
     * }
     */
    private MolliePaymentResponse callMollieCreatePayment(UUID bookingId, BigDecimal amount) {
        String url = MOLLIE_API_BASE + "/payments";
        
        // Build request body
        Map<String, Object> body = new HashMap<>();
        
        // Amount in Mollie format
        Map<String, String> amountObj = new HashMap<>();
        amountObj.put("value", String.format("%.2f", amount));
        amountObj.put("currency", "EUR");
        body.put("amount", amountObj);
        
        // Description
        body.put("description", "Eema booking #" + bookingId);
        
        // Redirect after payment
        body.put("redirectUrl", appProperties.getMollie().getRedirectUrl());
        
        // Webhook for payment confirmation (will be POST /api/webhook)
        String webhookUrl = appProperties.getMollie().getRedirectUrl()
            .replaceAll("/booked$", "/api/webhook");
        body.put("webhookUrl", webhookUrl);
        
        // Metadata (optional but useful for tracking)
        Map<String, String> metadata = new HashMap<>();
        metadata.put("bookingId", bookingId.toString());
        body.put("metadata", metadata);
        
        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + appProperties.getMollie().getApiKey());
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        // Make the call
        ResponseEntity<MolliePaymentResponse> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            MolliePaymentResponse.class
        );
        
        if (response.getBody() == null) {
            throw new IllegalStateException("Mollie returned empty response");
        }
        
        return response.getBody();
    }

    /**
     * Calculate the total amount to charge (dayRate + serviceFee).
     * This mirrors the backend fee calculation.
     * 
     * Formula:
     *   serviceFee = max(€5.00, min(€20.00, dayRate × 4%))
     *   total = dayRate + serviceFee
     */
    public static BigDecimal calculateTotal(BigDecimal dayRate) {
        // Calculate service fee: max(5.00, min(20.00, dayRate * 0.04))
        BigDecimal minFee = BigDecimal.valueOf(5.00);
        BigDecimal maxFee = BigDecimal.valueOf(20.00);
        BigDecimal percentageFee = dayRate.multiply(BigDecimal.valueOf(0.04));
        
        BigDecimal serviceFee = percentageFee.max(minFee).min(maxFee);
        
        return dayRate.add(serviceFee);
    }
}
