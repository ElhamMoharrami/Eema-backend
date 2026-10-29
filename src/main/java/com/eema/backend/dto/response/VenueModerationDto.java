package com.eema.backend.dto.response;
import com.eema.backend.entity.enums.VenueStatus;
import java.time.OffsetDateTime;
import java.util.*;
public record VenueModerationDto(UUID venueId, VenueStatus status, OffsetDateTime submittedForReviewAt,
                                 OffsetDateTime reviewedAt, String moderationReason,
                                 List<String> readinessIssues) {}
