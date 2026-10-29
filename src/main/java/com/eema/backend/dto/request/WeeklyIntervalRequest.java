package com.eema.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record WeeklyIntervalRequest(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime,
                                    @NotNull LocalTime endTime) {}
