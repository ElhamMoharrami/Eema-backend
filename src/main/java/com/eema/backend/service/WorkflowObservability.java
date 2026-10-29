package com.eema.backend.service;

import com.eema.backend.entity.enums.PaymentCreationStatus;
import com.eema.backend.entity.enums.RefundStatus;
import com.eema.backend.event.*;
import com.eema.backend.repository.BookingRepository;
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
    private final Counter approvalExpirations;
    private final Counter latePayments;
    private final Counter automaticRefunds;
    private final Counter inquiryOfferExpirations;
    private final AtomicLong stuckPaymentCreations = new AtomicLong();
    private final AtomicLong stuckRefunds = new AtomicLong();
    private final AtomicLong unreconciledFees = new AtomicLong();
    private final long paymentStuckMinutes;
    private final long refundStuckMinutes;
    private final long feeStuckMinutes;

    public WorkflowObservability(BookingRepository bookings, MeterRegistry registry,
            @Value("${app.workflow-observability.payment-creating-stuck-minutes:60}") long paymentStuckMinutes,
            @Value("${app.workflow-observability.refund-stuck-minutes:1440}") long refundStuckMinutes,
            @Value("${app.workflow-observability.fee-unreconciled-minutes:1440}") long feeStuckMinutes) {
        this.bookings = bookings;
        this.paymentStuckMinutes = paymentStuckMinutes;
        this.refundStuckMinutes = refundStuckMinutes;
        this.feeStuckMinutes = feeStuckMinutes;
        this.approvalExpirations = Counter.builder("eema.workflow.booking.approval.expired").register(registry);
        this.latePayments = Counter.builder("eema.workflow.payment.late.detected").register(registry);
        this.automaticRefunds = Counter.builder("eema.workflow.refund.automatic.initiated").register(registry);
        this.inquiryOfferExpirations = Counter.builder("eema.workflow.inquiry.offer.expired").register(registry);
        registry.gauge("eema.workflow.payment.creation.stuck", stuckPaymentCreations);
        registry.gauge("eema.workflow.refund.stuck", stuckRefunds);
        registry.gauge("eema.workflow.mollie.fee.unreconciled", unreconciledFees);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void bookingLifecycle(BookingLifecycleEvent event) {
        if (event.type() == BookingLifecycleEvent.Type.APPROVAL_EXPIRED) approvalExpirations.increment();
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
                PaymentCreationStatus.CREATING, now.minusMinutes(paymentStuckMinutes));
        long refunds = bookings.countByRefundStatusInAndRefundRequestedAtBefore(
                List.of(RefundStatus.CREATING, RefundStatus.QUEUED, RefundStatus.PENDING, RefundStatus.PROCESSING),
                now.minusMinutes(refundStuckMinutes));
        long fees = bookings.countOldUnreconciledMollieFees(now.minusMinutes(feeStuckMinutes));
        stuckPaymentCreations.set(payments);
        stuckRefunds.set(refunds);
        unreconciledFees.set(fees);
        if (payments > 0 || refunds > 0 || fees > 0) {
            log.warn("Stuck workflow states detected: paymentCreations={}, refunds={}, unreconciledMollieFees={}",
                    payments, refunds, fees);
        }
    }
}
