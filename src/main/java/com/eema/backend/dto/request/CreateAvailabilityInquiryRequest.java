package com.eema.backend.dto.request;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;

public record CreateAvailabilityInquiryRequest(
        @NotNull UUID venueId,
        @NotNull LocalDate requestedDate,
        @NotNull LocalTime requestedStartTime,
        @NotNull LocalTime requestedEndTime,
        @NotBlank @Size(max = 1000) String message) {}
