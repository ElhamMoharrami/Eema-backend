package com.eema.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateBookingRequest(

        @NotNull(message = "venueId is required")
        UUID venueId,

        @NotNull(message = "shootDate is required")
        LocalDate shootDate,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @NotNull(message = "endTime is required")
        LocalTime endTime,

        @NotBlank(message = "rawBrief cannot be blank")
        @Size(max = 1000)
        String rawBrief,

        @Valid
        @NotNull(message = "scoutContact is required")
        ScoutContactRequest scoutContact
) {
}
