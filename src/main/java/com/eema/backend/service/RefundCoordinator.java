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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.Objects;
import com.eema.backend.event.LatePaymentRefundInitiatedEvent;
import com.eema.backend.event.CancellationRefundCompletedEvent;

@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class RefundCoordinator {
    private static final Duration IDEMPOTENCY_WINDOW = Duration.ofHours(1);
    private final BookingRepository bookingRepository;
    private final BookingEventRepository eventRepository;
    private final BookingFinancials financials;
    private final BookingStateMachine stateMachine;
    private final ApplicationEventPublisher eventPublisher;
    private final OwnerPayoutService ownerPayoutService;
    private final java.time.Clock clock;

    RefundCoordinator(BookingRepository bookings, BookingEventRepository events, BookingFinancials financials,
                      BookingStateMachine states, ApplicationEventPublisher publisher, OwnerPayoutService payouts) {
        this(bookings, events, financials, states, publisher, payouts, java.time.Clock.systemUTC());
    }


    @Transactional
    public Claim claim(UUID bookingId, UUID adminId, String adminName) {
        Booking booking = locked(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Only confirmed bookings can be refunded");
        }
        if (!MollieResponseValidation.paymentId(booking.getMolliePaymentId()) || booking.getPaidAt() == null
                || booking.getTotalPaidEur() == null || booking.getTotalPaidEur().signum() <= 0) {
            throw new ConflictException("Only successfully paid bookings can be refunded");
        }
        if (booking.getRefundStatus() == RefundStatus.CREATING) {
            if (booking.getRefundIdempotencyKey() == null || booking.getRefundAmountEur() == null
                    || booking.getRefundAmountEur().signum() <= 0
                    || booking.getRefundAmountEur().compareTo(booking.getTotalPaidEur()) > 0) {
                throw new ConflictException("Refund claim requires manual reconciliation");
            }
            requireReplayWindow(booking);
        } else {
            if (booking.getRefundStatus() != RefundStatus.NONE || booking.getRefundIdempotencyKey() != null) {
                throw new ConflictException("Existing refund requires reconciliation; a new refund cannot be created");
            }
            booking.setRefundStatus(RefundStatus.CREATING);
            booking.setRefundIdempotencyKey(UUID.randomUUID());
            booking.setRefundAmountEur(financials.money(booking.getTotalPaidEur()));
            booking.setRefundRequestedAt(OffsetDateTime.now(clock));
            append(bookingId, BookingEventType.REFUND_REQUESTED, adminId, adminName, ActorType.ADMIN,
                    Map.of("amount", booking.getRefundAmountEur(), "currency", "EUR"));
            ownerPayoutService.recalculateForBooking(bookingId);
        }
        return new Claim(bookingId, booking.getMolliePaymentId(), booking.getRefundAmountEur(), booking.getRefundIdempotencyKey());
    }

    @Transactional
    public Claim claimCancellation(UUID bookingId, BigDecimal amount, UUID actorId,
                                   String actorName, ActorType actorType) {
        Booking booking = locked(bookingId);
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new ConflictException("Booking cancellation has not been recorded");
        }
        BigDecimal normalized = financials.money(amount);
        if (normalized.signum() <= 0 || normalized.compareTo(financials.money(booking.getTotalPaidEur())) > 0) {
            throw new ConflictException("Cancellation refund amount is invalid");
        }
        if (booking.getRefundStatus() == RefundStatus.REFUNDED
                || booking.getRefundStatus() == RefundStatus.QUEUED
                || booking.getRefundStatus() == RefundStatus.PENDING
                || booking.getRefundStatus() == RefundStatus.PROCESSING) {
            throw new ConflictException("A refund is already in progress or completed");
        }
        if (booking.getRefundIdempotencyKey() != null) {
            if (booking.getRefundAmountEur() == null || booking.getRefundAmountEur().compareTo(normalized) != 0) {
                throw new ConflictException("Cancellation refund amount cannot be changed");
            }
            if (booking.getRefundStatus() != RefundStatus.CREATING) {
                throw new ConflictException("Failed cancellation refund requires manual reconciliation");
            }
            requireReplayWindow(booking);
        } else {
            if (booking.getMolliePaymentId() == null) throw new ConflictException("Booking has no Mollie payment");
            if (booking.getCancellationRefundAmountEur() == null
                    || booking.getCancellationRefundAmountEur().compareTo(normalized) != 0) {
                throw new ConflictException("Refund must match the persisted cancellation obligation");
            }
            booking.setRefundStatus(RefundStatus.CREATING);
            booking.setRefundIdempotencyKey(UUID.randomUUID());
            booking.setRefundAmountEur(normalized);
            booking.setRefundRequestedAt(OffsetDateTime.now(clock));
            append(bookingId, BookingEventType.REFUND_REQUESTED, actorId, actorName, actorType,
                    Map.of("amount", normalized, "currency", "EUR", "reason", "booking_cancellation"));
        }
        return new Claim(bookingId, booking.getMolliePaymentId(), booking.getRefundAmountEur(),
                booking.getRefundIdempotencyKey());
    }

    @Transactional(readOnly = true)
    public Optional<Claim> resumableCancellationClaim(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        if (booking.getStatus() != BookingStatus.CANCELLED
                || booking.getRefundStatus() != RefundStatus.CREATING
                || booking.getRefundIdempotencyKey() == null
                || booking.getRefundAmountEur() == null
                || booking.getMolliePaymentId() == null) {
            return Optional.empty();
        }
        requireReplayWindow(booking);
        return Optional.of(new Claim(bookingId, booking.getMolliePaymentId(),
                booking.getRefundAmountEur(), booking.getRefundIdempotencyKey()));
    }

    @Transactional
    public Optional<Claim> claimLatePayment(UUID bookingId) {
        Booking booking = locked(bookingId);
        if ((booking.getStatus() != BookingStatus.EXPIRED
                && booking.getStatus() != BookingStatus.CANCELLED)
                || booking.getLatePaymentDetectedAt() == null) {
            throw new ConflictException("Booking is not an expired late payment");
        }
        if (booking.getMolliePaymentId() == null) throw new ConflictException("Booking has no Mollie payment");

        if (booking.getRefundStatus() == RefundStatus.REFUNDED
                || booking.getRefundStatus() == RefundStatus.QUEUED
                || booking.getRefundStatus() == RefundStatus.PENDING
                || booking.getRefundStatus() == RefundStatus.PROCESSING) {
            return Optional.empty();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (booking.getRefundStatus() == RefundStatus.CREATING) {
            requireReplayWindow(booking);
            if (booking.getRefundIdempotencyKey() == null || booking.getRefundAmountEur() == null
                    || booking.getRefundAmountEur().signum() <= 0
                    || booking.getRefundAmountEur().compareTo(booking.getTotalPaidEur()) > 0) {
                throw new ConflictException("Refund claim requires manual reconciliation");
            }
        } else {
            if (booking.getRefundStatus() != RefundStatus.NONE || booking.getRefundIdempotencyKey() != null) {
                throw new ConflictException("Failed late-payment refund requires manual reconciliation");
            }
            booking.setRefundStatus(RefundStatus.CREATING);
            booking.setRefundIdempotencyKey(UUID.randomUUID());
            booking.setRefundAmountEur(financials.money(booking.getTotalPaidEur()));
            booking.setRefundRequestedAt(now);
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                booking.setCancellationRefundPercentage(new BigDecimal("100.00"));
                booking.setCancellationRefundAmountEur(financials.money(booking.getTotalPaidEur()));
                booking.setCancellationRetainedAmountEur(financials.money(BigDecimal.ZERO));
                booking.setCommissionEur(financials.money(BigDecimal.ZERO));
                booking.setOwnerPayoutEur(financials.money(BigDecimal.ZERO));
                booking.setProviderCostResponsibility(
                        com.eema.backend.entity.enums.ProviderCostResponsibility.EEMA);
            }
            append(bookingId, BookingEventType.REFUND_REQUESTED, null, "SYSTEM", ActorType.SYSTEM,
                    Map.of("amount", booking.getRefundAmountEur(), "currency", "EUR", "reason", "late_payment"));
            append(bookingId, BookingEventType.LATE_PAYMENT_REFUND_REQUESTED, null, "SYSTEM", ActorType.SYSTEM,
                    Map.of("amount", booking.getRefundAmountEur(), "currency", "EUR"));
        }
        if (booking.getLatePaymentNotificationAt() == null) {
            booking.setLatePaymentNotificationAt(now);
            eventPublisher.publishEvent(new LatePaymentRefundInitiatedEvent(bookingId));
        }
        ownerPayoutService.recalculateForBooking(bookingId);
        return Optional.of(new Claim(bookingId, booking.getMolliePaymentId(), booking.getRefundAmountEur(),
                booking.getRefundIdempotencyKey()));
    }

    @Transactional
    public void recordProviderState(Claim claim, MollieRefundResponse refund) {
        Booking booking = locked(claim.bookingId());
        if (!Objects.equals(claim.paymentId(), booking.getMolliePaymentId())
                || claim.idempotencyKey() == null
                || !claim.idempotencyKey().equals(booking.getRefundIdempotencyKey())
                || claim.amount() == null || booking.getRefundAmountEur() == null
                || claim.amount().compareTo(booking.getRefundAmountEur()) != 0) {
            throw new IllegalStateException("Refund claim does not match booking");
        }
        validate(booking, refund);
        booking.setMollieRefundId(refund.id());
        applyStatus(booking, providerStatus(refund.status()));
    }

    @Transactional
    public void reconcile(String paymentId, MollieRefundResponse refund) {
        UUID bookingId = bookingRepository.findIdByMolliePaymentId(paymentId)
                .orElseThrow(() -> new NotFoundException("Booking not found for Mollie payment"));
        Booking booking = locked(bookingId);
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
        if (!refund.id().matches("re_[A-Za-z0-9_-]+") || booking.getRefundAmountEur() == null
                || MollieResponseValidation.eurAmount(refund.amount()).compareTo(booking.getRefundAmountEur()) != 0) {
            throw new IllegalStateException("Refund amount does not match booking");
        }
        providerStatus(refund.status());
    }

    private void requireReplayWindow(Booking booking) {
        if (booking.getRefundRequestedAt() == null
                || !OffsetDateTime.now(clock).isBefore(booking.getRefundRequestedAt().plus(IDEMPOTENCY_WINDOW))) {
            throw new ConflictException("Refund creation requires manual reconciliation before retry");
        }
    }

    private void applyStatus(Booking booking, RefundStatus target) {
        // Concurrent provider retrievals may arrive out of order; completion is terminal.
        if (!booking.getRefundStatus().canProgressTo(target)) return;
        RefundStatus previous = booking.getRefundStatus();
        booking.setRefundStatus(target);
        if (target == RefundStatus.REFUNDED) {
            boolean lateExpiredPayment = booking.getStatus() == BookingStatus.EXPIRED
                    && booking.getLatePaymentDetectedAt() != null;
            boolean cancellation = booking.getStatus() == BookingStatus.CANCELLED
                    && booking.getCancellationPolicyTier() != null;
            if (!lateExpiredPayment && !cancellation) {
                stateMachine.requireTransition(booking.getStatus(), BookingStatus.REFUNDED);
                booking.setStatus(BookingStatus.REFUNDED);
            }
            booking.setRefundedAt(OffsetDateTime.now(clock));
            if (!cancellation) {
                booking.setOwnerPayoutEur(BigDecimal.ZERO.setScale(2));
            }
            if (lateExpiredPayment) {
                append(booking.getId(), BookingEventType.LATE_PAYMENT_REFUND_COMPLETED,
                        null, "Mollie", ActorType.WEBHOOK, Map.of());
            }
            if (cancellation) {
                eventPublisher.publishEvent(new CancellationRefundCompletedEvent(booking.getId()));
            }
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("from", previous.name());
        payload.put("to", target.name());
        if (booking.getMollieRefundId() != null) payload.put("refundId", booking.getMollieRefundId());
        append(booking.getId(), target == RefundStatus.REFUNDED ? BookingEventType.PAYMENT_REFUNDED : BookingEventType.REFUND_STATUS_CHANGED,
                null, "Mollie", ActorType.WEBHOOK, payload);
        ownerPayoutService.recalculateForBooking(booking.getId());
    }

    private RefundStatus providerStatus(String status) {
        try {
            RefundStatus result = RefundStatus.valueOf(status.toUpperCase(java.util.Locale.ROOT));
            if (result == RefundStatus.NONE || result == RefundStatus.CREATING) throw new IllegalArgumentException();
            return result;
        }
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
