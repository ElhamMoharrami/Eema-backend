package com.eema.backend.service;

import com.eema.backend.entity.enums.PaymentCreationStatus;
import com.eema.backend.entity.enums.RefundStatus;
import com.eema.backend.event.*;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.OwnerPayoutRepository;
import com.eema.backend.repository.OwnerFinancialLiabilityRepository;
import com.eema.backend.entity.enums.OwnerPayoutStatus;
import com.eema.backend.entity.enums.OwnerFinancialLiabilityStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class WorkflowObservability {
    private final BookingRepository bookings;
    private final OwnerPayoutRepository payouts;
    private final OwnerFinancialLiabilityRepository liabilities;
    private final Counter approvalExpirations;
    private final Counter requestExpirations;
    private final Counter requestUnavailable;
    private final Counter latePayments;
    private final Counter automaticRefunds;
    private final Counter inquiryOfferExpirations;
    private final AtomicLong stuckPaymentCreations = new AtomicLong();
    private final AtomicLong stuckRefunds = new AtomicLong();
    private final AtomicLong unreconciledFees = new AtomicLong();
    private final AtomicLong heldPayouts = new AtomicLong();
    private final AtomicLong readyPayouts = new AtomicLong();
    private final AtomicLong overduePayouts = new AtomicLong();
    private final AtomicLong openOwnerLiabilities = new AtomicLong();
    private final OperationalThresholds thresholds;

    public WorkflowObservability(BookingRepository bookings, OwnerPayoutRepository payouts,
            OwnerFinancialLiabilityRepository liabilities, MeterRegistry registry,
            OperationalThresholds thresholds) {
        this.bookings = bookings;
        this.thresholds = thresholds;
        this.approvalExpirations = Counter.builder("eema.workflow.booking.approval.expired").register(registry);
        this.requestExpirations = Counter.builder("eema.workflow.booking.request.expired").register(registry);
        this.requestUnavailable = Counter.builder("eema.workflow.booking.request.unavailable").register(registry);
        this.latePayments = Counter.builder("eema.workflow.payment.late.detected").register(registry);
        this.automaticRefunds = Counter.builder("eema.workflow.refund.automatic.initiated").register(registry);
        this.inquiryOfferExpirations = Counter.builder("eema.workflow.inquiry.offer.expired").register(registry);
        registry.gauge("eema.workflow.payment.creation.stuck", stuckPaymentCreations);
        registry.gauge("eema.workflow.refund.stuck", stuckRefunds);
        registry.gauge("eema.workflow.mollie.fee.unreconciled", unreconciledFees);
        registry.gauge("eema.workflow.payout.held", heldPayouts);
        registry.gauge("eema.workflow.payout.ready", readyPayouts);
        registry.gauge("eema.workflow.payout.overdue", overduePayouts);
        registry.gauge("eema.workflow.owner.liability.open", openOwnerLiabilities);
        this.payouts = payouts;
        this.liabilities = liabilities;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void bookingLifecycle(BookingLifecycleEvent event) {
        if (event.type() == BookingLifecycleEvent.Type.APPROVAL_EXPIRED) approvalExpirations.increment();
        if (event.type() == BookingLifecycleEvent.Type.REQUEST_EXPIRED) requestExpirations.increment();
        if (event.type() == BookingLifecycleEvent.Type.REQUEST_BECAME_UNAVAILABLE) requestUnavailable.increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void inquiryLifecycle(AvailabilityInquiryLifecycleEvent event) {
        if (event.type() == AvailabilityInquiryLifecycleEvent.Type.OFFER_EXPIRED) inquiryOfferExpirations.increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void latePayment(LatePaymentDetectedEvent event) { latePayments.increment(); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void automaticRefund(LatePaymentRefundInitiatedEvent event) { automaticRefunds.increment(); }

    @Scheduled(fixedDelayString = "${app.workflow-observability.scan-delay-ms:300000}")
    public void scanStuckStates() {
        OffsetDateTime now = OffsetDateTime.now();
        long payments = bookings.countByPaymentCreationStatusAndPaymentCreationStartedAtBefore(
                PaymentCreationStatus.CREATING, thresholds.paymentCutoff(now));
        long refunds = bookings.countByRefundStatusInAndRefundRequestedAtBefore(
                List.of(RefundStatus.CREATING, RefundStatus.QUEUED, RefundStatus.PENDING, RefundStatus.PROCESSING),
                thresholds.refundCutoff(now));
        long fees = bookings.countOldUnreconciledMollieFees(thresholds.feeCutoff(now));
        long held = payouts.countByStatus(OwnerPayoutStatus.HELD);
        long ready = payouts.countByStatus(OwnerPayoutStatus.READY);
        long overdue = payouts.countByStatusAndReadyAtBefore(OwnerPayoutStatus.READY, thresholds.payoutCutoff(now));
        long openLiabilities = liabilities.countByStatus(OwnerFinancialLiabilityStatus.OPEN);
        stuckPaymentCreations.set(payments);
        stuckRefunds.set(refunds);
        unreconciledFees.set(fees);
        heldPayouts.set(held);
        readyPayouts.set(ready);
        overduePayouts.set(overdue);
        openOwnerLiabilities.set(openLiabilities);
        if (payments > 0 || refunds > 0 || fees > 0 || overdue > 0 || openLiabilities > 0) {
            log.warn("Operational workflow attention required: paymentCreations={}, refunds={}, unreconciledMollieFees={}, overduePayouts={}, openOwnerLiabilities={}",
                    payments, refunds, fees, overdue, openLiabilities);
        }
    }
}
