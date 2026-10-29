package com.eema.backend.service;

import com.eema.backend.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor
public class BookingLifecycleEventListener {
    private final BookingNotificationService notifications;
    private final NotificationOutboxWriter outbox;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void bookingLifecycleChanged(BookingLifecycleEvent event) {
        outbox.record("BOOKING",event.bookingId(),event.type().name(),"BOOKING:"+event.bookingId()+":"+event.type(),
                () -> notifications.sendLifecycle(event.bookingId(),event.type()));
    }
}
