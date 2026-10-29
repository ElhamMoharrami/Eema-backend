package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.OwnerPayoutHoldReason;
import com.eema.backend.entity.enums.OwnerPayoutStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OwnerPayoutDto(
        UUID id, UUID bookingId, UUID ownerId, UUID venueId, String currency,
        BigDecimal grossRetainedAmountEur, BigDecimal commissionEur,
        BigDecimal providerCostEur, BigDecimal payoutAmountEur,
        OwnerPayoutStatus status, OwnerPayoutHoldReason holdReason,
        OffsetDateTime eligibleAt, OffsetDateTime readyAt, OffsetDateTime paidAt,
        String externalTransferReference, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {}
