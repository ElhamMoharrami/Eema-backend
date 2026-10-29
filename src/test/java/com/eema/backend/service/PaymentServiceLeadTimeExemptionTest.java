package com.eema.backend.service;

import com.eema.backend.dto.response.CheckoutResponse;
import com.eema.backend.dto.response.MolliePaymentResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentServiceLeadTimeExemptionTest {
    @Test void acceptedBookingCanCheckoutWithoutReapplyingLeadTime() {
        PaymentCreationCoordinator coordinator = mock(PaymentCreationCoordinator.class);
        MollieProviderClient mollie = mock(MollieProviderClient.class);
        UUID bookingId = UUID.randomUUID();
        UUID scoutId = UUID.randomUUID();
        UUID key = UUID.randomUUID();
        var claim = new PaymentCreationCoordinator.Claim(bookingId, new BigDecimal("100.00"), key, null);
        when(coordinator.claim(bookingId, scoutId)).thenReturn(claim);
        MolliePaymentResponse payment = new MolliePaymentResponse("tr_1", "open",
                new MolliePaymentResponse.Amount("100.00", "EUR"),
                new MolliePaymentResponse.MollieLinks(new MolliePaymentResponse.MollieLink("https://pay.example/1")),
                java.util.Map.of("bookingId", bookingId.toString()));
        CheckoutResponse checkout = new CheckoutResponse(bookingId, "https://pay.example/1", "tr_1",
                new BigDecimal("100.00"), 3600L);
        when(mollie.createPayment(bookingId, claim.amount(), claim.idempotencyKey())).thenReturn(payment);
        when(coordinator.complete(claim, payment)).thenReturn(checkout);

        assertThat(new PaymentService(coordinator, mollie).createPayment(bookingId, scoutId)).isSameAs(checkout);
        verify(mollie).createPayment(bookingId, claim.amount(), claim.idempotencyKey());
    }
}
