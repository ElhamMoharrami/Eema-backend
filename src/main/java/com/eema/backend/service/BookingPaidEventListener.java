package com.eema.backend.service;

import com.eema.backend.event.BookingPaidEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BookingPaidEventListener {
    private final BookingNotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void bookingPaid(BookingPaidEvent event) {
        notificationService.sendPaymentReceived(event.bookingId());
    }
}
