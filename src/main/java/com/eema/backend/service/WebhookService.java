package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.response.MolliePaymentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final String MOLLIE_API_BASE = "https://api.mollie.com/v2";

    private final MollieProviderClient mollieProviderClient;
    private final BookingService bookingService;
    private final RefundService refundService;

    public void handlePaymentWebhook(String paymentId) {

        if (!MollieResponseValidation.paymentId(paymentId)) {

            throw new IllegalArgumentException("Invalid Mollie payment ID");
        }

        MolliePaymentResponse payment = mollieProviderClient.getPayment(paymentId);

        if (payment == null || !paymentId.equals(payment.id()) || payment.status() == null || payment.status().isBlank()) {
            throw new IllegalStateException("Mollie payment retrieval did not match the callback");
        }

        log.info("Mollie payment fetched: paymentId={}, status={}", payment.id(), payment.status());

        if ("paid".equalsIgnoreCase(payment.status())) {
            BookingService.PaidPaymentOutcome outcome = bookingService.processPaidPayment(payment);
            if (outcome.result() == BookingService.PaidPaymentResult.LATE_PAYMENT) {
                refundService.requestLatePaymentRefund(outcome.bookingId());
            }
        }

        if (!"paid".equalsIgnoreCase(payment.status())) {
            log.info("Payment {} currently has status {}; no booking transition needed", payment.id(), payment.status());
        }
        refundService.reconcileForPayment(payment.id());
    }
}
