package com.eema.backend.dto.response;
import com.eema.backend.entity.enums.*;
import java.time.OffsetDateTime;
import java.util.UUID;
public record PayoutDestinationDto(UUID id,UUID ownerId,PayoutDestinationType type,String currency,
 String accountHolderName,String maskedIban,PayoutDestinationStatus status,OffsetDateTime submittedAt,
 OffsetDateTime verifiedAt,OffsetDateTime rejectedAt,String rejectionReason,OffsetDateTime replacedAt,
 boolean anotherVerifiedDestinationExists){}
