package com.eema.backend.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateVenueRequest {
    @NotBlank @Size(min = 2, max = 120)
    private String name;

    @NotBlank
    private String neighbourhood;

    @NotNull @Min(1) @Max(9999)
    private Integer sizeM2;

    @NotNull @DecimalMin("0.01")
    private java.math.BigDecimal dayRateEur;

    @NotNull
    private List<String> tags;

    private String description;

    private String clipUrl;
}