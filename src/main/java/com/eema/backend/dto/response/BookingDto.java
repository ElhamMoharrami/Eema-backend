package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.BookingStatus;

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

        BigDecimal commissionPercentage,

        BigDecimal commissionEur,

        BigDecimal mollieFeeEur,

        BigDecimal ownerPayoutEur,

        BigDecimal totalPaidEur,

        String molliePaymentId,

        String scoutContact,

        String ownerContact,

        OffsetDateTime createdAt,

        OffsetDateTime paidAt,

        OffsetDateTime confirmedAt,

        OffsetDateTime cancelledAt

) {}