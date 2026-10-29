package com.eema.backend.service;

import com.eema.backend.config.AuthRecoveryProperties;
import com.eema.backend.dto.response.MessageResponse;
import com.eema.backend.entity.*;
import com.eema.backend.event.*;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {
    public static final String RESEND_MESSAGE = "If an unverified account exists for that email, a verification message has been sent.";
    public static final String FORGOT_MESSAGE = "If an account exists for that email, password-reset instructions have been sent.";
    public static final String VERIFIED_MESSAGE = "Email verified successfully.";
    public static final String RESET_MESSAGE = "Password reset successfully.";

    private final UserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeTokenCodec tokens;
    private final AuthRecoveryProperties properties;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meters;

    @Transactional
    public void issueVerification(User user) {
        OffsetDateTime now = now();
        verificationTokens.invalidateOutstanding(user.getId(), now);
        String raw = tokens.generate();
        verificationTokens.save(EmailVerificationToken.builder().user(user).tokenHash(tokens.hash(raw))
                .expiresAt(now.plusHours(properties.emailVerificationTokenHours())).build());
        events.publishEvent(new EmailVerificationRequestedEvent(user.getId(), raw));
    }

    @Transactional
    public MessageResponse verifyEmail(String rawToken) {
        EmailVerificationToken token = verificationTokens.findByTokenHashForUpdate(safeHash(rawToken))
                .orElseThrow(this::invalidVerification);
        User user = token.getUser();
        if (!user.isActive()) throw invalidVerification();
        if (token.getUsedAt() != null) {
            if (user.isEmailVerified()) return new MessageResponse(VERIFIED_MESSAGE);
            throw invalidVerification();
        }
        OffsetDateTime now = now();
        if (!token.getExpiresAt().isAfter(now)) throw invalidVerification();
        token.setUsedAt(now);
        user.setEmailVerified(true);
        verificationTokens.invalidateOutstanding(user.getId(), now);
        meters.counter("eema.auth.email.verification.completed").increment();
        return new MessageResponse(VERIFIED_MESSAGE);
    }

    @Transactional
    public MessageResponse resendVerification(String email) {
        users.findByEmailIgnoreCase(normalize(email))
                .filter(User::isActive).filter(user -> !user.isEmailVerified())
                .ifPresent(this::issueVerification);
        return new MessageResponse(RESEND_MESSAGE);
    }

    @Transactional
    public MessageResponse forgotPassword(String email) {
        users.findByEmailIgnoreCase(normalize(email)).filter(User::isActive).ifPresent(user -> {
            OffsetDateTime now = now();
            resetTokens.invalidateOutstanding(user.getId(), now);
            String raw = tokens.generate();
            resetTokens.save(PasswordResetToken.builder().user(user).tokenHash(tokens.hash(raw))
                    .expiresAt(now.plusMinutes(properties.passwordResetTokenMinutes())).build());
            events.publishEvent(new PasswordResetRequestedEvent(user.getId(), raw));
        });
        return new MessageResponse(FORGOT_MESSAGE);
    }

    @Transactional
    public MessageResponse resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokens.findByTokenHashForUpdate(safeHash(rawToken))
                .orElseThrow(this::invalidReset);
        User user = token.getUser();
        OffsetDateTime now = now();
        if (!user.isActive() || token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)) throw invalidReset();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(now);
        resetTokens.invalidateOutstanding(user.getId(), now);
        refreshTokens.deleteAllByUserId(user.getId());
        meters.counter("eema.auth.password.reset.completed").increment();
        return new MessageResponse(RESET_MESSAGE);
    }

    private String safeHash(String raw) {
        try { return tokens.hash(raw); }
        catch (IllegalArgumentException exception) { throw new BadRequestException("Invalid or expired token"); }
    }
    private BadRequestException invalidVerification() { return new BadRequestException("Invalid or expired verification token"); }
    private BadRequestException invalidReset() { return new BadRequestException("Invalid or expired password-reset token"); }
    private OffsetDateTime now() { return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
    private String normalize(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
}
