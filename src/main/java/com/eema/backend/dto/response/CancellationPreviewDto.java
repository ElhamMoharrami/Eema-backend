package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.CancellationPolicyTier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CancellationPreviewDto(
        UUID bookingId,
        CancellationPolicyTier policyTier,
        BigDecimal refundPercentage,
        BigDecimal refundAmountEur,
        BigDecimal retainedAmountEur,
        OffsetDateTime cancellationDeadline,
        boolean canCancel
) {
}
