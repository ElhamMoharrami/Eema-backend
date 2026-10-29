package com.eema.backend.repository;
import com.eema.backend.entity.PolicyVersion;
import com.eema.backend.entity.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface PolicyVersionRepository extends JpaRepository<PolicyVersion,UUID> {
    List<PolicyVersion> findByStatusOrderByPolicyTypeAsc(PolicyStatus status);
    List<PolicyVersion> findAllByOrderByPolicyTypeAscCreatedAtDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from PolicyVersion p where p.id=:id") Optional<PolicyVersion> findByIdForUpdate(@Param("id")UUID id);
    Optional<PolicyVersion> findByPolicyTypeAndStatus(PolicyType type,PolicyStatus status);
}
