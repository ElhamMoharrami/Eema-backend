package com.eema.backend.service;

import com.eema.backend.dto.request.RegisterRequest;
import com.eema.backend.dto.request.LoginRequest;
import com.eema.backend.dto.request.RefreshRequest;
import com.eema.backend.dto.response.AuthResponse;
import com.eema.backend.dto.response.UserDto;
import com.eema.backend.entity.RefreshToken;
import com.eema.backend.entity.User;
import com.eema.backend.entity.UserRole;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;

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

        // Check email uniqueness (case-insensitive)
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(request.password());

        // Create and save user
        User user = User.builder()
                .email(request.email())
                .passwordHash(hashedPassword)
                .role(request.role())
                .fullName(request.fullName())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(savedUser);
        String rawRefreshToken = jwtService.generateRefreshToken();
        String refreshTokenHash = hashRefreshToken(rawRefreshToken);

        // Save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(savedUser)
                .tokenHash(refreshTokenHash)
                .expiresAt(OffsetDateTime.now().plusDays(appProperties.getJwt().getRefreshTokenExpiryDays()))
                .revoked(false)
                .build();

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        // Return response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getAccessTokenExpiryMinutes() * 60L)
                .user(userMapper.toDto(savedUser))
                .build();
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
        // Find user by email (case-insensitive)
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check if user is active
        if (!user.isActive()) {
            throw new ForbiddenException("Account is inactive");
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken();
        String refreshTokenHash = hashRefreshToken(rawRefreshToken);

        // Save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenHash)
                .expiresAt(OffsetDateTime.now().plusDays(appProperties.getJwt().getRefreshTokenExpiryDays()))
                .revoked(false)
                .build();

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        // Return response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getAccessTokenExpiryMinutes() * 60L)
                .user(userMapper.toDto(user))
                .build();
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request refresh token request
     * @return authentication response with new tokens
     * @throws UnauthorizedException if refresh token is invalid
     */
    public AuthResponse refresh(RefreshRequest request) {
        // Hash the incoming token
        String tokenHash = hashRefreshToken(request.refreshToken());

        // Find refresh token by hash
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Validate token is not revoked and not expired
        if (refreshToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Get the user
        User user = refreshToken.getUser();

        // Revoke the old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate new tokens
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRefreshToken();
        String newRefreshTokenHash = hashRefreshToken(rawRefreshToken);

        // Save new refresh token
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newRefreshTokenHash)
                .expiresAt(OffsetDateTime.now().plusDays(appProperties.getJwt().getRefreshTokenExpiryDays()))
                .revoked(false)
                .build();

        RefreshToken savedNewRefreshToken = refreshTokenRepository.save(newRefreshToken);

        // Return response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getJwt().getAccessTokenExpiryMinutes() * 60L)
                .user(userMapper.toDto(user))
                .build();
    }

    /**
     * Hash a refresh token using SHA-256.
     *
     * @param token raw token to hash
     * @return hexadecimal string representation of the hash
     */
    private String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}