package com.eema.backend.service;

import com.eema.backend.dto.response.MolliePaymentResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

class WebhookServiceTest {
    private final MollieProviderClient mollie = mock(MollieProviderClient.class);
    private final BookingService bookings = mock(BookingService.class);
    private final RefundService refunds = mock(RefundService.class);
    private final WebhookService service = new WebhookService(mollie, bookings, refunds);

    @Test
    void latePaidWebhookStartsRefundInsteadOfNormalConfirmationPath() {
        UUID bookingId = UUID.randomUUID();
        MolliePaymentResponse payment = new MolliePaymentResponse("tr_late", "paid",
                new MolliePaymentResponse.Amount("100.00", "EUR"), null,
                Map.of("bookingId", bookingId.toString()));
        when(mollie.getPayment("tr_late")).thenReturn(payment);
        when(bookings.processPaidPayment(payment)).thenReturn(
                new BookingService.PaidPaymentOutcome(bookingId, BookingService.PaidPaymentResult.LATE_PAYMENT));

        service.handlePaymentWebhook("tr_late");

        verify(refunds).requestLatePaymentRefund(bookingId);
        verify(refunds).reconcileForPayment("tr_late");
    }
}
