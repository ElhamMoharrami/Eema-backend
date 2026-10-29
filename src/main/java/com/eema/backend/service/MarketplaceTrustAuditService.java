package com.eema.backend.service;
import com.eema.backend.entity.MarketplaceTrustEvent;
import com.eema.backend.entity.enums.*;
import com.eema.backend.repository.MarketplaceTrustEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class MarketplaceTrustAuditService {
    private final MarketplaceTrustEventRepository events;
    public void append(TrustSubjectType subjectType, UUID subjectId, TrustEventType eventType,
                       UUID actorId, ActorType actorType, String note) {
        events.save(MarketplaceTrustEvent.builder().subjectType(subjectType).subjectId(subjectId)
                .eventType(eventType).actorId(actorId).actorType(actorType).note(note).build());
    }
}
