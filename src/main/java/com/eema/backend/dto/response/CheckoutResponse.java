package com.eema.backend.dto.response;

import java.util.UUID;

public record CheckoutResponse(
    UUID bookingId,              // the booking that was created (in PENDING state)
    String checkoutUrl,          // Mollie payment page URL — redirect to this
    String molliePaymentId,      // Mollie's payment ID
    Double totalAmount,          // EUR amount that will be charged
    Long expiresIn               // seconds until this checkout expires
) {}
