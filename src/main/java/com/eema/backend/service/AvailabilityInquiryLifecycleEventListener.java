package com.eema.backend.service;

import com.eema.backend.event.AvailabilityInquiryLifecycleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AvailabilityInquiryLifecycleEventListener {
    private final AvailabilityInquiryNotificationService notifications;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void inquiryLifecycleChanged(AvailabilityInquiryLifecycleEvent event) {
        try {
            notifications.sendLifecycle(event.inquiryId(), event.type());
        } catch (RuntimeException exception) {
            log.error("Inquiry lifecycle notification failed: inquiryId={}, notificationType={}, recipientType=unknown, errorType={}",
                    event.inquiryId(), event.type(), exception.getClass().getSimpleName());
        }
    }
}
