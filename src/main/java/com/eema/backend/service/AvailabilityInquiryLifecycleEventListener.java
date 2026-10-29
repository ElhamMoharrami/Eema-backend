package com.eema.backend.service;

import com.eema.backend.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor
public class AvailabilityInquiryLifecycleEventListener {
    private final AvailabilityInquiryNotificationService notifications;
    private final NotificationOutboxWriter outbox;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void inquiryLifecycleChanged(AvailabilityInquiryLifecycleEvent event) {
        outbox.record("INQUIRY",event.inquiryId(),event.type().name(),"INQUIRY:"+event.inquiryId()+":"+event.type(),
                () -> notifications.sendLifecycle(event.inquiryId(),event.type()));
    }
}
