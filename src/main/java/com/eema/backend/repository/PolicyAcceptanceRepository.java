package com.eema.backend.repository;
import com.eema.backend.entity.PolicyAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface PolicyAcceptanceRepository extends JpaRepository<PolicyAcceptance,UUID>{
    @Query("select a.policyVersionId from PolicyAcceptance a where a.userId = :userId")
    Set<UUID> findPolicyVersionIdByUserId(@Param("userId") UUID userId);
    boolean existsByUserIdAndPolicyVersionId(UUID userId,UUID versionId);

    /** A concurrent acknowledgement must not abort the surrounding PostgreSQL transaction. */
    @Modifying
    @Query(value="""
        INSERT INTO policy_acceptances(id,user_id,policy_version_id,accepted_at,request_id,user_agent_summary)
        VALUES (:id,:userId,:versionId,:acceptedAt,:requestId,:agent)
        ON CONFLICT (user_id,policy_version_id) DO NOTHING
        """, nativeQuery=true)
    int insertIfAbsent(@Param("id") UUID id,@Param("userId") UUID userId,
            @Param("versionId") UUID versionId,@Param("acceptedAt") java.time.OffsetDateTime acceptedAt,
            @Param("requestId") String requestId,@Param("agent") String agent);
}
