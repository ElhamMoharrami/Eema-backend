package com.eema.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;

public record MolliePaymentResponse(
    String id,                          // payment ID (e.g. "tr_WDqYK6vllg")
    String status,                      // "pending", "open", "expired", etc.

    @JsonProperty("_links")
    MollieLinks links                   // contains checkout URL
    ,
    Map<String, String> metadata        // metadata from the payment
) {
    public record MollieLinks(
        @JsonProperty("checkout")
        MollieLink checkout              // the redirect URL
    ) {}

    public record MollieLink(
        String href                      // actual URL to redirect to
    ) {}
}