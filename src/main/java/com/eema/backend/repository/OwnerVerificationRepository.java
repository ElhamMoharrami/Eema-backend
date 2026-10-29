package com.eema.backend.repository;
import com.eema.backend.entity.OwnerVerification;
import com.eema.backend.entity.enums.OwnerVerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface OwnerVerificationRepository extends JpaRepository<OwnerVerification, UUID>, JpaSpecificationExecutor<OwnerVerification> {
    Optional<OwnerVerification> findByOwnerId(UUID ownerId);
    // The owner exists even before their first verification; locking an absent verification cannot serialize inserts.
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from User u where u.id=:ownerId")
    Optional<com.eema.backend.entity.User> lockOwnerForSubmission(@Param("ownerId") UUID ownerId);
    // Do not join-lock the owner here: submission locks owner then verification.
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select v from OwnerVerification v where v.id=:id")
    Optional<OwnerVerification> findByIdForUpdate(@Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select v from OwnerVerification v where v.owner.id=:ownerId")
    Optional<OwnerVerification> findByOwnerIdForUpdate(@Param("ownerId") UUID ownerId);
    Page<OwnerVerification> findByStatus(OwnerVerificationStatus status, Pageable pageable);
}
