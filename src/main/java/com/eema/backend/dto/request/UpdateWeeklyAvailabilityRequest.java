package com.eema.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateWeeklyAvailabilityRequest(@NotNull @Size(max = 50) List<@NotNull @Valid WeeklyIntervalRequest> intervals) {}
