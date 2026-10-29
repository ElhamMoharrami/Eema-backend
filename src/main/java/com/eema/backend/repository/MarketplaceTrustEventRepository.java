package com.eema.backend.repository;
import com.eema.backend.entity.MarketplaceTrustEvent;
import com.eema.backend.entity.enums.TrustSubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MarketplaceTrustEventRepository extends JpaRepository<MarketplaceTrustEvent, UUID> {
    List<MarketplaceTrustEvent> findBySubjectTypeAndSubjectIdOrderByCreatedAtAsc(TrustSubjectType type, UUID id);
}
