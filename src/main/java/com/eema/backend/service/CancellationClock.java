package com.eema.backend.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class CancellationClock {
    private final Clock clock;

    public CancellationClock(Clock clock) {
        this.clock = clock;
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
