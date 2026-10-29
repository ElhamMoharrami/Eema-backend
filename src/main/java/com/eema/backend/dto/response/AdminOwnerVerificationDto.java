package com.eema.backend.dto.response;
import com.eema.backend.entity.enums.*;
import java.time.OffsetDateTime;
import java.util.UUID;
public record AdminOwnerVerificationDto(UUID id, UUID ownerId, OwnerVerificationStatus status,
                                        String ownerNote, String reviewNote, VerificationMethod verificationMethod,
                                        OffsetDateTime submittedAt, OffsetDateTime reviewedAt, UUID reviewedBy,
                                        OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
