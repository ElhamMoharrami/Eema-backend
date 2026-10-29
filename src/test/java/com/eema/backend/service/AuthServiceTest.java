package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.request.LoginRequest;
import com.eema.backend.dto.request.RegisterRequest;
import com.eema.backend.dto.request.RefreshRequest;
import com.eema.backend.entity.RefreshToken;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.exception.UnauthorizedException;
import com.eema.backend.mapper.UserMapper;
import com.eema.backend.repository.RefreshTokenRepository;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AppProperties properties = new AppProperties(
            new AppProperties.JwtProperties("unused", 15, 7), null, null, null, null, null, null, null);
    private final AuthService service = new AuthService(users, tokens, passwords, jwt, new UserMapper(), properties);

    @Test
    void loginRejectsInactiveUser() {
        User user = User.builder().email("x@example.com").passwordHash("hash").role(UserRole.SCOUT).active(false).build();
        when(users.findByEmailIgnoreCase("x@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("password1", "hash")).thenReturn(true);
        assertThrows(ForbiddenException.class, () -> service.login(new LoginRequest("x@example.com", "password1")));
    }

    @Test
    void registerAndLoginIssueTokensWithoutPersistingRawRefreshToken() {
        when(passwords.encode("password1")).thenReturn("hash");
        when(jwt.generateAccessToken(any())).thenReturn("access");
        when(jwt.generateRefreshToken()).thenReturn("raw-refresh");
        when(jwt.hashRefreshToken("raw-refresh")).thenReturn("hashed-refresh");
        assertEquals("raw-refresh", service.register(new RegisterRequest(
                "New@Example.com", "password1", "New Scout", UserRole.SCOUT)).refreshToken());
        verify(tokens).save(argThat(token -> "hashed-refresh".equals(token.getTokenHash())));

        User active = User.builder().email("new@example.com").passwordHash("hash").role(UserRole.SCOUT).active(true).build();
        when(users.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.of(active));
        when(passwords.matches("password1", "hash")).thenReturn(true);
        assertEquals("access", service.login(new LoginRequest("new@example.com", "password1")).accessToken());
    }

    @Test
    void refreshRotatesAndRejectsReuse() {
        User user = User.builder().id(UUID.randomUUID()).email("x@example.com").role(UserRole.SCOUT).active(true).build();
        RefreshToken token = RefreshToken.builder().user(user).tokenHash("hash")
                .expiresAt(OffsetDateTime.now().plusDays(1)).build();
        when(jwt.hashRefreshToken("raw-token")).thenReturn("hash");
        when(tokens.findByTokenHashForUpdate("hash")).thenReturn(Optional.of(token));
        when(jwt.generateAccessToken(user)).thenReturn("access");
        when(jwt.generateRefreshToken()).thenReturn("new-refresh");
        when(jwt.hashRefreshToken("new-refresh")).thenReturn("new-hash");

        assertEquals("new-refresh", service.refresh(new RefreshRequest("raw-token")).refreshToken());
        assertTrue(token.isRevoked());
        assertThrows(UnauthorizedException.class, () -> service.refresh(new RefreshRequest("raw-token")));
    }
}
