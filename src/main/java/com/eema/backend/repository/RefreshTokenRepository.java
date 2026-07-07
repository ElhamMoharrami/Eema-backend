package com.eema.backend.repository;

import com.eema.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token hash.
     *
     * @param tokenHash the SHA-256 hash of the refresh token
     * @return Optional containing the refresh token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Delete all refresh tokens for a given user.
     *
     * @param userId the user ID
     */
    void deleteAllByUserId(UUID userId);

    /**
     * Delete all expired and non-revoked refresh tokens before a cutoff date.
     *
     * @param cutoff the cutoff date/time
     */
    void deleteAllByExpiresAtBeforeAndRevokedFalse(OffsetDateTime cutoff);
}