package com.eema.backend.dto.response;

public record MollieRefundResponse(
        String id,
        String status,
        String paymentId,
        MolliePaymentResponse.Amount amount
) {}
