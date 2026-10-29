package com.eema.backend.service;

import com.eema.backend.dto.response.MollieRefundResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.RefundStatus;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.repository.BookingEventRepository;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.entity.BookingEvent;
import com.eema.backend.entity.BookingEventPayload;
import com.eema.backend.entity.enums.ActorType;
import com.eema.backend.entity.enums.BookingEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundCoordinator {
    private static final Duration IDEMPOTENCY_WINDOW = Duration.ofHours(1);
    private final BookingRepository bookingRepository;
    private final BookingEventRepository eventRepository;
    private final BookingFinancials financials;
    private final BookingStateMachine stateMachine;

    @Transactional
    public Claim claim(UUID bookingId, UUID adminId, String adminName) {
        Booking booking = locked(bookingId);
        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Only paid or confirmed bookings can be refunded");
        }
        if (booking.getMolliePaymentId() == null) throw new ConflictException("Booking has no Mollie payment");
        if (booking.getRefundStatus() == RefundStatus.REFUNDED) throw new ConflictException("Booking is already refunded");
        if (booking.getRefundStatus() != RefundStatus.NONE && booking.getRefundStatus() != RefundStatus.FAILED
                && booking.getRefundStatus() != RefundStatus.CANCELED && booking.getRefundStatus() != RefundStatus.CREATING) {
            throw new ConflictException("A refund is already in progress");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (booking.getRefundStatus() == RefundStatus.CREATING) {
            if (booking.getRefundRequestedAt().plus(IDEMPOTENCY_WINDOW).isBefore(now)) {
                throw new ConflictException("Refund creation requires manual reconciliation before retry");
            }
        } else {
            booking.setRefundStatus(RefundStatus.CREATING);
            booking.setRefundIdempotencyKey(UUID.randomUUID());
            booking.setRefundAmountEur(financials.money(booking.getTotalPaidEur()));
            booking.setRefundRequestedAt(now);
            append(bookingId, BookingEventType.REFUND_REQUESTED, adminId, adminName, ActorType.ADMIN,
                    Map.of("amount", booking.getRefundAmountEur(), "currency", "EUR"));
        }
        return new Claim(bookingId, booking.getMolliePaymentId(), booking.getRefundAmountEur(), booking.getRefundIdempotencyKey());
    }

    @Transactional
    public void recordProviderState(Claim claim, MollieRefundResponse refund) {
        Booking booking = locked(claim.bookingId());
        validate(booking, refund);
        booking.setMollieRefundId(refund.id());
        applyStatus(booking, providerStatus(refund.status()));
    }

    @Transactional
    public void reconcile(String paymentId, MollieRefundResponse refund) {
        Booking booking = bookingRepository.findByMolliePaymentId(paymentId)
                .orElseThrow(() -> new NotFoundException("Booking not found for Mollie payment"));
        booking = locked(booking.getId());
        if (booking.getMollieRefundId() == null) return;
        validate(booking, refund);
        applyStatus(booking, providerStatus(refund.status()));
    }

    @Transactional
    public void markRejected(UUID bookingId) {
        Booking booking = locked(bookingId);
        if (booking.getRefundStatus() == RefundStatus.CREATING) applyStatus(booking, RefundStatus.FAILED);
    }

    private void validate(Booking booking, MollieRefundResponse refund) {
        if (refund == null || refund.id() == null || refund.paymentId() == null || refund.amount() == null) {
            throw new IllegalStateException("Mollie returned an incomplete refund");
        }
        if (!booking.getMolliePaymentId().equals(refund.paymentId())) throw new IllegalStateException("Refund payment does not match booking");
        if (booking.getMollieRefundId() != null && !booking.getMollieRefundId().equals(refund.id())) throw new IllegalStateException("Refund ID does not match booking");
        if (!"EUR".equalsIgnoreCase(refund.amount().currency())
                || financials.money(new BigDecimal(refund.amount().value())).compareTo(booking.getRefundAmountEur()) != 0) {
            throw new IllegalStateException("Refund amount does not match booking");
        }
    }

    private void applyStatus(Booking booking, RefundStatus target) {
        if (booking.getRefundStatus() == target) return;
        RefundStatus previous = booking.getRefundStatus();
        booking.setRefundStatus(target);
        if (target == RefundStatus.REFUNDED) {
            stateMachine.requireTransition(booking.getStatus(), BookingStatus.REFUNDED);
            booking.setStatus(BookingStatus.REFUNDED);
            booking.setRefundedAt(OffsetDateTime.now());
            booking.setOwnerPayoutEur(BigDecimal.ZERO.setScale(2));
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("from", previous.name());
        payload.put("to", target.name());
        if (booking.getMollieRefundId() != null) payload.put("refundId", booking.getMollieRefundId());
        append(booking.getId(), target == RefundStatus.REFUNDED ? BookingEventType.PAYMENT_REFUNDED : BookingEventType.REFUND_STATUS_CHANGED,
                null, "Mollie", ActorType.WEBHOOK, payload);
    }

    private RefundStatus providerStatus(String status) {
        try { return RefundStatus.valueOf(status.toUpperCase()); }
        catch (Exception e) { throw new IllegalStateException("Unsupported Mollie refund status"); }
    }

    private Booking locked(UUID id) {
        return bookingRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Booking not found: " + id));
    }

    private void append(UUID bookingId, BookingEventType type, UUID actorId, String actorName, ActorType actorType, Map<String, Object> data) {
        eventRepository.save(BookingEvent.builder().bookingId(bookingId).eventType(type).actorId(actorId)
                .actorName(actorName).actorType(actorType).payload(new BookingEventPayload(data)).build());
    }

    public record Claim(UUID bookingId, String paymentId, BigDecimal amount, UUID idempotencyKey) {}
}
