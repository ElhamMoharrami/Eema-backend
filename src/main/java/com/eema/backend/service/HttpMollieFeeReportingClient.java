package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class HttpMollieFeeReportingClient implements MollieFeeReportingClient {
    private static final String FIRST_PAGE = "https://api.mollie.com/v2/balances/primary/transactions?limit=250";
    private final RestTemplate restTemplate;
    private final AppProperties properties;

    @Override
    public Optional<BigDecimal> findAuthoritativePaymentFee(String paymentId) {
        String token = properties.mollie().reportingAccessToken();
        if (token == null || token.isBlank()) return Optional.empty();
        String url = FIRST_PAGE;
        Set<String> visited = new HashSet<>();
        while (url != null && visited.add(url)) {
            JsonNode root = get(url, token);
            JsonNode transactions = root.path("_embedded").path("balance_transactions");
            for (JsonNode transaction : transactions) {
                if (paymentId.equals(transaction.path("context").path("paymentId").asText())
                        && "payment".equals(transaction.path("type").asText())) {
                    JsonNode fees = transaction.path("deductionDetails").path("fees");
                    if (fees.isMissingNode() || fees.isNull()) return Optional.empty();
                    if (!"EUR".equalsIgnoreCase(fees.path("currency").asText())) {
                        throw new IllegalStateException("Mollie fee currency is not EUR");
                    }
                    return Optional.of(new BigDecimal(fees.path("value").asText()));
                }
            }
            JsonNode next = root.path("_links").path("next").path("href");
            url = next.isTextual() ? next.asText() : null;
            if (url != null && !url.startsWith("https://api.mollie.com/")) {
                throw new IllegalStateException("Unexpected Mollie pagination URL");
            }
        }
        if (url != null) throw new IllegalStateException("Mollie pagination loop detected");
        return Optional.empty();
    }

    private JsonNode get(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Void>(headers), JsonNode.class);
        if (response.getBody() == null) throw new IllegalStateException("Mollie reporting returned an empty response");
        return response.getBody();
    }
}
