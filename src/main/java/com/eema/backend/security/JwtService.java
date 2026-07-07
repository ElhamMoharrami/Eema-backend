package com.eema.backend.security;

import com.eema.backend.entity.User;
import com.eema.backend.entity.UserRole;
import com.eema.backend.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Service for handling JWT operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final AppProperties appProperties;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        String secret = appProperties.getJwt().getSecret();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        // Decode base64 secret if needed, otherwise use raw bytes
        byte[] keyBytes;
        try {
            // Try to decode as base64 first
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            // If not valid base64, use raw bytes
            keyBytes = secret.getBytes();
        }

        // Ensure key is at least 256 bits (32 bytes)
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes)");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate an access token for the given user.
     *
     * @param user the user to generate token for
     * @return JWT access token
     */
    public String generateAccessToken(User user) {
        long expirationMillis = appProperties.getJwt().getAccessTokenExpiryMinutes() * 60 * 1000L;
        java.util.Date now = new java.util.Date();
        java.util.Date expiryDate = new java.util.Date(now.getTime() + expirationMillis);

        return io.jsonwebtoken.Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate a cryptographically random refresh token.
     *
     * @return random refresh token (raw token, not hashed)
     */
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Validate and parse a JWT token.
     *
     * @param token the JWT token to validate
     * @return parsed claims
     * @throws io.jsonwebtoken.ExpiredJwtException if token is expired
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
    public io.jsonwebtoken.Claims validateToken(String token) {
        return io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token the JWT token
     * @return user ID as UUID
     */
    public java.util.UUID extractUserId(String token) {
        String userIdStr = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return java.util.UUID.fromString(userIdStr);
    }

    /**
     * Extract email from JWT token.
     *
     * @param token the JWT token
     * @return email address
     */
    public String extractEmail(String token) {
        return io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("email", String.class);
    }

    /**
     * Extract role from JWT token.
     *
     * @param token the JWT token
     * @return user role
     */
    public UserRole extractRole(String token) {
        String roleStr = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
        return UserRole.valueOf(roleStr);
    }
}