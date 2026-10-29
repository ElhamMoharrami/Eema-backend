package com.eema.backend.service;

import com.eema.backend.dto.request.LoginRequest;
import com.eema.backend.dto.request.RefreshRequest;
import com.eema.backend.dto.request.RegisterRequest;
import com.eema.backend.dto.response.AuthResponse;
import com.eema.backend.dto.response.UserDto;
import com.eema.backend.entity.RefreshToken;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.exception.ForbiddenException;
import com.eema.backend.exception.UnauthorizedException;
import com.eema.backend.mapper.UserMapper;
import com.eema.backend.repository.RefreshTokenRepository;
import com.eema.backend.repository.UserRepository;
import com.eema.backend.security.JwtService;
import com.eema.backend.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Service for authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AppProperties appProperties;

    /**
     * Register a new user.
     *
     * @param request registration request
     * @return authentication response with tokens
     * @throws BadRequestException if role is invalid
     * @throws ConflictException if email already exists
     */
    public AuthResponse register(RegisterRequest request) {
        // Validate role - only SCOUT or OWNER allowed
        if (request.role() == UserRole.ADMIN) {
            throw new BadRequestException("ADMIN role cannot be registered through this endpoint");
        }

        // Normalize email
        String normalizedEmail = normalizeEmail(request.email());

        // Check email uniqueness (case-insensitive)
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email already in use: " + request.email());
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(request.password());

        // Create and save user
        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(hashedPassword)
                .role(request.role())
                .fullName(request.fullName())
                .active(true)
                .build();

        userRepository.save(user);

        // Generate and save tokens
        return createAuthentication(user);
    }

    /**
     * Login a user.
     *
     * @param request login request
     * @return authentication response with tokens
     * @throws UnauthorizedException if credentials are invalid
     * @throws ForbiddenException if user is inactive
     */
    public AuthResponse login(LoginRequest request) {
        // Normalize email
        String normalizedEmail = normalizeEmail(request.email());

        // Find user by email (case-insensitive)
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check if user is active
        if (!user.isActive()) {
            throw new ForbiddenException("Account is inactive");
        }

        // Generate and save tokens
        return createAuthentication(user);
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request refresh token request
     * @return authentication response with new tokens
     * @throws UnauthorizedException if refresh token is invalid
     * @throws ForbiddenException if user is inactive
     */
    public AuthResponse refresh(RefreshRequest request) {
        // Hash the incoming token
        String tokenHash = jwtService.hashRefreshToken(request.refreshToken());

        // Find refresh token by hash
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Validate token is not revoked and expiration
        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }
        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Get the user
        User user = refreshToken.getUser();

        // Check if user is active
        if (!user.isActive()) {
            throw new ForbiddenException("Account is inactive");
        }

        // Revoke the old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate and save new tokens
        return createAuthentication(user);
    }

    /**
     * Create authentication response with tokens for the given user.
     *
     * @param user the user to generate tokens for
     * @return authentication response with tokens
     */
    private AuthResponse createAuthentication(User user) {
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken();
        String refreshTokenHash = jwtService.hashRefreshToken(rawRefreshToken);

        // Save refresh token
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenHash)
                .expiresAt(OffsetDateTime.now().plusDays(appProperties.jwt().refreshTokenExpiryDays()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        // Return response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.jwt().accessTokenExpiryMinutes() * 60L)
                .user(userMapper.toDto(user))
                .build();
    }

    /**
     * Get current user information from authentication.
     *
     * @param authentication the current authentication
     * @return user data for the currently authenticated user
     * @throws IllegalStateException if user is not found
     */
    public UserDto getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        return userMapper.toDto(user);
    }

    /**
     * Normalize email address by trimming and converting to lowercase.
     *
     * @param email the email to normalize
     * @return normalized email
     */
    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}