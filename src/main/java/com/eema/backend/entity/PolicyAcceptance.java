package com.eema.backend.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="policy_acceptances")
@Getter @Builder @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
public class PolicyAcceptance {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(name="policy_version_id",nullable=false) private UUID policyVersionId;
    @Column(name="accepted_at",nullable=false,updatable=false) private OffsetDateTime acceptedAt;
    @Column(name="request_id",length=100,updatable=false) private String requestId;
    @Column(name="user_agent_summary",length=200,updatable=false) private String userAgentSummary;
}
