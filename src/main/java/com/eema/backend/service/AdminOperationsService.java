package com.eema.backend.service;

import com.eema.backend.dto.response.*;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class AdminOperationsService {
    private static final List<RefundStatus> ACTIVE_REFUNDS = List.of(
            RefundStatus.CREATING, RefundStatus.QUEUED, RefundStatus.PENDING, RefundStatus.PROCESSING);
    private static final Set<String> SAFE_EVENT_KEYS = Set.of(
            "amount", "amountEur", "currency", "provider", "paymentId", "refundId", "payoutId",
            "liabilityId", "type", "from", "to", "reason", "source", "policyTier",
            "refundPercentage", "refundAmountEur", "retainedAmountEur", "commissionEur",
            "ownerPayoutEur", "mollieFeeEur", "ownerResponseExpiresAt", "paymentExpiresAt",
            "externalTransferReference");
    private static final int EXCEPTION_CANDIDATE_LIMIT = 200;

    private final BookingRepository bookings;
    private final BookingEventRepository events;
    private final OwnerPayoutRepository payouts;
    private final OwnerFinancialLiabilityRepository liabilities;
    private final OwnerPayoutService payoutService;
    private final OwnerFinancialLiabilityService liabilityService;
    private final OperationalThresholds thresholds;

    @Transactional(readOnly = true)
    public Page<AdminBookingSummaryDto> searchBookings(UUID bookingId, UUID venueId, UUID scoutId,
            BookingStatus status, String molliePaymentId, RefundStatus refundStatus,
            PaymentCreationStatus paymentCreationStatus, Pageable pageable) {
        String paymentId = molliePaymentId == null || molliePaymentId.isBlank() ? null : molliePaymentId.trim();
        return bookings.adminSearch(bookingId, venueId, scoutId, status, paymentId, refundStatus,
                paymentCreationStatus, AdminPagePolicy.bounded(pageable)).map(this::summary);
    }

    @Transactional(readOnly = true)
    public AdminBookingDetailDto bookingDetail(UUID id) {
        Booking b = booking(id);
        OwnerPayoutDto payout = payouts.findByBookingId(id).map(payoutService::dto).orElse(null);
        List<OwnerFinancialLiabilityDto> liabilityDtos = liabilities.findByBookingIdOrderByCreatedAtAsc(id)
                .stream().map(liabilityService::dto).toList();
        return new AdminBookingDetailDto(summary(b), b.getCommissionPercentage(), b.getCommissionEur(),
                b.getMollieFeeEur(), b.isMollieFeeReconciled(), b.getMollieFeeReconciledAt(),
                b.getRefundStatus(), b.getMollieRefundId(), b.getRefundAmountEur(), b.getRefundRequestedAt(),
                b.getRefundedAt(), b.getCancellationPolicyTier(), b.getCancelledByActorType(),
                b.getCancellationRefundPercentage(), b.getCancellationRefundAmountEur(),
                b.getCancellationRetainedAmountEur(), b.getProviderCostResponsibility(),
                b.getCancellationProviderCostEur(), b.isCancellationProviderCostReconciled(), payout, liabilityDtos);
    }

    @Transactional(readOnly = true)
    public Page<AdminBookingEventDto> events(UUID bookingId, Pageable pageable) {
        booking(bookingId);
        return events.findPageByBookingIdOrderByCreatedAtAsc(bookingId, AdminPagePolicy.bounded(pageable))
                .map(e -> new AdminBookingEventDto(e.getId(), e.getEventType(), e.getActorType(),
                        e.getActorName(), e.getCreatedAt(), safePayload(e.getPayload())));
    }

    @Transactional(readOnly = true)
    public Page<OperationalExceptionDto> exceptions(Pageable pageable) {
        OffsetDateTime now = OffsetDateTime.now();
        Pageable candidates = PageRequest.of(0, EXCEPTION_CANDIDATE_LIMIT);
        List<OperationalExceptionDto> result = new ArrayList<>();
        payouts.findByStatusOrderByCreatedAtAsc(OwnerPayoutStatus.HELD, candidates).forEach(p -> result.add(
                item(OperationalExceptionType.PAYOUT_HELD, OperationalExceptionSeverity.INFO, p.getBookingId(),
                        p.getId(), null, p.getUpdatedAt(), "PAYOUT_" + p.getHoldReason())));
        payouts.findByStatusAndReadyAtBeforeOrderByReadyAtAsc(OwnerPayoutStatus.READY,
                thresholds.payoutCutoff(now), candidates).forEach(p -> result.add(item(
                        OperationalExceptionType.PAYOUT_READY_OVERDUE, OperationalExceptionSeverity.WARNING,
                        p.getBookingId(), p.getId(), null, p.getReadyAt(), "READY_PAYOUT_NOT_RECORDED_PAID")));
        liabilities.findByStatusOrderByCreatedAtAsc(OwnerFinancialLiabilityStatus.OPEN, candidates).forEach(l -> result.add(
                item(OperationalExceptionType.OWNER_LIABILITY_OPEN, OperationalExceptionSeverity.WARNING,
                        l.getBookingId(), null, l.getId(), l.getCreatedAt(), "OWNER_LIABILITY_REQUIRES_RESOLUTION")));
        bookings.findStuckRefunds(ACTIVE_REFUNDS, thresholds.refundCutoff(now), candidates).forEach(b -> result.add(
                item(OperationalExceptionType.REFUND_STUCK, OperationalExceptionSeverity.CRITICAL,
                        b.getId(), null, null, b.getRefundRequestedAt(), "REFUND_EXCEEDED_STUCK_THRESHOLD")));
        bookings.findStuckPaymentCreations(PaymentCreationStatus.CREATING, thresholds.paymentCutoff(now), candidates)
                .forEach(b -> result.add(item(OperationalExceptionType.PAYMENT_CREATION_STUCK,
                        OperationalExceptionSeverity.WARNING, b.getId(), null, null,
                        b.getPaymentCreationStartedAt(), "PAYMENT_CREATION_EXCEEDED_STUCK_THRESHOLD")));
        bookings.findOldUnreconciledMollieFees(thresholds.feeCutoff(now), candidates).forEach(b -> result.add(
                item(OperationalExceptionType.MOLLIE_FEE_UNRECONCILED, OperationalExceptionSeverity.WARNING,
                        b.getId(), null, null, b.getPaidAt(), "AUTHORITATIVE_PROVIDER_FEE_UNAVAILABLE")));
        bookings.findLatePaymentRefundsInProgress(ACTIVE_REFUNDS, candidates).forEach(b -> result.add(
                item(OperationalExceptionType.LATE_PAYMENT_REFUND_IN_PROGRESS, OperationalExceptionSeverity.WARNING,
                        b.getId(), null, null, b.getLatePaymentDetectedAt(), "LATE_PAYMENT_REQUIRES_REFUND_COMPLETION")));
        result.sort(Comparator.comparingInt((OperationalExceptionDto e) -> severityRank(e.severity()))
                .thenComparing(OperationalExceptionDto::detectedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        Pageable bounded = AdminPagePolicy.bounded(pageable);
        int from = Math.min((int) bounded.getOffset(), result.size());
        int to = Math.min(from + bounded.getPageSize(), result.size());
        return new PageImpl<>(result.subList(from, to), bounded, result.size());
    }

    private Booking booking(UUID id) { return bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking not found: " + id)); }
    private AdminBookingSummaryDto summary(Booking b) { return new AdminBookingSummaryDto(b.getId(), b.getVenueId(), b.getScoutId(), b.getStatus(), b.getShootDate(), b.getStartTime(), b.getEndTime(), b.getVenuePriceEur(), b.getTotalPaidEur(), b.getOwnerPayoutEur(), b.getMolliePaymentId(), b.getPaymentCreationStatus(), b.getRefundStatus(), b.getCreatedAt(), b.getConfirmedAt(), b.getCancelledAt(), b.getLatePaymentDetectedAt()); }
    private Map<String, Object> safePayload(BookingEventPayload payload) {
        if (payload == null || payload.getData() == null) return Map.of();
        Map<String, Object> safe = new LinkedHashMap<>();
        payload.getData().forEach((key, value) -> { if (SAFE_EVENT_KEYS.contains(key)) safe.put(key, value); });
        return Collections.unmodifiableMap(safe);
    }
    private OperationalExceptionDto item(OperationalExceptionType type, OperationalExceptionSeverity severity,
            UUID bookingId, UUID payoutId, UUID liabilityId, OffsetDateTime detectedAt, String reason) {
        return new OperationalExceptionDto(type, severity, bookingId, payoutId, liabilityId, detectedAt, reason);
    }
    private int severityRank(OperationalExceptionSeverity s) { return switch (s) { case CRITICAL -> 0; case WARNING -> 1; case INFO -> 2; }; }
}
