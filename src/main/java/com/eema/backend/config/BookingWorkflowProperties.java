package com.eema.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.booking-workflow")
public record BookingWorkflowProperties(@Positive long paymentWindowHours,
                                        @Positive long expirationScanDelayMs) {
}
