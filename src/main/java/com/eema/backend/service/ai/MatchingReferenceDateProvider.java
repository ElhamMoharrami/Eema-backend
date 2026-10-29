package com.eema.backend.service.ai;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class MatchingReferenceDateProvider {

    public static final ZoneId MARKETPLACE_ZONE = ZoneId.of("Europe/Amsterdam");

    private final Clock clock;

    public MatchingReferenceDateProvider() {
        this(Clock.system(MARKETPLACE_ZONE));
    }

    MatchingReferenceDateProvider(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(MARKETPLACE_ZONE));
    }
}
