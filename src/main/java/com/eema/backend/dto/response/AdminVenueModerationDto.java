package com.eema.backend.dto.response;
import com.eema.backend.entity.enums.VenueStatus;
import java.time.OffsetDateTime;
import java.util.*;
public record AdminVenueModerationDto(UUID id, UUID ownerId, String name, String neighbourhood,
                                      VenueStatus status, OffsetDateTime submittedForReviewAt,
                                      OffsetDateTime reviewedAt, UUID reviewedBy, String moderationReason,
                                      List<String> readinessIssues, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
