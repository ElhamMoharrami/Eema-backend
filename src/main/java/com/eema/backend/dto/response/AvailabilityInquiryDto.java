package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.AvailabilityInquiryStatus;
import java.time.*;
import java.util.UUID;

public record AvailabilityInquiryDto(
        UUID id, UUID venueId, String venueName, String timezone, UUID scoutId,
        LocalDate requestedDate, LocalTime requestedStartTime, LocalTime requestedEndTime,
        String message, AvailabilityInquiryStatus status,
        LocalDate offeredDate, LocalTime offeredStartTime, LocalTime offeredEndTime,
        OffsetDateTime offerExpiresAt, UUID acceptedBookingId,
        OffsetDateTime createdAt, OffsetDateTime declinedAt, OffsetDateTime offeredAt,
        OffsetDateTime acceptedAt, OffsetDateTime expiredAt) {}
