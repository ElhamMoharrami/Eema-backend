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

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final BookingService bookingService;

    public void handlePaymentWebhook(String paymentId) {

        if (paymentId == null || paymentId.isBlank() || !paymentId.startsWith("tr_")) {

            throw new IllegalArgumentException("Invalid Mollie payment ID");
        }

        MolliePaymentResponse payment = fetchPayment(paymentId);

        log.info("Mollie payment fetched: paymentId={}, status={}", payment.id(), payment.status());

        if (!"paid".equalsIgnoreCase(payment.status())) {

            /*
             * You can later support failed/canceled/expired
             * statuses here too.
             */
            log.info("Payment {} currently has status {}; no PAID transition needed", payment.id(), payment.status());

            return;
        }

        bookingService.processPaidPayment(payment);
    }

    private MolliePaymentResponse fetchPayment(String paymentId) {

        String apiKey = appProperties.mollie().apiKey();

        if (apiKey == null || apiKey.isBlank()) {

            throw new IllegalStateException("Mollie API key is not configured");
        }

        String url = MOLLIE_API_BASE + "/payments/" + paymentId;

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(apiKey);

        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<MolliePaymentResponse> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Void>(headers), MolliePaymentResponse.class);

        if (response.getBody() == null) {
            throw new IllegalStateException("Mollie returned an empty payment response");
        }

        return response.getBody();
    }
}