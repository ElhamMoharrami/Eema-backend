package com.eema.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.*;

public record OfferAvailabilityInquiryRequest(
        @NotNull LocalDate offeredDate,
        @NotNull LocalTime offeredStartTime,
        @NotNull LocalTime offeredEndTime) {}
