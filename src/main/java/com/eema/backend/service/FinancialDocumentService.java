package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.*;
import com.eema.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @lombok.extern.slf4j.Slf4j
public class FinancialDocumentService {
    private static final int TEMPLATE_VERSION = 3;
    private final FinancialDocumentRepository documents;
    private final BookingRepository bookings;
    private final OwnerPayoutRepository payouts;
    private final VenueRepository venues;
    private final UserRepository users;
    private final BookingEventRepository events;
    private final FinancialDocumentPdfRenderer renderer;
    private final PrivateDocumentStorage storage;

    @Transactional
    public DocumentDownload bookingReceipt(UUID bookingId, User actor) {
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        authorizeReceipt(booking, actor);
        Optional<FinancialDocument> existing = documents.findByDocumentTypeAndBookingId(
                FinancialDocumentType.BOOKING_RECEIPT, bookingId);
        if (existing.isPresent()) return download(existing.get(), "eema-booking-receipt-");
        boolean commerciallyConfirmed = booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REFUNDED;
        if (!commerciallyConfirmed || booking.getConfirmedAt() == null
                || booking.getPaidAt() == null || booking.getTotalPaidEur() == null
                || booking.getTotalPaidEur().signum() <= 0 || booking.getLatePaymentDetectedAt() != null) {
            throw new ConflictException("A receipt is available only for a successfully paid confirmed booking");
        }
        Venue venue = venue(booking.getVenueId());
        OffsetDateTime now = OffsetDateTime.now();
        String number = number("RCT", now.getYear(), documents.nextReceiptSequence());
        UUID id = UUID.randomUUID();
        byte[] pdf = renderer.bookingReceipt(number, booking, venue);
        String reference = "booking-receipts/" + id + ".pdf";
        return issue(FinancialDocument.builder().id(id).documentType(FinancialDocumentType.BOOKING_RECEIPT)
                .bookingId(bookingId).recipientUserId(booking.getScoutId()).documentNumber(number)
                .status(FinancialDocumentStatus.ISSUED).templateVersion(TEMPLATE_VERSION)
                .storageReference(reference).issuedAt(now).build(), pdf,
                BookingEventType.BOOKING_RECEIPT_ISSUED, "eema-booking-receipt-");
    }

    @Transactional
    public DocumentDownload payoutStatement(UUID payoutId, User actor) {
        OwnerPayout payout = payouts.findByIdForUpdate(payoutId)
                .orElseThrow(() -> new NotFoundException("Owner payout not found: " + payoutId));
        authorizeStatement(payout, actor);
        Optional<FinancialDocument> existing = documents.findByDocumentTypeAndOwnerPayoutId(
                FinancialDocumentType.OWNER_PAYOUT_STATEMENT, payoutId);
        if (existing.isPresent()) return download(existing.get(), "eema-owner-payout-statement-");
        if (payout.getStatus() != OwnerPayoutStatus.PAID || payout.getPaidAt() == null
                || payout.getExternalTransferReference() == null || payout.getExternalTransferReference().isBlank()) {
            throw new ConflictException("A payout statement is available only after the payout is recorded as paid");
        }
        Booking booking = bookings.findById(payout.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found: " + payout.getBookingId()));
        Venue venue = venue(payout.getVenueId());
        User owner = users.findById(payout.getOwnerId()).orElseThrow(() -> new NotFoundException("Owner not found"));
        OffsetDateTime now = OffsetDateTime.now();
        String number = number("PST", now.getYear(), documents.nextPayoutStatementSequence());
        UUID id = UUID.randomUUID();
        byte[] pdf = renderer.payoutStatement(number, payout, booking, venue, owner);
        String reference = "payout-statements/" + id + ".pdf";
        return issue(FinancialDocument.builder().id(id).documentType(FinancialDocumentType.OWNER_PAYOUT_STATEMENT)
                .bookingId(booking.getId()).ownerPayoutId(payoutId).recipientUserId(payout.getOwnerId())
                .documentNumber(number).status(FinancialDocumentStatus.ISSUED)
                .templateVersion(TEMPLATE_VERSION).storageReference(reference).issuedAt(now).build(), pdf,
                BookingEventType.OWNER_PAYOUT_STATEMENT_ISSUED, "eema-owner-payout-statement-");
    }

    private DocumentDownload issue(FinancialDocument document, byte[] pdf, BookingEventType eventType, String filenamePrefix) {
        storage.store(document.getStorageReference(), pdf);
        boolean transactionalCleanup = org.springframework.transaction.support.TransactionSynchronizationManager
                .isSynchronizationActive();
        if (transactionalCleanup) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                try { storage.delete(document.getStorageReference()); }
                                catch (RuntimeException cleanupFailure) {
                                    log.error("Private document rollback cleanup failed: documentId={}", document.getId());
                                }
                            }
                        }
                    });
        }
        try {
            documents.saveAndFlush(document);
            events.save(BookingEvent.builder().bookingId(document.getBookingId()).eventType(eventType)
                    .actorId(null).actorName("SYSTEM").actorType(ActorType.SYSTEM)
                    .payload(new BookingEventPayload(Map.of("documentId", document.getId(),
                            "documentNumber", document.getDocumentNumber(), "templateVersion", TEMPLATE_VERSION))).build());
            return new DocumentDownload(pdf, document.getDocumentNumber(), filenamePrefix + document.getDocumentNumber() + ".pdf");
        } catch (RuntimeException e) {
            if (!transactionalCleanup) {
                try { storage.delete(document.getStorageReference()); }
                catch (RuntimeException cleanupFailure) { e.addSuppressed(cleanupFailure); }
            }
            throw e;
        }
    }
    private DocumentDownload download(FinancialDocument document, String prefix) {
        return new DocumentDownload(storage.load(document.getStorageReference()), document.getDocumentNumber(),
                prefix + document.getDocumentNumber() + ".pdf");
    }
    private void authorizeReceipt(Booking booking, User actor) {
        if (actor == null || (actor.getRole() != UserRole.ADMIN && !booking.getScoutId().equals(actor.getId())))
            throw new ForbiddenException("Receipt access is limited to the booking scout and administrators");
    }
    private void authorizeStatement(OwnerPayout payout, User actor) {
        if (actor == null || (actor.getRole() != UserRole.ADMIN && !payout.getOwnerId().equals(actor.getId())))
            throw new ForbiddenException("Payout statement access is limited to the payout owner and administrators");
    }
    private Venue venue(UUID id) { return venues.findById(id).orElseThrow(() -> new NotFoundException("Venue not found: " + id)); }
    private String number(String prefix, int year, long sequence) { return "%s-%d-%06d".formatted(prefix, year, sequence); }
    public record DocumentDownload(byte[] content, String documentNumber, String filename) {}
}
