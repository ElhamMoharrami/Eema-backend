package com.eema.backend.service;

import com.eema.backend.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor @Slf4j
public class AccountEmailEventListener {
    private final AccountEmailNotificationService notifications;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void verificationRequested(EmailVerificationRequestedEvent event) {
        try { notifications.sendVerification(event.userId(), event.rawToken()); }
        catch (RuntimeException exception) { log.error("Verification email delivery failed: userId={}, errorType={}", event.userId(), exception.getClass().getSimpleName()); }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void passwordResetRequested(PasswordResetRequestedEvent event) {
        try { notifications.sendPasswordReset(event.userId(), event.rawToken()); }
        catch (RuntimeException exception) { log.error("Password-reset email delivery failed: userId={}, errorType={}", event.userId(), exception.getClass().getSimpleName()); }
    }
}
