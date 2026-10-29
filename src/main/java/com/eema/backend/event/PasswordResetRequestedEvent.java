package com.eema.backend.event;
import java.util.UUID;
public record PasswordResetRequestedEvent(UUID userId, String rawToken) {}
