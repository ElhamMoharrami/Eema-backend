package com.eema.backend.service;

import com.eema.backend.entity.enums.PaymentCreationStatus;
import com.eema.backend.entity.enums.RefundStatus;
import com.eema.backend.event.*;
import com.eema.backend.repository.BookingRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkflowObservabilityTest {
    @Test
    void recordsLifecycleCountersWithoutHighCardinalityLabels() {
        BookingRepository bookings = mock(BookingRepository.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WorkflowObservability visibility = new WorkflowObservability(bookings, registry, 60, 1440, 1440);

        visibility.bookingLifecycle(new BookingLifecycleEvent(java.util.UUID.randomUUID(),
                BookingLifecycleEvent.Type.APPROVAL_EXPIRED));
        visibility.inquiryLifecycle(new AvailabilityInquiryLifecycleEvent(java.util.UUID.randomUUID(),
                AvailabilityInquiryLifecycleEvent.Type.OFFER_EXPIRED));
        visibility.latePayment(new LatePaymentDetectedEvent(java.util.UUID.randomUUID()));
        visibility.automaticRefund(new LatePaymentRefundInitiatedEvent(java.util.UUID.randomUUID()));

        assertEquals(1, registry.counter("eema.workflow.booking.approval.expired").count());
        assertEquals(1, registry.counter("eema.workflow.inquiry.offer.expired").count());
        assertEquals(1, registry.counter("eema.workflow.payment.late.detected").count());
        assertEquals(1, registry.counter("eema.workflow.refund.automatic.initiated").count());
        registry.getMeters().forEach(meter -> assertEquals(0, meter.getId().getTags().size()));
    }

    @Test
    void stuckScanPublishesReadOnlyGauges() {
        BookingRepository bookings = mock(BookingRepository.class);
        when(bookings.countByPaymentCreationStatusAndPaymentCreationStartedAtBefore(
                eq(PaymentCreationStatus.CREATING), any())).thenReturn(2L);
        when(bookings.countByRefundStatusInAndRefundRequestedAtBefore(anyList(), any())).thenReturn(3L);
        when(bookings.countOldUnreconciledMollieFees(any())).thenReturn(4L);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        WorkflowObservability visibility = new WorkflowObservability(bookings, registry, 60, 1440, 1440);

        visibility.scanStuckStates();

        assertEquals(2, registry.get("eema.workflow.payment.creation.stuck").gauge().value());
        assertEquals(3, registry.get("eema.workflow.refund.stuck").gauge().value());
        assertEquals(4, registry.get("eema.workflow.mollie.fee.unreconciled").gauge().value());
        verify(bookings).countByRefundStatusInAndRefundRequestedAtBefore(
                eq(List.of(RefundStatus.CREATING, RefundStatus.QUEUED, RefundStatus.PENDING, RefundStatus.PROCESSING)), any());
        verify(bookings, never()).save(any());
    }
}
