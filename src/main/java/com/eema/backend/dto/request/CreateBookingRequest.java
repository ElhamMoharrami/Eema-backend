package com.eema.backend.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID venueId,
        @NotNull @Future LocalDate shootDate,
        String rawBrief,
        @NotNull ScoutContactDto scoutContact
) {
}