package com.eema.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.payout-destination")
public record PayoutDestinationProperties(boolean enabled, String encryptionKey, String fingerprintKey) {}
