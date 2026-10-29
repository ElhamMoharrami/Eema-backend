package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.config.AuthRecoveryProperties;
import com.eema.backend.entity.User;
import com.eema.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class AccountEmailNotificationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final EmailService email = mock(EmailService.class);
    private final AppProperties app = new AppProperties(null, null, null, null, null, null, null,
            "https://app.eema.example/", null);
    private final AccountEmailNotificationService service = new AccountEmailNotificationService(
            users, email, app, new AuthRecoveryProperties(24, 60, 3_600_000));

    @Test
    void verificationUsesAuthoritativeEmailAndFrontendVerificationRoute() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(User.builder().id(id).email("current@example.com").build()));

        service.sendVerification(id, "raw+/token");

        verify(email).send(eq("current@example.com"), eq("Verify your Eema email"),
                contains("https://app.eema.example/verify-email?token=raw%2B%2Ftoken"),
                contains("/verify-email?token=raw%2B%2Ftoken"));
    }

    @Test
    void resetUsesAuthoritativeEmailAndFrontendResetRoute() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.of(User.builder().id(id).email("current@example.com").build()));

        service.sendPasswordReset(id, "raw-token");

        verify(email).send(eq("current@example.com"), eq("Reset your Eema password"),
                contains("https://app.eema.example/reset-password?token=raw-token"),
                contains("/reset-password?token=raw-token"));
    }
}
