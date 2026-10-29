package com.eema.backend.repository;

import com.eema.backend.entity.FinancialDocument;
import com.eema.backend.entity.enums.FinancialDocumentType;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface FinancialDocumentRepository extends JpaRepository<FinancialDocument, UUID> {
    Optional<FinancialDocument> findByDocumentTypeAndBookingId(FinancialDocumentType type, UUID bookingId);
    Optional<FinancialDocument> findByDocumentTypeAndOwnerPayoutId(FinancialDocumentType type, UUID payoutId);
    @Query(value = "select nextval('booking_receipt_document_seq')", nativeQuery = true)
    long nextReceiptSequence();
    @Query(value = "select nextval('owner_payout_statement_document_seq')", nativeQuery = true)
    long nextPayoutStatementSequence();
}
