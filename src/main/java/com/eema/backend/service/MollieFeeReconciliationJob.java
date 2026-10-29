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

    @Scheduled(fixedDelayString = "${app.mollie.fee-reconciliation-delay-ms:3600000}",
            initialDelayString = "${app.mollie.fee-reconciliation-initial-delay-ms:60000}")
    public void reconcile() {
        if (!properties.mollie().feeReconciliationEnabled()) return;
        for (Booking booking : bookingRepository.findPendingFeeReconciliation(PageRequest.of(0, 100))) {
            try {
                reportingClient.findAuthoritativePaymentFee(booking.getMolliePaymentId())
                        .ifPresent(fee -> bookingService.reconcileMollieFee(booking.getId(), fee));
            } catch (Exception e) {
                log.warn("Mollie fee reconciliation deferred: bookingId={}, paymentId={}, errorType={}",
                        booking.getId(), booking.getMolliePaymentId(), e.getClass().getSimpleName());
            }
        }
    }
}
