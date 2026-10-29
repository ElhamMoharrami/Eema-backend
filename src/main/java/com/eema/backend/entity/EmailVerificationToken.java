package com.eema.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_email_verification_user", columnList = "user_id"),
        @Index(name = "idx_email_verification_expiry", columnList = "expires_at")})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class EmailVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false, updatable = false) private User user;
    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false, updatable = false) private OffsetDateTime expiresAt;
    @Column(name = "used_at") private OffsetDateTime usedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
}
