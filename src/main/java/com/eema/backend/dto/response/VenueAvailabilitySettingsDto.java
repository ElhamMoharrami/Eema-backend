package com.eema.backend.dto.response;

import java.util.List;
import java.util.UUID;

public record VenueAvailabilitySettingsDto(UUID venueId, String timezone, List<WeeklyIntervalDto> weekly,
                                           List<AvailabilityOverrideDto> overrides) {}
