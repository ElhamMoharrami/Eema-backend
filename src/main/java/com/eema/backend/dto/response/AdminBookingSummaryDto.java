package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record AdminBookingSummaryDto(UUID id, UUID venueId, UUID scoutId, BookingStatus status,
        LocalDate shootDate, LocalTime startTime, LocalTime endTime,
        BigDecimal venuePriceEur, BigDecimal totalPaidEur, BigDecimal ownerPayoutEur,
        String molliePaymentId, PaymentCreationStatus paymentCreationStatus,
        RefundStatus refundStatus, OffsetDateTime createdAt, OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt, OffsetDateTime latePaymentDetectedAt) {}
