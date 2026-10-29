package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.ActorType;
import com.eema.backend.entity.enums.BookingEventType;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AdminBookingEventDto(UUID id, BookingEventType eventType, ActorType actorType,
        String actorName, OffsetDateTime createdAt, Map<String, Object> payload) {}
