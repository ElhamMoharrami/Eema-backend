package com.eema.backend.dto.response;

import java.time.LocalTime;

public record AvailabilityIntervalDto(LocalTime startTime, LocalTime endTime) {}
