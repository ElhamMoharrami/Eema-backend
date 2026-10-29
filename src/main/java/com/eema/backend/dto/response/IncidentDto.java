package com.eema.backend.dto.response;
import com.eema.backend.entity.enums.*;
import java.time.OffsetDateTime;
import java.util.UUID;
public record IncidentDto(UUID id,UUID bookingId,UUID reportedByUserId,IncidentCategory category,String description,
 IncidentStatus status,OffsetDateTime createdAt,OffsetDateTime updatedAt,OffsetDateTime resolvedAt,String resolutionNote){}
