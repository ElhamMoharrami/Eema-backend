package com.eema.backend.service;

import com.eema.backend.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor
public class BookingPaidEventListener {
    private final BookingNotificationService notifications;
    private final NotificationOutboxWriter outbox;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void bookingPaid(BookingPaidEvent event) {
        outbox.record("BOOKING",event.bookingId(),"PAYMENT_RECEIVED","BOOKING:"+event.bookingId()+":PAYMENT_RECEIVED",
                () -> notifications.sendPaymentReceived(event.bookingId()));
    }
}
