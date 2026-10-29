package com.eema.backend.service;

import com.eema.backend.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor
public class AccountEmailEventListener {
    private final AccountEmailNotificationService notifications;
    private final NotificationOutboxWriter outbox;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void verificationRequested(EmailVerificationRequestedEvent event) {
        outbox.auth(event.userId(),"EMAIL_VERIFICATION",event.rawToken(),
                () -> notifications.sendVerification(event.userId(),NotificationOutboxWriter.TOKEN_PLACEHOLDER));
    }
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void passwordResetRequested(PasswordResetRequestedEvent event) {
        outbox.auth(event.userId(),"PASSWORD_RESET",event.rawToken(),
                () -> notifications.sendPasswordReset(event.userId(),NotificationOutboxWriter.TOKEN_PLACEHOLDER));
    }
}
