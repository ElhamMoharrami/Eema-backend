package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.dto.response.MollieRefundResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HttpMollieProviderClient implements MollieProviderClient {
    private static final String API = "https://api.mollie.com/v2";
    private final RestTemplate restTemplate;
    private final AppProperties properties;

    @Override
    public MolliePaymentResponse createPayment(UUID bookingId, BigDecimal amount, UUID key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount(amount));
        body.put("description", "Eema booking #" + bookingId);
        body.put("redirectUrl", required(properties.mollie().redirectUrl(), "Mollie redirect URL"));
        if (properties.mollie().webhookUrl() != null && !properties.mollie().webhookUrl().isBlank()) {
            body.put("webhookUrl", properties.mollie().webhookUrl());
        }
        body.put("metadata", Map.of("bookingId", bookingId.toString()));
        return exchange(API + "/payments", HttpMethod.POST, body, key, MolliePaymentResponse.class);
    }

    @Override
    public MolliePaymentResponse getPayment(String paymentId) {
        return exchange(API + "/payments/" + paymentId, HttpMethod.GET, null, null, MolliePaymentResponse.class);
    }

    @Override
    public MollieRefundResponse createFullRefund(String paymentId, UUID bookingId, BigDecimal amount, UUID key) {
        Map<String, Object> body = Map.of(
                "amount", amount(amount),
                "description", "Full refund for Eema booking #" + bookingId,
                "metadata", Map.of("bookingId", bookingId.toString()));
        return exchange(API + "/payments/" + paymentId + "/refunds", HttpMethod.POST, body, key, MollieRefundResponse.class);
    }

    @Override
    public MollieRefundResponse getRefund(String paymentId, String refundId) {
        return exchange(API + "/payments/" + paymentId + "/refunds/" + refundId,
                HttpMethod.GET, null, null, MollieRefundResponse.class);
    }

    private Map<String, String> amount(BigDecimal value) {
        return Map.of("value", value.toPlainString(), "currency", "EUR");
    }

    private <T> T exchange(String url, HttpMethod method, Object body, UUID key, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(required(properties.mollie().apiKey(), "Mollie API key"));
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        if (key != null) headers.set("Idempotency-Key", key.toString());
        ResponseEntity<T> response = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), type);
        if (response.getBody() == null) throw new IllegalStateException("Mollie returned an empty response");
        return response.getBody();
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is not configured");
        return value;
    }
}
