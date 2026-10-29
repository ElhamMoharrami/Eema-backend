package com.eema.backend.repository;

import com.eema.backend.entity.OwnerPayoutDestination;
import com.eema.backend.entity.enums.PayoutDestinationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface OwnerPayoutDestinationRepository extends JpaRepository<OwnerPayoutDestination,UUID>, JpaSpecificationExecutor<OwnerPayoutDestination>{
    // Lock the owner even when there is no current destination, so inserts and review serialize.
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from User u where u.id=:ownerId")
    Optional<com.eema.backend.entity.User> lockOwnerForMutation(@Param("ownerId") UUID ownerId);
    @Query("select d.ownerId from OwnerPayoutDestination d where d.id=:id")
    Optional<UUID> findOwnerIdById(@Param("id") UUID id);
    List<OwnerPayoutDestination> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Optional<OwnerPayoutDestination> findByOwnerIdAndStatus(UUID ownerId,PayoutDestinationStatus status);
    boolean existsByIbanFingerprintAndOwnerId(String fingerprint,UUID ownerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from OwnerPayoutDestination d where d.id=:id")
    Optional<OwnerPayoutDestination> findByIdForUpdate(@Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from OwnerPayoutDestination d where d.ownerId=:ownerId and d.status=:status")
    List<OwnerPayoutDestination> findCurrentForUpdate(@Param("ownerId") UUID ownerId,@Param("status") PayoutDestinationStatus status);
}
