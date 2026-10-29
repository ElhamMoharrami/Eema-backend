package com.eema.backend.security;

import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Service for handling JWT operations.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppProperties appProperties;

    private SecretKey secretKey;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @PostConstruct
    public void init() {
        String secret = appProperties.jwt().secret();

        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret must be configured");
        }

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid JWT secret", e);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 256 bits (32 bytes). Current length: "
                            + keyBytes.length
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .requireIssuer("eema")
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Generate an access token for the given user.
     *
     * @param user the user to generate token for
     * @return JWT access token
     */
    public String generateAccessToken(User user) {
        long expirationMillis = appProperties.jwt().accessTokenExpiryMinutes() * 60 * 1000L;
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .setIssuer("eema")
                .compact();
    }

    /**
     * Generate a cryptographically random refresh token.
     *
     * @return random refresh token (raw token, not hashed)
     */
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
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
    public Claims validateToken(String token) {
        return parseClaims(token);
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token the JWT token
     * @return user ID as UUID
     */
    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        String userIdStr = claims.getSubject();
        return UUID.fromString(userIdStr);
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract email from JWT token.
     *
     * @param token the JWT token
     * @return email address
     */
    public String extractEmail(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Extract role from JWT token.
     *
     * @param token the JWT token
     * @return user role
     */
    public UserRole extractRole(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        return UserRole.valueOf(role);
    }

    /**
     * Hash a refresh token using SHA-256.
     *
     * @param token raw token to hash
     * @return hexadecimal string representation of the hash string
     */
    public String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
