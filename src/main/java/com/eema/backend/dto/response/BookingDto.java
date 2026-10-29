package com.eema.backend.dto.response;

import com.eema.backend.entity.BookingContactSnapshot;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.entity.enums.PaymentCreationStatus;
import com.eema.backend.entity.enums.RefundStatus;
import com.eema.backend.entity.enums.OwnerPayoutStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingDto(
        UUID id,
        UUID venueId,
        UUID scoutId,
        BookingStatus status,

        LocalDate shootDate,
        LocalTime startTime,
        LocalTime endTime,

        String rawBrief,

        BigDecimal venuePriceEur,
        BigDecimal hourlyRateEurSnapshot,
        Integer minimumBookingMinutesSnapshot,
        BigDecimal fullDayRateEurSnapshot,
        Integer durationMinutes,
        Integer billableMinutes,
        BigDecimal commissionPercentage,
        BigDecimal commissionEur,
        BigDecimal mollieFeeEur,
        boolean mollieFeeReconciled,
        OffsetDateTime mollieFeeReconciledAt,
        BigDecimal ownerPayoutEur,
        OwnerPayoutStatus ownerPayoutStatus,
        BigDecimal totalPaidEur,

        String molliePaymentId,
        PaymentCreationStatus paymentCreationStatus,
        RefundStatus refundStatus,
        String mollieRefundId,
        BigDecimal refundAmountEur,
        OffsetDateTime refundRequestedAt,
        OffsetDateTime refundedAt,

        BookingContactSnapshot scoutContact,
        BookingContactSnapshot ownerContact,

        OffsetDateTime createdAt,
        OffsetDateTime paidAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime acceptedAt,
        OffsetDateTime ownerResponseExpiresAt,
        OffsetDateTime paymentExpiresAt,
        OffsetDateTime declinedAt,
        OffsetDateTime expiredAt
) {
}
