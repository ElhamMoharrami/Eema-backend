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
    private static final Set<String> SAFE_EVENT_KEYS = Set.of(
            "amount", "amountEur", "currency", "provider", "paymentId", "refundId", "payoutId",
            "liabilityId", "type", "from", "to", "reason", "source", "policyTier",
            "refundPercentage", "refundAmountEur", "retainedAmountEur", "commissionEur",
            "ownerPayoutEur", "mollieFeeEur", "ownerResponseExpiresAt", "paymentExpiresAt",
            "externalTransferReference", "documentId", "documentNumber", "templateVersion");

    private final BookingRepository bookings;
    private final BookingEventRepository events;
    private final OwnerPayoutRepository payouts;
    private final OwnerFinancialLiabilityRepository liabilities;
    private final OwnerPayoutService payoutService;
    private final OwnerFinancialLiabilityService liabilityService;
    private final OperationalThresholds thresholds;
    private final OperationalExceptionRepository exceptions;

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
        OffsetDateTime now = thresholds.now();
        return exceptions.findPage(AdminPagePolicy.bounded(pageable), thresholds.payoutCutoff(now),
                thresholds.refundCutoff(now), thresholds.paymentCutoff(now), thresholds.feeCutoff(now));
    }

    private Booking booking(UUID id) { return bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking not found: " + id)); }
    private AdminBookingSummaryDto summary(Booking b) { return new AdminBookingSummaryDto(b.getId(), b.getVenueId(), b.getScoutId(), b.getStatus(), b.getShootDate(), b.getStartTime(), b.getEndTime(), b.getVenuePriceEur(), b.getTotalPaidEur(), b.getOwnerPayoutEur(), b.getMolliePaymentId(), b.getPaymentCreationStatus(), b.getRefundStatus(), b.getCreatedAt(), b.getConfirmedAt(), b.getCancelledAt(), b.getLatePaymentDetectedAt()); }
    private Map<String, Object> safePayload(BookingEventPayload payload) {
        if (payload == null || payload.getData() == null) return Map.of();
        Map<String, Object> safe = new LinkedHashMap<>();
        payload.getData().forEach((key, value) -> { if (SAFE_EVENT_KEYS.contains(key) && (value == null || value instanceof String || value instanceof Number || value instanceof Boolean)) safe.put(key, value); });
        return Collections.unmodifiableMap(safe);
    }
}
