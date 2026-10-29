package com.eema.backend.support;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.ProductionActivity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** One baseline for READY validator/service cases. No storage lookup is part of readiness. */
public final class ReadyVenueFixture {
    private ReadyVenueFixture() {}
    public static final List<String> KEYS = List.of("maxCrewSize", "permittedActivities", "productionRules",
            "naturalLight", "loadingAccess", "bathroomsAvailable", "soundRecordingSuitable", "hourlyRateEur",
            "minimumBookingMinutes", "minimumLeadTimeMinutes", "timezone", "photoPaths");
    public static Venue complete() {
        return Venue.builder().id(UUID.randomUUID()).name("QA Ready Studio").neighbourhood("QA West").sizeM2(100)
                .maxCrewSize(20).permittedActivities(List.of(ProductionActivity.PHOTO))
                .productionRules("QA fictional venue rules").naturalLight(false).loadingAccess(false)
                .bathroomsAvailable(false).soundRecordingSuitable(false).hourlyRateEur(new BigDecimal("100.00"))
                .minimumBookingMinutes(60).minimumLeadTimeMinutes(60).timezone("Europe/Amsterdam")
                .photoPaths(List.of("/api/upload/files/11111111-1111-4111-8111-111111111111.png")).build();
    }
}
