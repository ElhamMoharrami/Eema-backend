package com.eema.backend.event;
import java.util.UUID;
public record EmailVerificationRequestedEvent(UUID userId, String rawToken) {}
