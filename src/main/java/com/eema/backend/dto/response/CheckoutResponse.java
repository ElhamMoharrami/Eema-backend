package com.eema.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutResponse(
    UUID bookingId,
    String checkoutUrl,          // Mollie payment page URL — redirect to this
    String molliePaymentId,      // Mollie's payment ID
    BigDecimal totalAmount,      // EUR amount that will be charged
    Long expiresIn               // seconds until this checkout expires
) {}
