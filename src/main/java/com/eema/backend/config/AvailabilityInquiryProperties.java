package com.eema.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.availability-inquiry")
public record AvailabilityInquiryProperties(@Positive long offerValidHours,
                                            @Positive long expirationScanDelayMs) {}
