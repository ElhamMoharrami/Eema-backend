package com.eema.backend.service;

import com.eema.backend.dto.response.OwnerPayoutDto;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OwnerPayoutService {
    private static final Set<RefundStatus> ACTIVE_REFUNDS = EnumSet.of(
            RefundStatus.CREATING, RefundStatus.QUEUED, RefundStatus.PENDING, RefundStatus.PROCESSING);
    private final OwnerPayoutRepository payouts;
    private final OwnerFinancialLiabilityRepository liabilities;
    private final BookingRepository bookings;
    private final VenueRepository venues;
    private final BookingEventRepository events;
    private final BookingFinancials financials;

    @Transactional
    public OwnerPayout recalculateForBooking(UUID bookingId) {
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        return recalculate(booking, OffsetDateTime.now());
    }

    @Transactional
    public OwnerPayout evaluateReadiness(UUID payoutId) {
        OwnerPayout payout = payouts.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new NotFoundException("Owner payout not found: " + payoutId));
        Booking booking = bookings.findByIdForUpdate(payout.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found: " + payout.getBookingId()));
        return recalculate(booking, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public Page<OwnerPayoutDto> list(OwnerPayoutStatus status, UUID ownerId, UUID bookingId,
                                     UUID venueId, Pageable pageable) {
        return payouts.search(status, ownerId, bookingId, venueId, AdminPagePolicy.bounded(pageable)).map(this::dto);
    }

    @Transactional(readOnly = true)
    public OwnerPayoutDto get(UUID id) {
        return dto(payouts.findById(id).orElseThrow(() -> new NotFoundException("Owner payout not found: " + id)));
    }

    @Transactional(readOnly = true)
    public OwnerPayoutStatus statusForBooking(UUID bookingId) {
        return payouts.findByBookingId(bookingId).map(OwnerPayout::getStatus).orElse(null);
    }

    @Transactional
    public OwnerPayoutDto markPaid(UUID id, String reference, User admin) {
        if (admin == null || admin.getRole() != UserRole.ADMIN) throw new ForbiddenException("Only administrators can mark payouts paid");
        String normalized = reference == null ? null : reference.trim();
        if (normalized == null || normalized.isEmpty()) throw new BadRequestException("External transfer reference is required");
        OwnerPayout payout = payouts.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Owner payout not found: " + id));
        if (payout.getStatus() == OwnerPayoutStatus.PAID) {
            if (normalized.equals(payout.getExternalTransferReference())) return dto(payout);
            throw new ConflictException("Payout was already marked paid with a different reference");
        }
        if (payout.getStatus() != OwnerPayoutStatus.READY) throw new ConflictException("Only READY payouts can be marked paid");
        payout.setStatus(OwnerPayoutStatus.PAID);
        payout.setPaidAt(OffsetDateTime.now());
        payout.setExternalTransferReference(normalized);
        payout.setHoldReason(null);
        append(payout.getBookingId(), BookingEventType.PAYOUT_MARKED_PAID, admin.getId(), actorName(admin),
                ActorType.ADMIN, Map.of("payoutId", payout.getId(), "externalTransferReference", normalized));
        return dto(payouts.save(payout));
    }

    private OwnerPayout recalculate(Booking booking, OffsetDateTime now) {
        Optional<OwnerPayout> existing = payouts.findByBookingId(booking.getId());
        if (existing.isEmpty() && booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.CANCELLED) return null;
        Venue venue = venues.findById(booking.getVenueId()).orElseThrow(() -> new NotFoundException("Venue not found"));
        UUID ownerId = venue.getOwner().getId();
        OffsetDateTime eligibleAt = booking.getShootDate().atTime(booking.getEndTime())
                .atZone(ZoneId.of(venue.getTimezone())).toOffsetDateTime();

        Economics economics = economics(booking);
        if (existing.isPresent() && existing.get().getStatus() == OwnerPayoutStatus.PAID) {
            OwnerPayout paid = existing.get();
            if (paid.getPayoutAmountEur().compareTo(economics.payout()) != 0
                    || booking.getStatus() == BookingStatus.REFUNDED) {
                BigDecimal recovery = financials.money(paid.getPayoutAmountEur().subtract(economics.payout()).max(BigDecimal.ZERO));
                createLiability(ownerId, booking.getId(), OwnerFinancialLiabilityType.OWNER_PAYOUT_RECOVERY,
                        recovery, "Financial reversal occurred after the owner payout was recorded as paid");
            }
            return paid;
        }

        OwnerPayout payout = existing.orElseGet(() -> OwnerPayout.builder()
                .bookingId(booking.getId()).ownerId(ownerId).venueId(booking.getVenueId()).currency("EUR").build());
        OwnerPayoutStatus previous = payout.getStatus();
        payout.setGrossRetainedAmountEur(economics.gross());
        payout.setCommissionEur(economics.commission());
        payout.setProviderCostEur(economics.providerCost());
        payout.setPayoutAmountEur(economics.payout());
        payout.setEligibleAt(eligibleAt);

        State state = state(booking, economics.payout(), eligibleAt, now);
        payout.setStatus(state.status());
        payout.setHoldReason(state.holdReason());
        if (state.status() == OwnerPayoutStatus.READY && previous != OwnerPayoutStatus.READY) payout.setReadyAt(now);
        if (state.status() != OwnerPayoutStatus.READY) payout.setReadyAt(null);
        payout = payouts.save(payout);

        if (previous == null) append(booking.getId(), BookingEventType.PAYOUT_CALCULATED, null, "SYSTEM", ActorType.SYSTEM,
                Map.of("payoutId", payout.getId(), "amountEur", payout.getPayoutAmountEur()));
        if (previous != payout.getStatus()) {
            if (payout.getStatus() == OwnerPayoutStatus.READY) append(booking.getId(), BookingEventType.PAYOUT_READY,
                    null, "SYSTEM", ActorType.SYSTEM, Map.of("payoutId", payout.getId()));
            if (payout.getStatus() == OwnerPayoutStatus.CANCELLED) append(booking.getId(), BookingEventType.PAYOUT_CANCELLED,
                    null, "SYSTEM", ActorType.SYSTEM, Map.of("payoutId", payout.getId()));
        }
        if (booking.getProviderCostResponsibility() == ProviderCostResponsibility.OWNER
                && booking.isCancellationProviderCostReconciled()) {
            createLiability(ownerId, booking.getId(), OwnerFinancialLiabilityType.PROVIDER_COST,
                    financials.money(booking.getCancellationProviderCostEur()),
                    "Provider cost owed after owner-caused cancellation");
        }
        return payout;
    }

    private Economics economics(Booking b) {
        boolean cancelled = b.getCancellationPolicyTier() != null || b.getStatus() == BookingStatus.CANCELLED;
        BigDecimal gross = cancelled && b.getCancellationRetainedAmountEur() != null
                ? financials.money(b.getCancellationRetainedAmountEur()) : financials.money(b.getVenuePriceEur());
        BigDecimal commission = financials.money(b.getCommissionEur());
        BigDecimal provider = b.isMollieFeeReconciled() && b.getMollieFeeEur() != null
                ? financials.money(b.getMollieFeeEur()) : financials.money(BigDecimal.ZERO);
        BigDecimal amount = b.getOwnerPayoutEur() == null ? BigDecimal.ZERO : b.getOwnerPayoutEur();
        if (b.getStatus() == BookingStatus.REFUNDED
                || b.getProviderCostResponsibility() == ProviderCostResponsibility.OWNER
                || b.getProviderCostResponsibility() == ProviderCostResponsibility.EEMA
                || b.getLatePaymentDetectedAt() != null) amount = BigDecimal.ZERO;
        return new Economics(gross, commission, provider, financials.money(amount));
    }

    private State state(Booking b, BigDecimal amount, OffsetDateTime eligibleAt, OffsetDateTime now) {
        if (amount.signum() == 0 || b.getStatus() == BookingStatus.REFUNDED || b.getLatePaymentDetectedAt() != null)
            return new State(OwnerPayoutStatus.CANCELLED, null);
        if (ACTIVE_REFUNDS.contains(b.getRefundStatus()))
            return new State(OwnerPayoutStatus.HELD, OwnerPayoutHoldReason.REFUND_IN_PROGRESS);
        if (!b.isMollieFeeReconciled())
            return new State(OwnerPayoutStatus.HELD, OwnerPayoutHoldReason.WAITING_FOR_PROVIDER_COST);
        if (now.isBefore(eligibleAt))
            return new State(OwnerPayoutStatus.CALCULATED, OwnerPayoutHoldReason.WAITING_FOR_BOOKING_END);
        return new State(OwnerPayoutStatus.READY, null);
    }

    private void createLiability(UUID ownerId, UUID bookingId, OwnerFinancialLiabilityType type,
                                 BigDecimal amount, String note) {
        if (amount == null || amount.signum() <= 0 || liabilities.findByBookingIdAndType(bookingId, type).isPresent()) return;
        liabilities.save(OwnerFinancialLiability.builder().ownerId(ownerId).bookingId(bookingId).type(type)
                .amountEur(amount).status(OwnerFinancialLiabilityStatus.OPEN).note(note).build());
        append(bookingId, BookingEventType.OWNER_LIABILITY_CREATED, null, "SYSTEM", ActorType.SYSTEM,
                Map.of("type", type.name(), "amountEur", amount));
    }

    private void append(UUID bookingId, BookingEventType type, UUID actorId, String actorName,
                        ActorType actorType, Map<String, Object> data) {
        events.save(BookingEvent.builder().bookingId(bookingId).eventType(type).actorId(actorId)
                .actorName(actorName).actorType(actorType).payload(new BookingEventPayload(data)).build());
    }
    private String actorName(User user) { return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName(); }
    public OwnerPayoutDto dto(OwnerPayout p) { return new OwnerPayoutDto(p.getId(), p.getBookingId(), p.getOwnerId(), p.getVenueId(), p.getCurrency(), p.getGrossRetainedAmountEur(), p.getCommissionEur(), p.getProviderCostEur(), p.getPayoutAmountEur(), p.getStatus(), p.getHoldReason(), p.getEligibleAt(), p.getReadyAt(), p.getPaidAt(), p.getExternalTransferReference(), p.getCreatedAt(), p.getUpdatedAt()); }
    private record Economics(BigDecimal gross, BigDecimal commission, BigDecimal providerCost, BigDecimal payout) {}
    private record State(OwnerPayoutStatus status, OwnerPayoutHoldReason holdReason) {}
}
