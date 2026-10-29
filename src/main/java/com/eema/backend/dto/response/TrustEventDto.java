package com.eema.backend.dto.response;
import com.eema.backend.entity.enums.*;
import java.time.OffsetDateTime;
import java.util.UUID;
public record TrustEventDto(UUID id, TrustEventType eventType, UUID actorId, ActorType actorType,
                            String note, OffsetDateTime createdAt) {}
