package com.eema.backend.dto.response;

import java.math.BigDecimal;

/**
 * Represents a line item in a quote.
 */
public record QuoteItemDto(
    String label,      // e.g., "Day rate", "Service fee", "Total"
    BigDecimal amount, // in EUR
    String note        // optional explanation
) {}