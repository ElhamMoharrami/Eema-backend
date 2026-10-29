package com.eema.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateVenueAvailabilitySettingsRequest(@NotBlank String timezone) {}
