package com.eema.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

@Component
public class OperationalThresholds {
    private final long paymentStuckMinutes;
    private final long refundStuckMinutes;
    private final long feeStuckMinutes;
    private final long payoutOverdueHours;

    public OperationalThresholds(
            @Value("${app.workflow-observability.payment-creating-stuck-minutes:60}") long paymentStuckMinutes,
            @Value("${app.workflow-observability.refund-stuck-minutes:1440}") long refundStuckMinutes,
            @Value("${app.workflow-observability.fee-unreconciled-minutes:1440}") long feeStuckMinutes,
            @Value("${app.workflow-observability.payout-ready-overdue-hours:72}") long payoutOverdueHours) {
        this.paymentStuckMinutes = paymentStuckMinutes;
        this.refundStuckMinutes = refundStuckMinutes;
        this.feeStuckMinutes = feeStuckMinutes;
        this.payoutOverdueHours = payoutOverdueHours;
    }
    public OffsetDateTime paymentCutoff(OffsetDateTime now) { return now.minusMinutes(paymentStuckMinutes); }
    public OffsetDateTime refundCutoff(OffsetDateTime now) { return now.minusMinutes(refundStuckMinutes); }
    public OffsetDateTime feeCutoff(OffsetDateTime now) { return now.minusMinutes(feeStuckMinutes); }
    public OffsetDateTime payoutCutoff(OffsetDateTime now) { return now.minusHours(payoutOverdueHours); }
}
