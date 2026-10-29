package com.eema.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_password_reset_user", columnList = "user_id"),
        @Index(name = "idx_password_reset_expiry", columnList = "expires_at")})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false, updatable = false) private User user;
    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false, updatable = false) private OffsetDateTime expiresAt;
    @Column(name = "used_at") private OffsetDateTime usedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
}
