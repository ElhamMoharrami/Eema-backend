package com.eema.backend.service;

import com.eema.backend.event.BookingCancelledEvent;
import com.eema.backend.event.CancellationRefundCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BookingCancellationEventListener {
    private final BookingNotificationService notifications;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cancelled(BookingCancelledEvent event) {
        notifications.sendCancellation(event.bookingId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refundCompleted(CancellationRefundCompletedEvent event) {
        notifications.sendCancellationRefundCompleted(event.bookingId());
    }
}
