package com.eema.backend.dto.response;

import com.eema.backend.entity.enums.VenueStatus;
import com.eema.backend.entity.enums.ProductionActivity;
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
        BigDecimal hourlyRateEur,
        Integer minimumBookingMinutes,
        BigDecimal fullDayRateEur,
        Integer maxCrewSize,
        List<ProductionActivity> permittedActivities,
        List<ProductionActivity> prohibitedActivities,
        Boolean naturalLight,
        Boolean blackoutPossible,
        BigDecimal ceilingHeightM,
        Boolean loadingAccess,
        Boolean parkingAvailable,
        Integer parkingSpaces,
        Integer floorNumber,
        Boolean liftAvailable,
        Boolean wheelchairAccessible,
        Boolean bathroomsAvailable,
        Boolean soundRecordingSuitable,
        String productionRules,
        String powerDetails,
        List<String> tags,
        List<String> photoPaths,
        String clipUrl,
        String description,
        String timezone,
        VenueStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
