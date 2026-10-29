package com.eema.backend.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.notification-outbox")
public record NotificationOutboxProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("20") @Min(1) @Max(100) int batchSize,
        @DefaultValue("8") @Min(1) @Max(20) int maxAttempts,
        @DefaultValue("300") @Min(60) @Max(3600) int leaseSeconds,
        @DefaultValue("30") @Min(5) @Max(3600) int initialBackoffSeconds,
        @DefaultValue("3600") @Min(5) @Max(86400) int maxBackoffSeconds) {
    public long backoffSeconds(int attempt) {
        return Math.min(maxBackoffSeconds, initialBackoffSeconds * (1L << Math.min(20, Math.max(0, attempt - 1))));
    }
}
