package com.eema.backend.entity;

import com.eema.backend.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="owner_payout_destinations")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OwnerPayoutDestination {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="owner_id",nullable=false,updatable=false) private UUID ownerId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) @Builder.Default
    private PayoutDestinationType type=PayoutDestinationType.IBAN;
    @Column(nullable=false,length=3) @Builder.Default private String currency="EUR";
    @Column(name="account_holder_name",nullable=false,length=160) private String accountHolderName;
    @Column(name="iban_encrypted",nullable=false,length=512) private String ibanEncrypted;
    @Column(name="iban_fingerprint",nullable=false,length=64) private String ibanFingerprint;
    @Column(name="iban_last4",nullable=false,length=4) private String ibanLast4;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private PayoutDestinationStatus status;
    @Column(name="submitted_at",nullable=false) private OffsetDateTime submittedAt;
    @Column(name="verified_at") private OffsetDateTime verifiedAt;
    @Column(name="verified_by") private UUID verifiedBy;
    @Column(name="rejected_at") private OffsetDateTime rejectedAt;
    @Column(name="rejection_reason",length=1000) private String rejectionReason;
    @Column(name="replaced_at") private OffsetDateTime replacedAt;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    @UpdateTimestamp @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    @Version private long version;
}
