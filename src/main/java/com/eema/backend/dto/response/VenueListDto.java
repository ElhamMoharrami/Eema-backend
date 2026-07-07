package com.eema.backend.dto.response;

import com.eema.backend.entity.VenueStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VenueListDto(
        UUID id,
        String name,
        String neighbourhood,
        int sizeM2,
        BigDecimal dayRateEur,
        List<String> tags,
        String firstPhotoUrl,
        VenueStatus status
) {
}