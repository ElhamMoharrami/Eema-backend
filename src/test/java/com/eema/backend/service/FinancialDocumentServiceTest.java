package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
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

class FinancialDocumentServiceTest {
    private final FinancialDocumentRepository documents = mock(FinancialDocumentRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final OwnerPayoutRepository payouts = mock(OwnerPayoutRepository.class);
    private final VenueRepository venues = mock(VenueRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final BookingEventRepository events = mock(BookingEventRepository.class);
    private final FinancialDocumentPdfRenderer renderer = mock(FinancialDocumentPdfRenderer.class);
    private final PrivateDocumentStorage storage = mock(PrivateDocumentStorage.class);
    private final FinancialDocumentService service = new FinancialDocumentService(documents, bookings, payouts,
            venues, users, events, renderer, storage);
    private final AtomicReference<FinancialDocument> receipt = new AtomicReference<>();
    private final AtomicReference<FinancialDocument> statement = new AtomicReference<>();
    private UUID bookingId; private UUID scoutId; private UUID ownerId; private UUID venueId;
    private Booking booking; private Venue venue; private OwnerPayout payout;

    @BeforeEach void setup() {
        bookingId = UUID.randomUUID(); scoutId = UUID.randomUUID(); ownerId = UUID.randomUUID(); venueId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("owner@example.test").fullName("Owner").role(UserRole.OWNER).build();
        venue = Venue.builder().id(venueId).owner(owner).name("Studio").neighbourhood("Noord").timezone("Europe/Amsterdam").build();
        booking = Booking.builder().id(bookingId).venueId(venueId).scoutId(scoutId).status(BookingStatus.CONFIRMED)
                .confirmedAt(OffsetDateTime.now()).paidAt(OffsetDateTime.now()).totalPaidEur(new BigDecimal("1000.00"))
                .build();
        payout = OwnerPayout.builder().id(UUID.randomUUID()).bookingId(bookingId).venueId(venueId).ownerId(ownerId)
                .status(OwnerPayoutStatus.PAID).paidAt(OffsetDateTime.now()).externalTransferReference("bank-42").build();
        when(bookings.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        when(venues.findById(venueId)).thenReturn(Optional.of(venue)); when(users.findById(ownerId)).thenReturn(Optional.of(owner));
        when(payouts.findByIdForUpdate(payout.getId())).thenReturn(Optional.of(payout));
        when(documents.findByDocumentTypeAndBookingId(FinancialDocumentType.BOOKING_RECEIPT, bookingId))
                .thenAnswer(i -> Optional.ofNullable(receipt.get()));
        when(documents.findByDocumentTypeAndOwnerPayoutId(FinancialDocumentType.OWNER_PAYOUT_STATEMENT, payout.getId()))
                .thenAnswer(i -> Optional.ofNullable(statement.get()));
        when(documents.nextReceiptSequence()).thenReturn(1L, 2L); when(documents.nextPayoutStatementSequence()).thenReturn(1L, 2L);
        when(renderer.bookingReceipt(any(), any(), any())).thenReturn("receipt-pdf".getBytes());
        when(renderer.payoutStatement(any(), any(), any(), any(), any())).thenReturn("statement-pdf".getBytes());
        when(documents.saveAndFlush(any())).thenAnswer(i -> { FinancialDocument d = i.getArgument(0);
            if (d.getDocumentType() == FinancialDocumentType.BOOKING_RECEIPT) receipt.set(d); else statement.set(d); return d; });
        when(storage.load(any())).thenAnswer(i -> i.getArgument(0).toString().startsWith("booking")
                ? "receipt-pdf".getBytes() : "statement-pdf".getBytes());
    }

    @Test void confirmedScoutReceiptIssuesOnceAndAdminCanReuseIt() {
        User scout = User.builder().id(scoutId).role(UserRole.SCOUT).build();
        var first = service.bookingReceipt(bookingId, scout);
        var second = service.bookingReceipt(bookingId, User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).build());
        assertThat(first.documentNumber()).matches("RCT-\\d{4}-000001");
        assertThat(second.documentNumber()).isEqualTo(first.documentNumber());
        verify(renderer, times(1)).bookingReceipt(any(), same(booking), same(venue));
        verify(events, times(1)).save(argThat(e -> e.getEventType() == BookingEventType.BOOKING_RECEIPT_ISSUED));
    }

    @Test void documentNumbersAreUniqueAndHistoricalReceiptIsNotRewritten() {
        User scout = User.builder().id(scoutId).role(UserRole.SCOUT).build();
        var first = service.bookingReceipt(bookingId, scout);
        booking.setStatus(BookingStatus.CANCELLED);
        var historical = service.bookingReceipt(bookingId, scout);
        assertThat(historical.documentNumber()).isEqualTo(first.documentNumber());

        UUID secondId = UUID.randomUUID();
        Booking second = Booking.builder().id(secondId).venueId(venueId).scoutId(scoutId)
                .status(BookingStatus.CONFIRMED).confirmedAt(OffsetDateTime.now()).paidAt(OffsetDateTime.now())
                .totalPaidEur(BigDecimal.ONE).build();
        when(bookings.findByIdForUpdate(secondId)).thenReturn(Optional.of(second));
        when(documents.findByDocumentTypeAndBookingId(FinancialDocumentType.BOOKING_RECEIPT, secondId))
                .thenReturn(Optional.empty());
        receipt.set(null);
        var next = service.bookingReceipt(secondId, scout);
        assertThat(next.documentNumber()).matches("RCT-\\d{4}-000002").isNotEqualTo(first.documentNumber());
        verify(renderer, times(2)).bookingReceipt(any(), any(), any());
    }

    @Test void unpaidStatesAndUnauthorizedPeopleCannotObtainReceipt() {
        for (BookingStatus status : List.of(BookingStatus.REQUESTED, BookingStatus.AWAITING_PAYMENT)) {
            booking.setStatus(status);
            assertThatThrownBy(() -> service.bookingReceipt(bookingId,
                    User.builder().id(scoutId).role(UserRole.SCOUT).build())).isInstanceOf(ConflictException.class);
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        assertThatThrownBy(() -> service.bookingReceipt(bookingId,
                User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.bookingReceipt(bookingId,
                User.builder().id(ownerId).role(UserRole.OWNER).build())).isInstanceOf(ForbiddenException.class);
    }

    @Test void confirmedPaymentRemainsReceiptEligibleAfterCancellationBeforeFirstDownload() {
        booking.setStatus(BookingStatus.CANCELLED);
        var result = service.bookingReceipt(bookingId, User.builder().id(scoutId).role(UserRole.SCOUT).build());
        assertThat(result.documentNumber()).startsWith("RCT-");
        verify(renderer).bookingReceipt(any(), same(booking), same(venue));
    }

    @Test void onlyPaidPayoutUsesExactLedgerAndIssuesOneStatement() {
        User owner = venue.getOwner();
        var result = service.payoutStatement(payout.getId(), owner);
        assertThat(result.documentNumber()).matches("PST-\\d{4}-000001");
        verify(renderer).payoutStatement(any(), same(payout), same(booking), same(venue), same(owner));
        service.payoutStatement(payout.getId(), User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).build());
        verify(renderer, times(1)).payoutStatement(any(), any(), any(), any(), any());
    }

    @Test void readyHeldScoutAndUnrelatedOwnerCannotObtainStatement() {
        for (OwnerPayoutStatus status : List.of(OwnerPayoutStatus.READY, OwnerPayoutStatus.HELD)) {
            payout.setStatus(status);
            assertThatThrownBy(() -> service.payoutStatement(payout.getId(), venue.getOwner())).isInstanceOf(ConflictException.class);
        }
        payout.setStatus(OwnerPayoutStatus.PAID);
        assertThatThrownBy(() -> service.payoutStatement(payout.getId(),
                User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).build())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.payoutStatement(payout.getId(),
                User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build())).isInstanceOf(ForbiddenException.class);
    }
}
