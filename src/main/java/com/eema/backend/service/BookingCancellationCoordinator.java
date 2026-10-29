package com.eema.backend.service;

import com.eema.backend.dto.response.CancellationPreviewDto;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.BookingEvent;
import com.eema.backend.entity.BookingEventPayload;
import com.eema.backend.entity.User;
import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.*;
import com.eema.backend.event.BookingCancelledEvent;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.repository.BookingEventRepository;
import com.eema.backend.repository.BookingRepository;
import com.eema.backend.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingCancellationCoordinator {
    private final BookingRepository bookings;
    private final VenueRepository venues;
    private final BookingEventRepository events;
    private final BookingCancellationPolicy policy;
    private final BookingFinancials financials;
    private final BookingStateMachine stateMachine;
    private final RefundCoordinator refunds;
    private final ApplicationEventPublisher publisher;
    private final OwnerPayoutService ownerPayoutService;

    @Transactional
    public CancellationResult cancel(UUID bookingId, User actor, String reason, OffsetDateTime now) {
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        ActorType actorType = actorType(actor);
        authorize(actor, booking);
        validateState(actorType, booking);
        requireReason(actorType, reason);

        BookingCancellationPolicy.CancellationDecision decision = policy.evaluate(booking, actorType, now);
        stateMachine.requireTransition(booking.getStatus(), BookingStatus.CANCELLED);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setCancelledByActorType(actorType);
        booking.setCancellationPolicyTier(decision.policyTier());
        booking.setCancellationRefundPercentage(decision.refundPercentage());
        booking.setCancellationRefundAmountEur(decision.refundAmountEur());
        booking.setCancellationRetainedAmountEur(decision.retainedAmountEur());
        booking.setOwnerCancellationSettlementEur(decision.ownerGrossSettlementEur());
        booking.setCancellationReason(trimToNull(reason));
        booking.setProviderCostResponsibility(decision.providerCostResponsibility());
        booking.setCommissionEur(decision.eemaCommissionAfterCancellationEur());
        booking.setCancellationProviderCostEur(booking.isMollieFeeReconciled()
                ? financials.money(booking.getMollieFeeEur()) : null);
        booking.setCancellationProviderCostReconciled(booking.isMollieFeeReconciled());
        booking.setOwnerPayoutEur(ownerEntitlement(booking, decision));

        Map<String, Object> payload = new HashMap<>();
        payload.put("policyTier", decision.policyTier().name());
        payload.put("refundPercentage", decision.refundPercentage());
        payload.put("refundAmountEur", decision.refundAmountEur());
        payload.put("retainedAmountEur", decision.retainedAmountEur());
        if (booking.getCancellationReason() != null) payload.put("reason", booking.getCancellationReason());
        events.save(BookingEvent.builder().bookingId(bookingId).eventType(eventType(actorType))
                .actorId(actor.getId()).actorName(actorName(actor)).actorType(actorType)
                .payload(new BookingEventPayload(payload)).build());

        RefundCoordinator.Claim refundClaim = decision.refundable()
                ? refunds.claimCancellation(bookingId, decision.refundAmountEur(), actor.getId(),
                actorName(actor), actorType)
                : null;
        ownerPayoutService.recalculateForBooking(bookingId);
        publisher.publishEvent(new BookingCancelledEvent(bookingId));
        return new CancellationResult(bookingId, refundClaim);
    }

    @Transactional(readOnly = true)
    public CancellationPreviewDto preview(UUID bookingId, User actor, OffsetDateTime now) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        authorize(actor, booking);
        ActorType actorType = actorType(actor);
        boolean canCancel = isStateAllowed(actorType, booking.getStatus());
        if (!canCancel) {
            return new CancellationPreviewDto(bookingId, null, BigDecimal.ZERO.setScale(2),
                    BigDecimal.ZERO.setScale(2), booking.getTotalPaidEur(), null, false);
        }
        BookingCancellationPolicy.CancellationDecision decision = policy.evaluate(booking, actorType, now);
        return new CancellationPreviewDto(bookingId, decision.policyTier(), decision.refundPercentage(),
                decision.refundAmountEur(), decision.retainedAmountEur(), deadline(booking, decision), true);
    }

    private BigDecimal ownerEntitlement(Booking booking, BookingCancellationPolicy.CancellationDecision decision) {
        if (decision.providerCostResponsibility() != ProviderCostResponsibility.OWNER_SETTLEMENT) {
            return financials.money(BigDecimal.ZERO);
        }
        BigDecimal knownCost = booking.isMollieFeeReconciled()
                ? financials.money(booking.getMollieFeeEur()) : financials.money(BigDecimal.ZERO);
        return financials.money(decision.ownerGrossSettlementEur().subtract(knownCost).max(BigDecimal.ZERO));
    }

    private OffsetDateTime deadline(Booking booking, BookingCancellationPolicy.CancellationDecision decision) {
        return switch (decision.policyTier()) {
            case GRACE_PERIOD -> booking.getGracePeriodEndsAt();
            case FLEXIBLE_FULL_REFUND -> booking.getFullRefundUntil();
            case PARTIAL_REFUND -> booking.getPartialRefundUntil();
            default -> null;
        };
    }

    private void validateState(ActorType actor, Booking booking) {
        if (!isStateAllowed(actor, booking.getStatus())) {
            throw new ConflictException("Booking cannot be cancelled in its current state");
        }
    }

    private boolean isStateAllowed(ActorType actor, BookingStatus status) {
        return actor == ActorType.SCOUT
                ? status == BookingStatus.REQUESTED || status == BookingStatus.AWAITING_PAYMENT
                || status == BookingStatus.CONFIRMED
                : (actor == ActorType.OWNER || actor == ActorType.ADMIN) && status == BookingStatus.CONFIRMED;
    }

    private void authorize(User actor, Booking booking) {
        if (actor.getRole() == UserRole.ADMIN) return;
        if (actor.getRole() == UserRole.SCOUT) {
            if (actor.getId().equals(booking.getScoutId())) return;
            throw new ForbiddenException("Scout cannot cancel another scout's booking");
        }
        Venue venue = venues.findById(booking.getVenueId())
                .orElseThrow(() -> new NotFoundException("Venue not found"));
        if (venue.getOwner() != null && actor.getId().equals(venue.getOwner().getId())) return;
        throw new ForbiddenException("Owner cannot cancel a booking for another owner's venue");
    }

    private void requireReason(ActorType actor, String reason) {
        if ((actor == ActorType.OWNER || actor == ActorType.ADMIN) && trimToNull(reason) == null) {
            throw new ConflictException("Owner and admin cancellations require a reason");
        }
    }

    private ActorType actorType(User user) {
        return switch (user.getRole()) {
            case SCOUT -> ActorType.SCOUT;
            case OWNER -> ActorType.OWNER;
            case ADMIN -> ActorType.ADMIN;
        };
    }

    private BookingEventType eventType(ActorType actor) {
        return switch (actor) {
            case SCOUT -> BookingEventType.SCOUT_CANCELLED;
            case OWNER -> BookingEventType.OWNER_CANCELLED;
            case ADMIN -> BookingEventType.ADMIN_CANCELLED;
            default -> throw new IllegalArgumentException("Unsupported cancellation actor");
        };
    }

    private String actorName(User actor) {
        return actor.getFullName() == null || actor.getFullName().isBlank()
                ? actor.getEmail() : actor.getFullName();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CancellationResult(UUID bookingId, RefundCoordinator.Claim refundClaim) {
    }
}
