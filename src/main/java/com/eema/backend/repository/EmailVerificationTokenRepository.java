package com.eema.backend.repository;

import com.eema.backend.entity.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.*;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from EmailVerificationToken t join fetch t.user where t.tokenHash = :hash")
    Optional<EmailVerificationToken> findByTokenHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("update EmailVerificationToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    int invalidateOutstanding(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    @Query("select t from EmailVerificationToken t where t.expiresAt < :now or t.usedAt is not null order by t.createdAt")
    List<EmailVerificationToken> findCleanupCandidates(@Param("now") OffsetDateTime now, Pageable pageable);
}
