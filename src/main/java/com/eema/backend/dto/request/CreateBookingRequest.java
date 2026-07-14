package com.eema.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a booking.
 */
public record CreateBookingRequest(

        @NotNull
        UUID venueId,

        @NotNull
        @Future
        LocalDate shootDate,

        @Size(max = 5000)
        String rawBrief,

        @NotNull
        @Valid
        ScoutContactDto scoutContact

) {
}