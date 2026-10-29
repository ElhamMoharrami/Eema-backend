package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OwnerPayoutSummaryDto(UUID id, UUID bookingId, UUID venueId, String currency,
        BigDecimal grossRetainedAmountEur, BigDecimal commissionEur, BigDecimal providerCostEur,
        BigDecimal payoutAmountEur, OwnerPayoutStatus status, OwnerPayoutHoldReason holdReason,
        OffsetDateTime eligibleAt, OffsetDateTime readyAt, OffsetDateTime paidAt,
        boolean statementAvailable) {}
