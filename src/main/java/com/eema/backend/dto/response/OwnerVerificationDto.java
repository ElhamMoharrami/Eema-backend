package com.eema.backend.dto.response;
import com.eema.backend.entity.enums.*;
import java.time.OffsetDateTime;
import java.util.UUID;
public record OwnerVerificationDto(UUID id, UUID ownerId, OwnerVerificationStatus status,
                                   OffsetDateTime submittedAt, OffsetDateTime reviewedAt,
                                   String reviewNote, VerificationMethod verificationMethod) {}
