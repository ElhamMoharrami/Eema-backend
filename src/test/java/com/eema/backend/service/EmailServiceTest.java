package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailServiceTest {
    @Test
    void disabledEmailNeedsNoCredentials() {
        assertDoesNotThrow(() -> new EmailService(properties(false, null, null)));
    }

    @Test
    void enabledEmailRequiresCredentials() {
        assertThrows(IllegalStateException.class, () -> new EmailService(properties(true, "", "")));
    }

    private AppProperties properties(boolean enabled, String key, String sender) {
        return new AppProperties(null, null, null, null,
                new AppProperties.ResendProperties(enabled, key, sender), null, null,
                "http://localhost:3000", null);
    }
}
