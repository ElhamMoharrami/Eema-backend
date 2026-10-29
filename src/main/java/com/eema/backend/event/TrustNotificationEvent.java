package com.eema.backend.event;
import com.eema.backend.entity.enums.TrustEventType;
import java.util.UUID;

/** Audit row identity distinguishes legitimate repeated moderation/review transitions. */
public record TrustNotificationEvent(TrustEventType type, UUID subjectId, String ownerVisibleReason, UUID eventId) {}
