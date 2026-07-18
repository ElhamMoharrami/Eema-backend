package com.eema.backend.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record CheckoutRequest(
    @NotNull(message = "venueId is required")
    UUID venueId,
    
    @NotNull(message = "shootDate is required")
    @FutureOrPresent(message = "shootDate must be today or later")
    LocalDate shootDate,
    
    @NotBlank(message = "brief cannot be blank")
    @Size(max = 1000, message = "brief must be 1000 characters or less")
    String rawBrief,
    
    @NotNull(message = "scoutContact is required")
    ScoutContactRequest scoutContact
) {}
