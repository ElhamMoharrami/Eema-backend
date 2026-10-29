package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.VenueStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VenueListDto(
        UUID id,
        String name,
        String neighbourhood,
        int sizeM2,
        BigDecimal hourlyRateEur,
        Integer minimumBookingMinutes,
        BigDecimal fullDayRateEur,
        Integer maxCrewSize,
        Boolean naturalLight,
        Boolean loadingAccess,
        Boolean parkingAvailable,
        Boolean soundRecordingSuitable,
        List<String> tags,
        String firstPhotoUrl,
        VenueStatus status
) {
}
