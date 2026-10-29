package com.eema.backend.service;

import com.eema.backend.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor
public class LatePaymentRefundEventListener {
    private final BookingNotificationService notifications;
    private final NotificationOutboxWriter outbox;
    private final com.eema.backend.repository.BookingRepository bookings;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void refundInitiated(LatePaymentRefundInitiatedEvent event) {
        // This financial event also counts late receipts after unpaid withdrawal.
        // That CANCELLED branch has no existing initiation template (the expired-window
        // wording would be false). Preserve its existing completion email and metric.
        var booking=bookings.findById(event.bookingId()).orElseThrow();
        if(booking.getStatus()==com.eema.backend.entity.enums.BookingStatus.CANCELLED) return;
        outbox.record("BOOKING",event.bookingId(),"LATE_PAYMENT_REFUND","BOOKING:"+event.bookingId()+":LATE_PAYMENT_REFUND",
                () -> notifications.sendLatePaymentRefundInitiated(event.bookingId()));
    }
}
