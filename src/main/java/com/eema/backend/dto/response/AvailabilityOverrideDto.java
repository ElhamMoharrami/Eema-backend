package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.AvailabilityOverrideType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityOverrideDto(UUID id, LocalDate date, LocalTime startTime, LocalTime endTime,
                                      AvailabilityOverrideType type, boolean allDay) {}
