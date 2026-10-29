package com.eema.backend.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DailyAvailabilityDto(LocalDate date, List<AvailabilityIntervalDto> intervals) {}
