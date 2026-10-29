package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminBookingDetailDto(
        AdminBookingSummaryDto booking,
        BigDecimal commissionPercentage, BigDecimal commissionEur,
        BigDecimal mollieFeeEur, boolean mollieFeeReconciled, OffsetDateTime mollieFeeReconciledAt,
        RefundStatus refundStatus, String mollieRefundId, BigDecimal refundAmountEur,
        OffsetDateTime refundRequestedAt, OffsetDateTime refundedAt,
        CancellationPolicyTier cancellationPolicyTier, ActorType cancelledByActorType,
        BigDecimal cancellationRefundPercentage, BigDecimal cancellationRefundAmountEur,
        BigDecimal cancellationRetainedAmountEur, ProviderCostResponsibility providerCostResponsibility,
        BigDecimal cancellationProviderCostEur, boolean cancellationProviderCostReconciled,
        OwnerPayoutDto payout, List<OwnerFinancialLiabilityDto> liabilities
) {}
