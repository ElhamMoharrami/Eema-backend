package com.eema.backend.service;

import com.eema.backend.event.LatePaymentRefundInitiatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LatePaymentRefundEventListener {
    private final BookingNotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refundInitiated(LatePaymentRefundInitiatedEvent event) {
        notificationService.sendLatePaymentRefundInitiated(event.bookingId());
    }
}
