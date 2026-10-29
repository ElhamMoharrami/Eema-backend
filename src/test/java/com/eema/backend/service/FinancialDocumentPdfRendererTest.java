package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialDocumentPdfRendererTest {
    private final FinancialDocumentPdfRenderer renderer = new FinancialDocumentPdfRenderer();

    @Test
    void receiptContainsPaymentSnapshotAndExcludesInternalEconomics() throws Exception {
        Booking b = booking(); Venue venue = venue();
        String text = text(renderer.bookingReceipt("RCT-2026-000001", b, venue));
        assertThat(text).contains("Booking receipt", "RCT-2026-000001", b.getId().toString(),
                "Studio North", "27 Aug 2026", "10:00 - 14:00", "EUR 1000.00", "Total paid");
        assertThat(text).doesNotContain("commission", "Owner payout", "Mollie fee", "provider cost");
    }

    @Test
    void payoutStatementUsesExactLedgerValuesAndTransferReference() throws Exception {
        Booking b = booking(); Venue venue = venue(); User owner = venue.getOwner();
        OwnerPayout p = OwnerPayout.builder().id(UUID.randomUUID()).bookingId(b.getId()).venueId(venue.getId())
                .ownerId(owner.getId()).status(OwnerPayoutStatus.PAID).paidAt(OffsetDateTime.parse("2026-08-28T12:00:00Z"))
                .externalTransferReference("bank-transfer-42").grossRetainedAmountEur(new BigDecimal("500.00"))
                .commissionEur(new BigDecimal("100.00")).providerCostEur(new BigDecimal("15.00"))
                .payoutAmountEur(new BigDecimal("385.00")).build();
        String text = text(renderer.payoutStatement("PST-2026-000001", p, b, venue, owner));
        assertThat(text).contains("Owner payout statement", "PST-2026-000001", "bank-transfer-42",
                "EUR 500.00", "EUR 100.00", "EUR 15.00", "EUR 385.00",
                "not presented as a tax invoice");
    }

    private String text(byte[] pdf) throws Exception {
        try (var document = Loader.loadPDF(pdf)) { return new PDFTextStripper().getText(document); }
    }
    private Booking booking() {
        return Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED)
                .shootDate(LocalDate.of(2026, 8, 27)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(14, 0))
                .paidAt(OffsetDateTime.parse("2026-08-20T10:00:00Z"))
                .scoutContact(BookingContactSnapshot.builder().name("Scout Name").email("scout@example.test").build())
                .hourlyRateEurSnapshot(new BigDecimal("250.00")).minimumBookingMinutesSnapshot(120)
                .durationMinutes(240).billableMinutes(240).fullDayRateEurSnapshot(new BigDecimal("1000.00"))
                .venuePriceEur(new BigDecimal("1000.00")).totalPaidEur(new BigDecimal("1000.00")).build();
    }
    private Venue venue() {
        return Venue.builder().id(UUID.randomUUID()).name("Studio North").neighbourhood("Noord")
                .timezone("Europe/Amsterdam").owner(User.builder().id(UUID.randomUUID()).fullName("Owner Name")
                        .email("owner@example.test").role(UserRole.OWNER).build()).build();
    }
}
