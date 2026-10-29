package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.OwnerFinancialLiabilityStatus;
import com.eema.backend.entity.enums.OwnerFinancialLiabilityType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OwnerFinancialLiabilityDto(UUID id, UUID ownerId, UUID bookingId,
        OwnerFinancialLiabilityType type, BigDecimal amountEur,
        OwnerFinancialLiabilityStatus status, String note,
        OffsetDateTime createdAt, OffsetDateTime settledAt) {}
