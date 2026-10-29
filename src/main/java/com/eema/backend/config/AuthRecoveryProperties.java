package com.eema.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth-recovery")
public record AuthRecoveryProperties(@Positive int emailVerificationTokenHours,
                                     @Positive int passwordResetTokenMinutes,
                                     @Positive int cleanupDelayMs) {}
