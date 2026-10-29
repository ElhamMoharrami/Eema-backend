package com.eema.backend.service;

import com.eema.backend.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor
public class BookingCancellationEventListener {
    private final BookingNotificationService notifications;
    private final NotificationOutboxWriter outbox;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void cancelled(BookingCancelledEvent event) {
        outbox.record("BOOKING",event.bookingId(),"CANCELLATION","BOOKING:"+event.bookingId()+":CANCELLATION",
                () -> notifications.sendCancellation(event.bookingId()));
    }
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void refundCompleted(CancellationRefundCompletedEvent event) {
        outbox.record("BOOKING",event.bookingId(),"CANCELLATION_REFUND_COMPLETED","BOOKING:"+event.bookingId()+":CANCELLATION_REFUND_COMPLETED",
                () -> notifications.sendCancellationRefundCompleted(event.bookingId()));
    }
}
