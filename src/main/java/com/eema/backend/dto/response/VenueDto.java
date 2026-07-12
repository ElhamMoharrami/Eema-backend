package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.VenueStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record VenueDto(
        UUID id,
        UserDto owner,
        String name,
        String neighbourhood,
        int sizeM2,
        BigDecimal dayRateEur,
        List<String> tags,
        List<String> photoPaths,
        String clipUrl,
        String description,
        VenueStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
