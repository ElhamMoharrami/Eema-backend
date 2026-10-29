package com.eema.backend.entity;

import com.eema.backend.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "owner_verifications", indexes = @Index(name="idx_owner_verification_status", columnList="status,submitted_at"))
@Getter @Setter @Builder @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
public class OwnerVerification {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="owner_id", nullable=false, unique=true, updatable=false) private User owner;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable=false, columnDefinition="owner_verification_status") @Builder.Default
    private OwnerVerificationStatus status = OwnerVerificationStatus.UNVERIFIED;
    @Column(name="owner_note", length=1000) private String ownerNote;
    @Column(name="review_note", length=1000) private String reviewNote;
    @Enumerated(EnumType.STRING) @Column(name="verification_method", length=40) private VerificationMethod verificationMethod;
    @Column(name="submitted_at") private OffsetDateTime submittedAt;
    @Column(name="reviewed_at") private OffsetDateTime reviewedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private User reviewedBy;
    @CreationTimestamp @Column(name="created_at", nullable=false, updatable=false) private OffsetDateTime createdAt;
    @UpdateTimestamp @Column(name="updated_at", nullable=false) private OffsetDateTime updatedAt;
    @Version private long version;
}
