package com.eema.backend.dto.response;

import com.eema.backend.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingDto(
        UUID id,
        UUID venueId,
        UUID scoutId,
        String molliePaymentId,
        BookingStatus status,
        BigDecimal bookingAmountEur,
        BigDecimal serviceFeeEur,
        BigDecimal eeemaCommissionEur,
        BigDecimal ownerPayoutEur,
        LocalDate shootDate,
        String rawBrief,
        String scoutContact,
        String ownerContact,
        boolean calaVerified,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt
) {
}