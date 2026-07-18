package com.eema.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request for venue matching.
 */
public record MatchRequest(
    @NotBlank(message = "Brief cannot be blank")
    @Size(min = 10, max = 1000, message = "Brief must be 10-1000 characters")
    String brief,

    UUID venueId  // optional: if provided, return a direct quote for this venue only
) {}