package com.eema.backend.entity;

import com.eema.backend.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_documents")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FinancialDocument {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, updatable = false, length = 40)
    private FinancialDocumentType documentType;
    @Column(name = "booking_id", nullable = false, updatable = false) private UUID bookingId;
    @Column(name = "owner_payout_id", updatable = false) private UUID ownerPayoutId;
    @Column(name = "recipient_user_id", nullable = false, updatable = false) private UUID recipientUserId;
    @Column(name = "document_number", nullable = false, unique = true, updatable = false, length = 40)
    private String documentNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 20)
    private FinancialDocumentStatus status;
    @Column(name = "template_version", nullable = false, updatable = false) private int templateVersion;
    @Column(name = "storage_reference", nullable = false, unique = true, updatable = false, length = 300)
    private String storageReference;
    @Column(name = "issued_at", nullable = false, updatable = false) private OffsetDateTime issuedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
}
