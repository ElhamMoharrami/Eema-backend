package com.eema.backend.service;

import java.time.Clock;
import java.util.UUID;
import com.eema.backend.repository.OwnerPayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Component @RequiredArgsConstructor
public class OwnerPayoutReadinessJob {
    private final OwnerPayoutRepository payouts;
    private final OwnerPayoutService service;
    private final Clock clock;

    private record Cursor(OffsetDateTime time, UUID id) {}
    private volatile Cursor cursor;

    @Scheduled(fixedDelayString = "${app.payout.readiness-scan-delay-ms:300000}")
    @Transactional
    public void scan() {
        // Acquire booking locks first, just like cancellation, reconciliation and mark-paid.
        // Fetch payout state only after that lock; skip busy bookings instead of blocking the batch.
        OffsetDateTime now = OffsetDateTime.now(clock);
        Cursor position = cursor;
        var batch = payouts.lockReadinessCandidatesAfter(now,
                position == null ? null : position.time(), position == null ? null : position.id(), 100);
        if (batch.isEmpty() && position != null) {
            batch = payouts.lockReadinessCandidatesAfter(now, null, null, 100);
        }
        for (var candidate : batch) service.recalculateForBooking(candidate.getBookingId());
        Cursor next = batch.isEmpty() ? null : new Cursor(batch.getLast().getEligibleAt().atOffset(java.time.ZoneOffset.UTC), batch.getLast().getBookingId());
        // Advance only after the batch commits. Lost scan position only causes safe revisiting.
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { cursor = next; }
                });
    }
}
