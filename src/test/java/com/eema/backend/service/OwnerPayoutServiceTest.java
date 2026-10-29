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
    private final OwnerPayoutDestinationRepository destinations = mock(OwnerPayoutDestinationRepository.class);
    private final MarketplaceTrustAuditService trustAudit = mock(MarketplaceTrustAuditService.class);
    private final UUID bookingId = UUID.randomUUID();
    private final UUID venueId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final AtomicReference<OwnerPayout> stored = new AtomicReference<>();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC);
    private final OwnerPayoutService service = new OwnerPayoutService(
            payouts, liabilities, bookings, venues, events, new BookingFinancials(), destinations, trustAudit, clock);
    private Booking booking;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(ownerId).role(UserRole.OWNER).build();
        when(venues.findById(venueId)).thenReturn(Optional.of(Venue.builder().id(venueId).owner(owner)
                .timezone("Europe/Amsterdam").build()));
        booking = Booking.builder().id(bookingId).venueId(venueId).status(BookingStatus.CONFIRMED)
                .shootDate(LocalDate.now(clock).minusDays(1)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(14, 0))
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
        when(destinations.findByOwnerIdAndStatus(ownerId, PayoutDestinationStatus.VERIFIED)).thenReturn(Optional.of(
                OwnerPayoutDestination.builder().id(UUID.randomUUID()).ownerId(ownerId).status(PayoutDestinationStatus.VERIFIED).ibanLast4("4300").build()));
    }

    @Test
    void otherwiseEligiblePayoutWaitsForVerifiedDestination() {
        when(destinations.findByOwnerIdAndStatus(ownerId, PayoutDestinationStatus.VERIFIED)).thenReturn(Optional.empty());
        OwnerPayout payout = service.recalculateForBooking(bookingId);
        assertThat(payout.getStatus()).isEqualTo(OwnerPayoutStatus.HELD);
        assertThat(payout.getHoldReason()).isEqualTo(OwnerPayoutHoldReason.WAITING_FOR_PAYOUT_DESTINATION);
        assertThat(payout.getPayoutDestination()).isNull();
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
        booking.setShootDate(LocalDate.now(clock).plusDays(1));
        assertThat(service.recalculateForBooking(bookingId).getStatus()).isEqualTo(OwnerPayoutStatus.CALCULATED);
        stored.set(null);
        booking.setShootDate(LocalDate.now(clock).minusDays(1));
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
        booking.setLatePaymentDetectedAt(OffsetDateTime.now(clock));
        assertThat(service.recalculateForBooking(bookingId).getStatus()).isEqualTo(OwnerPayoutStatus.CANCELLED);
        assertThat(stored.get().getPayoutAmountEur()).isZero();
    }

    @Test
    void paidHistoryIsImmutableAndReversalCreatesRecoveryLiability() {
        OwnerPayout paid = service.recalculateForBooking(bookingId);
        paid.setStatus(OwnerPayoutStatus.PAID);
        paid.setPaidAt(OffsetDateTime.now(clock));
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
        when(payouts.findBookingIdById(ready.getId())).thenReturn(Optional.of(bookingId));
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

    @Test
    void ownerHistoryIsScopedAndDoesNotExposeTransferReference() {
        OwnerPayout payout = OwnerPayout.builder().id(UUID.randomUUID()).ownerId(ownerId).bookingId(bookingId)
                .venueId(venueId).status(OwnerPayoutStatus.PAID).externalTransferReference("private-bank-ref").build();
        when(payouts.findOwnerPayouts(eq(ownerId), any())).thenReturn(new PageImpl<>(List.of(payout)));
        var result = service.listForOwner(ownerId, PageRequest.of(0, 20)).getContent().getFirst();
        assertThat(result.id()).isEqualTo(payout.getId());
        assertThat(result.statementAvailable()).isTrue();
        assertThat(Arrays.stream(result.getClass().getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("externalTransferReference", "ownerId", "updatedAt");
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "0,CREATING,false,true,false,CANCELLED,",
        "785,CREATING,false,true,false,HELD,REFUND_IN_PROGRESS",
        "785,QUEUED,true,false,true,HELD,REFUND_IN_PROGRESS",
        "785,PENDING,true,false,true,HELD,REFUND_IN_PROGRESS",
        "785,PROCESSING,true,false,true,HELD,REFUND_IN_PROGRESS",
        "785,NONE,false,true,false,HELD,WAITING_FOR_PROVIDER_COST",
        "785,NONE,false,false,true,HELD,WAITING_FOR_PROVIDER_COST",
        "785,NONE,true,true,false,CALCULATED,WAITING_FOR_BOOKING_END",
        "785,NONE,true,true,true,CALCULATED,WAITING_FOR_BOOKING_END",
        "785,NONE,true,false,false,HELD,WAITING_FOR_PAYOUT_DESTINATION",
        "785,NONE,true,false,true,READY,",
        "785,FAILED,true,false,true,READY,",
        "785,CANCELED,true,false,true,READY,"
    })
    void combinedBlockersHaveExplicitPrecedence(String amount, RefundStatus refund, boolean fee,
            boolean future, boolean destination, OwnerPayoutStatus expected, OwnerPayoutHoldReason reason) {
        booking.setOwnerPayoutEur(new BigDecimal(amount));booking.setRefundStatus(refund);booking.setMollieFeeReconciled(fee);
        booking.setShootDate(LocalDate.now(clock).plusDays(future?1:-1));
        if(!destination)when(destinations.findByOwnerIdAndStatus(ownerId,PayoutDestinationStatus.VERIFIED)).thenReturn(Optional.empty());
        var result=service.recalculateForBooking(bookingId);assertThat(result.getStatus()).isEqualTo(expected);assertThat(result.getHoldReason()).isEqualTo(reason);
    }
    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> endBoundaries(){
        return java.util.stream.Stream.of("2026-01-15T13:00:00Z","2026-03-29T12:00:00Z","2026-10-25T13:00:00Z")
                .flatMap(end->java.util.stream.Stream.of(-1,0,1).map(offset->org.junit.jupiter.params.provider.Arguments.of(end,offset)));
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("endBoundaries")
    void readinessUsesVenueEndInstantIncludingBothDstTransitions(String end, int offset){
        Instant eligible=Instant.parse(end);Clock fixed=Clock.fixed(eligible.plusSeconds(offset),ZoneOffset.UTC);
        var subject=new OwnerPayoutService(payouts,liabilities,bookings,venues,events,new BookingFinancials(),destinations,trustAudit,fixed);
        booking.setShootDate(eligible.atZone(ZoneId.of("Europe/Amsterdam")).toLocalDate());booking.setEndTime(LocalTime.of(14,0));
        var result=subject.recalculateForBooking(bookingId);assertThat(result.getEligibleAt().toInstant()).isEqualTo(eligible);
        assertThat(result.getStatus()).isEqualTo(offset<0?OwnerPayoutStatus.CALCULATED:OwnerPayoutStatus.READY);
        if(offset>=0){assertThat(result.getReadyAt().toInstant()).isEqualTo(fixed.instant());subject.recalculateForBooking(bookingId);verify(trustAudit,times(1)).append(eq(TrustSubjectType.PAYOUT_DESTINATION),any(),eq(TrustEventType.PAYOUT_DESTINATION_SNAPSHOTTED),isNull(),eq(ActorType.SYSTEM),anyString());}
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=BookingStatus.class,names={"REQUESTED","AWAITING_PAYMENT","DECLINED","EXPIRED","UNAVAILABLE","REFUNDED"})
    void noNewPayoutForIneligibleBooking(BookingStatus status){booking.setStatus(status);assertThat(service.recalculateForBooking(bookingId)).isNull();verify(payouts,never()).save(any());}

    @Test
    void paidOwnerSettlementDoesNotChargeAlreadyAppliedProviderCostAgain() {
        booking.setProviderCostResponsibility(ProviderCostResponsibility.OWNER);
        booking.setCancellationProviderCostReconciled(true);
        booking.setCancellationProviderCostEur(new BigDecimal("15.00"));
        OwnerPayout paid = service.recalculateForBooking(bookingId);
        paid.setStatus(OwnerPayoutStatus.PAID);
        clearInvocations(liabilities);
        service.recalculateForBooking(bookingId);
        verify(liabilities, never()).save(any());
        assertThat(paid.getPayoutAmountEur()).isEqualByComparingTo("785.00");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=ProviderCostResponsibility.class, names={"NONE","EEMA"})
    void paidPlatformBorneCostCannotBecomeLimitedRecourseRecovery(ProviderCostResponsibility originalResponsibility) {
        booking.setProviderCostResponsibility(originalResponsibility);
        booking.setOwnerPayoutEur(new BigDecimal("800.00"));
        OwnerPayout paid = service.recalculateForBooking(bookingId);
        paid.setStatus(OwnerPayoutStatus.PAID);
        booking.setProviderCostResponsibility(ProviderCostResponsibility.OWNER_SETTLEMENT);
        booking.setMollieFeeEur(new BigDecimal("25.00"));
        booking.setOwnerPayoutEur(new BigDecimal("775.00"));
        clearInvocations(liabilities);
        service.recalculateForBooking(bookingId);
        verify(liabilities, never()).save(any());
        assertThat(paid.getProviderCostEur()).isEqualByComparingTo("15.00");
        assertThat(paid.getPayoutAmountEur()).isEqualByComparingTo("800.00");
    }
}
