package com.eema.backend.service;

import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.dto.response.MollieRefundResponse;
import java.math.BigDecimal;
import java.util.UUID;

public interface MollieProviderClient {
    MolliePaymentResponse createPayment(UUID bookingId, BigDecimal amount, UUID idempotencyKey);
    MolliePaymentResponse getPayment(String paymentId);
    MollieRefundResponse createFullRefund(String paymentId, UUID bookingId, BigDecimal amount, UUID idempotencyKey);
    MollieRefundResponse getRefund(String paymentId, String refundId);
}
