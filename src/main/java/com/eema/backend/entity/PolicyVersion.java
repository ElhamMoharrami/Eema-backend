package com.eema.backend.entity;

import com.eema.backend.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="policy_versions")
@Getter @Builder @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
public class PolicyVersion {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Enumerated(EnumType.STRING) @Column(name="policy_type",nullable=false,length=40) private PolicyType policyType;
    @Column(nullable=false,length=50) private String version;
    @Column(nullable=false,length=200) private String title;
    @Column(name="effective_at",nullable=false) private OffsetDateTime effectiveAt;
    @Column(name="published_at") private OffsetDateTime publishedAt;
    @Column(nullable=false,columnDefinition="text") private String content;
    @Column(name="content_hash",length=64) private String contentHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PolicyStatus status;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;

    public void publish(String hash, OffsetDateTime when) {
        if (status != PolicyStatus.DRAFT) throw new IllegalStateException("Only draft policies can be published");
        contentHash=hash; publishedAt=when; status=PolicyStatus.PUBLISHED;
    }
    public void retire() {
        if (status != PolicyStatus.PUBLISHED) throw new IllegalStateException("Only published policies can be retired");
        status=PolicyStatus.RETIRED;
    }
}
