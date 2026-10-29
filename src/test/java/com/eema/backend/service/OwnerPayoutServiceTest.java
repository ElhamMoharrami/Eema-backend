package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.*;

class OwnerPayoutServiceTest {
    private final OwnerPayoutRepository payouts = mock(OwnerPayoutRepository.class);
    private final OwnerFinancialLiabilityRepository liabilities = mock(OwnerFinancialLiabilityRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final VenueRepository venues = mock(VenueRepository.class);
    private final BookingEventRepository events = mock(BookingEventRepository.class);
    private final UUID bookingId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final AtomicReference<OwnerPayout> stored = new AtomicReference<>();
    private final OwnerPayoutService service = new OwnerPayoutService(
            payouts, liabilities, bookings, venues, events, new BookingFinancials());
    private Booking booking;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(ownerId).role(UserRole.OWNER).build();
        when(venues.findById(venueId)).thenReturn(Optional.of(Venue.builder().id(venueId).owner(owner)
                .timezone("Europe/Amsterdam").build()));
        booking = Booking.builder().id(bookingId).venueId(venueId).status(BookingStatus.CONFIRMED)
                .shootDate(LocalDate.now().minusDays(1)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(14, 0))
                .venuePriceEur(new BigDecimal("1000.00")).commissionEur(new BigDecimal("200.00"))
                .mollieFeeEur(new BigDecimal("15.00")).mollieFeeReconciled(true)
                .ownerPayoutEur(new BigDecimal("785.00")).refundStatus(RefundStatus.NONE).build();
        when(bookings.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(payouts.findByBookingId(bookingId)).thenAnswer(i -> Optional.ofNullable(stored.get()));
        when(payouts.save(any())).thenAnswer(i -> {
            OwnerPayout value = i.getArgument(0);
            if (value.getId() == null) value.setId(UUID.randomUUID());
            stored.set(value);
            return value;
        });
        when(liabilities.findByBookingIdAndType(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void normalEconomicsBecomeReadyOnlyAfterEndAndAuthoritativeFee() {
        OwnerPayout ready = service.recalculateForBooking(bookingId);
        assertThat(ready.getGrossRetainedAmountEur()).isEqualByComparingTo("1000.00");
        assertThat(ready.getCommissionEur()).isEqualByComparingTo("200.00");
        assertThat(ready.getProviderCostEur()).isEqualByComparingTo("15.00");
        assertThat(ready.getPayoutAmountEur()).isEqualByComparingTo("785.00");
        assertThat(ready.getStatus()).isEqualTo(OwnerPayoutStatus.READY);

        stored.set(null);
        booking.setShootDate(LocalDate.now().plusDays(1));
        assertThat(service.recalculateForBooking(bookingId).getStatus()).isEqualTo(OwnerPayoutStatus.CALCULATED);
        stored.set(null);
        booking.setShootDate(LocalDate.now().minusDays(1));
        booking.setMollieFeeReconciled(false);
        booking.setMollieFeeEur(BigDecimal.ZERO);
        booking.setOwnerPayoutEur(new BigDecimal("800.00"));
        OwnerPayout held = service.recalculateForBooking(bookingId);
        assertThat(held.getStatus()).isEqualTo(OwnerPayoutStatus.HELD);
        assertThat(held.getHoldReason()).isEqualTo(OwnerPayoutHoldReason.WAITING_FOR_PROVIDER_COST);
    }

    @Test
    void scoutPartialCancellationUsesAuthoritativeSnapshot() {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationPolicyTier(CancellationPolicyTier.PARTIAL_REFUND);
        booking.setCancellationRetainedAmountEur(new BigDecimal("500.00"));
        booking.setCommissionEur(new BigDecimal("100.00"));
        booking.setOwnerPayoutEur(new BigDecimal("385.00"));
        booking.setProviderCostResponsibility(ProviderCostResponsibility.OWNER_SETTLEMENT);
        OwnerPayout payout = service.recalculateForBooking(bookingId);
        assertThat(payout.getGrossRetainedAmountEur()).isEqualByComparingTo("500.00");
        assertThat(payout.getPayoutAmountEur()).isEqualByComparingTo("385.00");
    }

    @Test
    void ownerCancellationCreatesProviderCostLiabilityWithoutNegativePayout() {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationPolicyTier(CancellationPolicyTier.OWNER_FULL_REFUND);
        booking.setCancellationRetainedAmountEur(BigDecimal.ZERO);
        booking.setCommissionEur(BigDecimal.ZERO);
        booking.setOwnerPayoutEur(BigDecimal.ZERO);
        booking.setProviderCostResponsibility(ProviderCostResponsibility.OWNER);
        booking.setCancellationProviderCostEur(new BigDecimal("15.00"));
        booking.setCancellationProviderCostReconciled(true);
        OwnerPayout payout = service.recalculateForBooking(bookingId);
        assertThat(payout.getStatus()).isEqualTo(OwnerPayoutStatus.CANCELLED);
        assertThat(payout.getPayoutAmountEur()).isZero();
        verify(liabilities).save(argThat(l -> l.getType() == OwnerFinancialLiabilityType.PROVIDER_COST
                && l.getAmountEur().compareTo(new BigDecimal("15.00")) == 0));
    }

    @Test
    void adminCancellationIsCancelledWithoutOwnerLiability() {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationPolicyTier(CancellationPolicyTier.ADMIN_FULL_REFUND);
        booking.setCancellationRetainedAmountEur(BigDecimal.ZERO);
        booking.setCommissionEur(BigDecimal.ZERO);
        booking.setOwnerPayoutEur(BigDecimal.ZERO);
        booking.setProviderCostResponsibility(ProviderCostResponsibility.EEMA);
        assertThat(service.recalculateForBooking(bookingId).getStatus()).isEqualTo(OwnerPayoutStatus.CANCELLED);
        verify(liabilities, never()).save(any());
    }

    @Test
    void latePaymentCancelsAnyDefensivePayoutRecord() {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setLatePaymentDetectedAt(OffsetDateTime.now());
        assertThat(service.recalculateForBooking(bookingId).getStatus()).isEqualTo(OwnerPayoutStatus.CANCELLED);
        assertThat(stored.get().getPayoutAmountEur()).isZero();
    }

    @Test
    void paidHistoryIsImmutableAndReversalCreatesRecoveryLiability() {
        OwnerPayout paid = service.recalculateForBooking(bookingId);
        paid.setStatus(OwnerPayoutStatus.PAID);
        paid.setPaidAt(OffsetDateTime.now());
        paid.setExternalTransferReference("bank-1");
        booking.setStatus(BookingStatus.REFUNDED);
        booking.setOwnerPayoutEur(BigDecimal.ZERO);

        OwnerPayout result = service.recalculateForBooking(bookingId);
        assertThat(result.getPayoutAmountEur()).isEqualByComparingTo("785.00");
        assertThat(result.getStatus()).isEqualTo(OwnerPayoutStatus.PAID);
        verify(liabilities).save(argThat(l -> l.getType() == OwnerFinancialLiabilityType.OWNER_PAYOUT_RECOVERY
                && l.getAmountEur().compareTo(new BigDecimal("785.00")) == 0));
    }

    @Test
    void markPaidIsIdempotentOnlyForSameReference() {
        OwnerPayout ready = service.recalculateForBooking(bookingId);
        when(payouts.findByIdForUpdate(ready.getId())).thenReturn(Optional.of(ready));
        User admin = User.builder().id(UUID.randomUUID()).email("admin@example.test").role(UserRole.ADMIN).build();
        assertThat(service.markPaid(ready.getId(), " transfer-42 ", admin).status()).isEqualTo(OwnerPayoutStatus.PAID);
        assertThat(service.markPaid(ready.getId(), "transfer-42", admin).externalTransferReference()).isEqualTo("transfer-42");
        assertThatThrownBy(() -> service.markPaid(ready.getId(), "different", admin)).isInstanceOf(ConflictException.class);
    }

    @Test
    void adminListForwardsAllFiltersAndCapsPageSize() {
        UUID venueFilter = UUID.randomUUID();
        when(payouts.search(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        service.list(OwnerPayoutStatus.HELD, ownerId, bookingId, venueFilter, PageRequest.of(0, 500));
        verify(payouts).search(eq(OwnerPayoutStatus.HELD), eq(ownerId), eq(bookingId), eq(venueFilter),
                argThat(p -> p.getPageSize() == 100));
    }
}
