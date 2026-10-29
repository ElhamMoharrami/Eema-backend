package com.eema.backend.repository;

import com.eema.backend.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.*;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash = :hash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("hash") String hash);

    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    int invalidateOutstanding(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    @Query("select t from PasswordResetToken t where t.expiresAt < :now or t.usedAt is not null order by t.createdAt")
    List<PasswordResetToken> findCleanupCandidates(@Param("now") OffsetDateTime now, Pageable pageable);
}
