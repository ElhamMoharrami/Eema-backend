package com.eema.backend.service;

import com.eema.backend.event.BookingLifecycleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingLifecycleEventListener {
    private final BookingNotificationService notifications;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void bookingLifecycleChanged(BookingLifecycleEvent event) {
        try {
            notifications.sendLifecycle(event.bookingId(), event.type());
        } catch (RuntimeException exception) {
            log.error("Booking lifecycle notification failed: bookingId={}, notificationType={}, recipientType=unknown, errorType={}",
                    event.bookingId(), event.type(), exception.getClass().getSimpleName());
        }
    }
}
