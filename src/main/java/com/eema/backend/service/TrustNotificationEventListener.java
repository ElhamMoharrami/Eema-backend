package com.eema.backend.service;

import com.eema.backend.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor
public class TrustNotificationEventListener {
    private final TrustNotificationService notifications;
    private final NotificationOutboxWriter outbox;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void notifyOwner(TrustNotificationEvent event) {
        outbox.record("TRUST",event.subjectId(),event.type().name(),"TRUST:"+java.util.Objects.requireNonNull(event.eventId()),
                () -> notifications.send(event));
    }
}
