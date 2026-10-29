package com.eema.backend.repository;
import com.eema.backend.entity.PolicyAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface PolicyAcceptanceRepository extends JpaRepository<PolicyAcceptance,UUID>{
    @Query("select a.policyVersionId from PolicyAcceptance a where a.userId = :userId")
    Set<UUID> findPolicyVersionIdByUserId(@Param("userId") UUID userId);
    boolean existsByUserIdAndPolicyVersionId(UUID userId,UUID versionId);
}
