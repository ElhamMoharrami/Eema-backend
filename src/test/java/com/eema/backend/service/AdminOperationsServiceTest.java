package com.eema.backend.service;

import com.eema.backend.dto.response.OperationalExceptionDto;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.time.OffsetDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;

class AdminOperationsServiceTest {
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingEventRepository events = mock(BookingEventRepository.class);
    private final OwnerPayoutRepository payouts = mock(OwnerPayoutRepository.class);
    private final OwnerFinancialLiabilityRepository liabilities = mock(OwnerFinancialLiabilityRepository.class);
    private final OwnerPayoutService payoutService = mock(OwnerPayoutService.class);
    private final OwnerFinancialLiabilityService liabilityService = mock(OwnerFinancialLiabilityService.class);
    private final AdminOperationsService service = new AdminOperationsService(bookings, events, payouts, liabilities,
            payoutService, liabilityService, new OperationalThresholds(60, 1440, 1440, 72));

    @BeforeEach
    void emptyQueues() {
        when(payouts.findByStatusOrderByCreatedAtAsc(any(), any())).thenReturn(List.of());
        when(payouts.findByStatusAndReadyAtBeforeOrderByReadyAtAsc(any(), any(), any())).thenReturn(List.of());
        when(liabilities.findByStatusOrderByCreatedAtAsc(any(), any())).thenReturn(List.of());
        when(bookings.findStuckRefunds(anyList(), any(), any())).thenReturn(List.of());
        when(bookings.findStuckPaymentCreations(any(), any(), any())).thenReturn(List.of());
        when(bookings.findOldUnreconciledMollieFees(any(), any())).thenReturn(List.of());
        when(bookings.findLatePaymentRefundsInProgress(anyList(), any())).thenReturn(List.of());
    }

    @Test
    void eventTimelineIsChronologicalPagedAndRedactsUnknownSensitiveKeys() {
        UUID bookingId = UUID.randomUUID();
        when(bookings.findById(bookingId)).thenReturn(Optional.of(Booking.builder().id(bookingId).build()));
        BookingEvent event = BookingEvent.builder().id(UUID.randomUUID()).bookingId(bookingId)
                .eventType(BookingEventType.PAYMENT_SUCCEEDED).actorType(ActorType.SYSTEM).actorName("SYSTEM")
                .payload(new BookingEventPayload(new LinkedHashMap<>(Map.of(
                        "amount", "100.00", "apiKey", "secret", "rawMollieResponse", "hidden")))).build();
        when(events.findPageByBookingIdOrderByCreatedAtAsc(eq(bookingId), any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        var page = service.events(bookingId, PageRequest.of(0, 250));
        assertThat(page.getContent().getFirst().payload()).containsEntry("amount", "100.00")
                .doesNotContainKeys("apiKey", "rawMollieResponse");
        verify(events).findPageByBookingIdOrderByCreatedAtAsc(eq(bookingId),
                argThat(p -> p.getPageSize() == 100));
    }

    @Test
    void exceptionQueueAggregatesAllRequiredOperationalCategories() {
        UUID bookingId = UUID.randomUUID();
        OffsetDateTime old = OffsetDateTime.now().minusDays(10);
        OwnerPayout held = OwnerPayout.builder().id(UUID.randomUUID()).bookingId(bookingId)
                .status(OwnerPayoutStatus.HELD).holdReason(OwnerPayoutHoldReason.WAITING_FOR_PROVIDER_COST).updatedAt(old).build();
        OwnerPayout overdue = OwnerPayout.builder().id(UUID.randomUUID()).bookingId(bookingId)
                .status(OwnerPayoutStatus.READY).readyAt(old).build();
        OwnerFinancialLiability liability = OwnerFinancialLiability.builder().id(UUID.randomUUID()).bookingId(bookingId)
                .status(OwnerFinancialLiabilityStatus.OPEN).createdAt(old).build();
        Booking booking = Booking.builder().id(bookingId).refundRequestedAt(old).paymentCreationStartedAt(old)
                .paidAt(old).latePaymentDetectedAt(old).build();
        when(payouts.findByStatusOrderByCreatedAtAsc(eq(OwnerPayoutStatus.HELD), any())).thenReturn(List.of(held));
        when(payouts.findByStatusAndReadyAtBeforeOrderByReadyAtAsc(eq(OwnerPayoutStatus.READY), any(), any())).thenReturn(List.of(overdue));
        when(liabilities.findByStatusOrderByCreatedAtAsc(eq(OwnerFinancialLiabilityStatus.OPEN), any())).thenReturn(List.of(liability));
        when(bookings.findStuckRefunds(anyList(), any(), any())).thenReturn(List.of(booking));
        when(bookings.findStuckPaymentCreations(eq(PaymentCreationStatus.CREATING), any(), any())).thenReturn(List.of(booking));
        when(bookings.findOldUnreconciledMollieFees(any(), any())).thenReturn(List.of(booking));
        when(bookings.findLatePaymentRefundsInProgress(anyList(), any())).thenReturn(List.of(booking));

        Set<OperationalExceptionType> types = service.exceptions(PageRequest.of(0, 100)).stream()
                .map(OperationalExceptionDto::type).collect(java.util.stream.Collectors.toSet());
        assertThat(types).containsExactlyInAnyOrder(OperationalExceptionType.values());
    }

    @Test
    void bookingSearchForwardsControlledFiltersAndCapsPageSize() {
        UUID id = UUID.randomUUID();
        when(bookings.adminSearch(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        service.searchBookings(id, null, null, BookingStatus.CONFIRMED, " tr_1 ", RefundStatus.NONE,
                PaymentCreationStatus.CREATED, PageRequest.of(0, 500));
        verify(bookings).adminSearch(eq(id), isNull(), isNull(), eq(BookingStatus.CONFIRMED), eq("tr_1"),
                eq(RefundStatus.NONE), eq(PaymentCreationStatus.CREATED), argThat(p -> p.getPageSize() == 100));
    }

    @Test
    void bookingDetailCombinesAuthoritativeRefundFeePayoutAndLiabilityState() {
        UUID id = UUID.randomUUID();
        Booking booking = Booking.builder().id(id).venueId(UUID.randomUUID()).scoutId(UUID.randomUUID())
                .status(BookingStatus.CONFIRMED).commissionPercentage(new BigDecimal("20.00"))
                .commissionEur(new BigDecimal("200.00")).mollieFeeEur(new BigDecimal("15.00"))
                .mollieFeeReconciled(true).refundStatus(RefundStatus.PROCESSING)
                .refundAmountEur(new BigDecimal("500.00")).build();
        OwnerPayout payout = OwnerPayout.builder().id(UUID.randomUUID()).bookingId(id).build();
        OwnerFinancialLiability liability = OwnerFinancialLiability.builder().id(UUID.randomUUID()).bookingId(id).build();
        when(bookings.findById(id)).thenReturn(Optional.of(booking));
        when(payouts.findByBookingId(id)).thenReturn(Optional.of(payout));
        when(liabilities.findByBookingIdOrderByCreatedAtAsc(id)).thenReturn(List.of(liability));
        var payoutDto = new com.eema.backend.dto.response.OwnerPayoutDto(payout.getId(), id, null, null, "EUR",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        var liabilityDto = new com.eema.backend.dto.response.OwnerFinancialLiabilityDto(liability.getId(), null,
                id, null, null, null, null, null, null);
        when(payoutService.dto(payout)).thenReturn(payoutDto);
        when(liabilityService.dto(liability)).thenReturn(liabilityDto);
        var detail = service.bookingDetail(id);
        assertThat(detail.mollieFeeEur()).isEqualByComparingTo("15.00");
        assertThat(detail.refundStatus()).isEqualTo(RefundStatus.PROCESSING);
        assertThat(detail.refundAmountEur()).isEqualByComparingTo("500.00");
        assertThat(detail.payout()).isSameAs(payoutDto);
        assertThat(detail.liabilities()).containsExactly(liabilityDto);
    }
}
