package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.Booking;
import com.eema.backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MollieFeeReconciliationJob {
    private final AppProperties properties;
    private final BookingRepository bookingRepository;
    private final MollieFeeReportingClient reportingClient;
    private final BookingService bookingService;

    // Scan position is an optimization, never financial authority. Restart begins at the oldest row.
    private record Cursor(java.time.OffsetDateTime time, java.util.UUID id) {}
    private Cursor cursor;

    @Scheduled(fixedDelayString = "${app.mollie.fee-reconciliation-delay-ms:3600000}",
            initialDelayString = "${app.mollie.fee-reconciliation-initial-delay-ms:60000}")
    public synchronized void reconcile() {
        if (!properties.mollie().feeReconciliationEnabled()) return;
        var page = PageRequest.of(0, 100);
        var batch = cursor == null ? bookingRepository.findPendingFeeReconciliation(page)
                : bookingRepository.findPendingFeeReconciliationAfter(cursor.time(), cursor.id(), page);
        if (batch.isEmpty() && cursor != null) {
            cursor = null;
            batch = bookingRepository.findPendingFeeReconciliation(page);
        }
        for (Booking booking : batch) {
            try {
                reportingClient.findAuthoritativePaymentFee(booking.getMolliePaymentId())
                        .ifPresent(fee -> bookingService.reconcileMollieFee(booking.getId(), fee));
            } catch (Exception e) {
                log.warn("Mollie fee reconciliation deferred: bookingId={}, paymentId={}, errorType={}",
                        booking.getId(), booking.getMolliePaymentId(), e.getClass().getSimpleName());
            }
        }
        if (!batch.isEmpty()) {
            Booking last = batch.getLast();
            cursor = new Cursor(last.getPaidAt() == null ? last.getCreatedAt() : last.getPaidAt(), last.getId());
        }
    }
}
