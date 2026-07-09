package com.eema.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a venue.
 */
public record CreateVenueRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank String neighbourhood,
        @NotNull @Min(1) @Max(9999) Integer sizeM2,
        @NotNull @DecimalMin("0.01") BigDecimal dayRateEur,
        @NotNull List<String> tags,
        String description,
        String clipUrl
) {}