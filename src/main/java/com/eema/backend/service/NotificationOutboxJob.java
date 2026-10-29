package com.eema.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;

@Component @RequiredArgsConstructor @Slf4j
public class NotificationOutboxJob {
    private final NotificationOutboxDispatcher dispatcher;
    private final AtomicBoolean running = new AtomicBoolean();

    @Scheduled(fixedDelayString="${app.notification-outbox.poll-delay-ms:10000}",
            initialDelayString="${app.notification-outbox.initial-delay-ms:10000}")
    public void dispatchDue() {
        // At most one submitted/running batch per instance, including executor saturation.
        if(!running.compareAndSet(false,true)) return;
        try { dispatcher.dispatchAsync(()->running.set(false)); }
        catch(RuntimeException failure) {running.set(false);log.warn("Notification batch submission failed: errorType={}",failure.getClass().getSimpleName());}
    }
}
