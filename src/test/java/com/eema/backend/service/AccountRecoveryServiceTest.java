package com.eema.backend.service;

import com.eema.backend.config.AuthRecoveryProperties;
import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.event.*;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.repository.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountRecoveryServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final EmailVerificationTokenRepository verification = mock(EmailVerificationTokenRepository.class);
    private final PasswordResetTokenRepository resets = mock(PasswordResetTokenRepository.class);
    private final RefreshTokenRepository refresh = mock(RefreshTokenRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final OneTimeTokenCodec codec = mock(OneTimeTokenCodec.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneOffset.UTC);
    private final AccountRecoveryService service = new AccountRecoveryService(users, verification, resets, refresh,
            passwords, codec, new AuthRecoveryProperties(24, 60, 3600000), clock, events, new SimpleMeterRegistry());
    private User user;

    @BeforeEach void setup() {
        user = User.builder().id(UUID.randomUUID()).email("user@example.com").passwordHash("old-hash")
                .role(UserRole.SCOUT).active(true).emailVerified(false).build();
        when(codec.generate()).thenReturn("raw-secret-token");
        when(codec.hash("raw-secret-token")).thenReturn("hashed-token");
    }

    @Test void issueStoresOnlyHashAndPublishesRawTokenAfterTransactionBoundary() {
        service.issueVerification(user);
        verify(verification).save(argThat(token -> token.getTokenHash().equals("hashed-token")
                && !token.getTokenHash().contains("raw-secret-token")
                && token.getExpiresAt().equals(OffsetDateTime.parse("2026-08-28T10:00:00Z"))));
        verify(events).publishEvent(new EmailVerificationRequestedEvent(user.getId(), "raw-secret-token"));
    }

    @Test void validVerificationIsSingleUseAndSameUsedTokenIsIdempotent() {
        EmailVerificationToken token = EmailVerificationToken.builder().user(user).tokenHash("hashed-token")
                .expiresAt(OffsetDateTime.parse("2026-08-28T10:00:00Z")).build();
        when(verification.findByTokenHashForUpdate("hashed-token")).thenReturn(Optional.of(token));
        assertThat(service.verifyEmail("raw-secret-token").message()).contains("verified");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(service.verifyEmail("raw-secret-token").message()).contains("verified");
    }

    @Test void expiredAndInactiveVerificationAreRejectedWithoutActivation() {
        EmailVerificationToken expired = EmailVerificationToken.builder().user(user).tokenHash("hashed-token")
                .expiresAt(OffsetDateTime.parse("2026-08-27T09:59:59Z")).build();
        when(verification.findByTokenHashForUpdate("hashed-token")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.verifyEmail("raw-secret-token")).isInstanceOf(BadRequestException.class);
        user.setActive(false);
        expired.setExpiresAt(OffsetDateTime.parse("2026-08-28T10:00:00Z"));
        assertThatThrownBy(() -> service.verifyEmail("raw-secret-token")).isInstanceOf(BadRequestException.class);
        assertThat(user.isActive()).isFalse();
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test void resendIsGenericAndDoesNothingForVerifiedOrUnknownAccount() {
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        assertThat(service.resendVerification(" USER@example.com ").message()).isEqualTo(AccountRecoveryService.RESEND_MESSAGE);
        verify(verification).invalidateOutstanding(eq(user.getId()), any());
        clearInvocations(verification, events);
        user.setEmailVerified(true);
        service.resendVerification("user@example.com");
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        assertThat(service.resendVerification("missing@example.com").message()).isEqualTo(AccountRecoveryService.RESEND_MESSAGE);
        verifyNoInteractions(verification);
    }

    @Test void forgotIsGenericAndResetChangesPasswordAndRevokesEveryRefreshSession() {
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        assertThat(service.forgotPassword("user@example.com").message()).isEqualTo(AccountRecoveryService.FORGOT_MESSAGE);
        verify(resets).save(argThat(token -> token.getTokenHash().equals("hashed-token")
                && token.getExpiresAt().equals(OffsetDateTime.parse("2026-08-27T11:00:00Z"))));
        verify(events).publishEvent(new PasswordResetRequestedEvent(user.getId(), "raw-secret-token"));

        PasswordResetToken reset = PasswordResetToken.builder().user(user).tokenHash("hashed-token")
                .expiresAt(OffsetDateTime.parse("2026-08-27T11:00:00Z")).build();
        when(resets.findByTokenHashForUpdate("hashed-token")).thenReturn(Optional.of(reset));
        when(passwords.encode("new-password")).thenReturn("new-hash");
        service.resetPassword("raw-secret-token", "new-password");
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(reset.getUsedAt()).isNotNull();
        verify(refresh).deleteAllByUserId(user.getId());
        assertThatThrownBy(() -> service.resetPassword("raw-secret-token", "new-password"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test void unknownForgotResponseMatchesExistingAndExpiredResetIsRejected() {
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        assertThat(service.forgotPassword("missing@example.com").message()).isEqualTo(AccountRecoveryService.FORGOT_MESSAGE);
        PasswordResetToken expired = PasswordResetToken.builder().user(user).tokenHash("hashed-token")
                .expiresAt(OffsetDateTime.parse("2026-08-27T09:00:00Z")).build();
        when(resets.findByTokenHashForUpdate("hashed-token")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.resetPassword("raw-secret-token", "new-password"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(refresh);
    }
    @Test void recoveryTokensRejectExactExpiryEquality() {
        OffsetDateTime boundary = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        when(verification.findByTokenHashForUpdate("hashed-token")).thenReturn(Optional.of(
                EmailVerificationToken.builder().user(user).expiresAt(boundary).build()));
        when(resets.findByTokenHashForUpdate("hashed-token")).thenReturn(Optional.of(
                PasswordResetToken.builder().user(user).expiresAt(boundary).build()));
        assertThatThrownBy(() -> service.verifyEmail("raw-secret-token")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.resetPassword("raw-secret-token", "new-password")).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(passwords, refresh);
    }

}
