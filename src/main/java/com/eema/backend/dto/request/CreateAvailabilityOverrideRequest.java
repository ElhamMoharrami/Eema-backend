package com.eema.backend.dto.request;

import com.eema.backend.entity.enums.AvailabilityOverrideType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateAvailabilityOverrideRequest(@NotNull LocalDate date, LocalTime startTime, LocalTime endTime,
                                                @NotNull AvailabilityOverrideType type, boolean allDay) {}
