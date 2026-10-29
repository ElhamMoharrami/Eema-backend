package com.eema.backend.repository;
import com.eema.backend.entity.PolicyAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PolicyAcceptanceRepository extends JpaRepository<PolicyAcceptance,UUID>{
    Set<UUID> findPolicyVersionIdByUserId(UUID userId);
    boolean existsByUserIdAndPolicyVersionId(UUID userId,UUID versionId);
}
