package com.eema.backend.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record WeeklyIntervalDto(UUID id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {}
