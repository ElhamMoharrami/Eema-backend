package com.eema.backend.service;
import com.eema.backend.event.TrustNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component @RequiredArgsConstructor @Slf4j
public class TrustNotificationEventListener {
    private final TrustNotificationService notifications;
    @Async("notificationExecutor") @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void notifyOwner(TrustNotificationEvent event){try{notifications.send(event);}catch(RuntimeException ex){
        log.error("Trust workflow notification failed: subjectId={}, eventType={}, errorType={}",event.subjectId(),event.type(),ex.getClass().getSimpleName());}}
}
