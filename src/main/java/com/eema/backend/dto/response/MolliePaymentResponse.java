package com.eema.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;

public record MolliePaymentResponse(
    String id,                          // payment ID (e.g. "tr_WDqYK6vllg")
    String status,                      // "pending", "open", "expired", etc.
    Amount amount,                      // payment amount and currency
    @JsonProperty("_links")
    MollieLinks links                   // contains checkout URL
    ,
    Map<String, String> metadata        // metadata from the payment
) {
    public record Amount(
        String value,                   // amount value as string (e.g., "100.00")
        String currency                 // currency code (e.g., "EUR")
    ) {}

    public record MollieLinks(
        @JsonProperty("checkout")
        MollieLink checkout              // the redirect URL
    ) {}

    public record MollieLink(
        String href                      // actual URL to redirect to
    ) {}
}