package com.eema.backend.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record UpdateVenueRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Size(max = 100) String neighbourhood,
        @NotNull @Min(1) @Max(9999) Integer sizeM2,
        @NotNull @DecimalMin("0.01") @DecimalMax("99999999.99") BigDecimal hourlyRateEur,
        @NotNull @Min(1) @Max(10080) Integer minimumBookingMinutes,
        @DecimalMin("0.01") @DecimalMax("99999999.99") BigDecimal fullDayRateEur,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 50) String> tags,
        @Size(max = 5000) String description,
        @Size(max = 500) String clipUrl,
        @NotBlank @Size(max = 100) String timezone,
        @NotNull @Size(max = 12) List<@NotBlank @Size(max = 500) String> photoPaths
) {
}
