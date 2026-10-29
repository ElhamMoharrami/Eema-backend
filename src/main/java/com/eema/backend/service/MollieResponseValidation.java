package com.eema.backend.service;

import com.eema.backend.dto.response.MolliePaymentResponse;
import java.math.BigDecimal;
import java.net.URI;

/** Validate provider facts without rounding away an economic mismatch. */
final class MollieResponseValidation {
    private MollieResponseValidation() {}

    static boolean paymentId(String value) {
        return value != null && value.matches("tr_[A-Za-z0-9_-]+");
    }

    static BigDecimal eurAmount(MolliePaymentResponse.Amount amount) {
        if (amount == null || !"EUR".equalsIgnoreCase(amount.currency())
                || amount.value() == null || !amount.value().matches("[0-9]+\\.[0-9]{2}")) {
            throw new IllegalStateException("Invalid provider EUR amount");
        }
        return new BigDecimal(amount.value());
    }

    static void checkoutUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || value.length() > 1000) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Invalid provider checkout URL");
        }
    }
}
