package com.eema.backend.service;

import com.eema.backend.config.*;
import com.eema.backend.entity.User;
import com.eema.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AccountEmailNotificationService {
    private final UserRepository users;
    private final EmailService email;
    private final AppProperties app;
    private final AuthRecoveryProperties properties;

    public void sendVerification(java.util.UUID userId, String rawToken) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalStateException("Verification user no longer exists"));
        String link = link("/verify-email?token=", rawToken);
        String expiry = properties.emailVerificationTokenHours() + " hours";
        email.send(user.getEmail(), "Verify your Eema email",
                "Verify your Eema email within " + expiry + ": " + link,
                "<p>Verify your Eema email to use marketplace transactions.</p><p><a href=\"" + link + "\">Verify email</a></p><p>This link expires in " + expiry + ".</p>");
    }

    public void sendPasswordReset(java.util.UUID userId, String rawToken) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalStateException("Password-reset user no longer exists"));
        String link = link("/reset-password?token=", rawToken);
        String expiry = properties.passwordResetTokenMinutes() + " minutes";
        email.send(user.getEmail(), "Reset your Eema password",
                "Reset your Eema password within " + expiry + ": " + link,
                "<p>A password reset was requested for your Eema account.</p><p><a href=\"" + link + "\">Reset password</a></p><p>This link expires in " + expiry + ".</p>");
    }

    private String link(String path, String token) {
        String base = app.frontendBaseUrl().replaceAll("/+$", "");
        return base + path + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
