package com.eema.backend.event;
import com.eema.backend.entity.enums.TrustEventType;
import java.util.UUID;
public record TrustNotificationEvent(TrustEventType type, UUID subjectId, String ownerVisibleReason) {}
