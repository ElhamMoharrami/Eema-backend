package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.response.MolliePaymentResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpMollieProviderClientTest {

    @Test
    void omitsWebhookUrlForManualLocalWebhookMode() {
        Map<String, Object> body = createPaymentAndCaptureBody("");

        assertThat(body).doesNotContainKey("webhookUrl");
        assertThat(body).containsEntry("redirectUrl", "http://localhost:3000/booked");
    }

    @Test
    void sendsWebhookUrlWhenPublicTunnelIsConfigured() {
        Map<String, Object> body = createPaymentAndCaptureBody(
                "https://tunnel.example/api/webhooks/mollie/payment");

        assertThat(body).containsEntry("webhookUrl", "https://tunnel.example/api/webhooks/mollie/payment");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createPaymentAndCaptureBody(String webhookUrl) {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MolliePaymentResponse response = new MolliePaymentResponse(
                "tr_test", "open", new MolliePaymentResponse.Amount("42.00", "EUR"), null, Map.of());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(MolliePaymentResponse.class))).thenReturn(ResponseEntity.ok(response));

        AppProperties properties = new AppProperties(null, null, null,
                new AppProperties.MollieProperties("test_key", "http://localhost:3000/booked",
                        webhookUrl, null, false, 3_600_000, 60_000),
                null, null, null, null, null);
        HttpMollieProviderClient client = new HttpMollieProviderClient(restTemplate, properties);

        client.createPayment(UUID.randomUUID(), new BigDecimal("42.00"), UUID.randomUUID());

        ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST),
                entityCaptor.capture(), eq(MolliePaymentResponse.class));
        return (Map<String, Object>) entityCaptor.getValue().getBody();
    }
}
