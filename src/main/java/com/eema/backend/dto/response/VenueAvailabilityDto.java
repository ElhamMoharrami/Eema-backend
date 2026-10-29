package com.eema.backend.dto.response;

import java.util.List;
import java.util.UUID;

public record VenueAvailabilityDto(UUID venueId, String timezone, List<DailyAvailabilityDto> dates) {}
