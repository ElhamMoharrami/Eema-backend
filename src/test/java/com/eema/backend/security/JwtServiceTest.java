package com.eema.backend.security;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final byte[] KEY = "01234567890123456789012345678901".getBytes();

    @Test
    void validatesSignatureExpiryAndExpectedIssuer() {
        AppProperties properties = new AppProperties(
                new AppProperties.JwtProperties(Base64.getEncoder().encodeToString(KEY), 15, 7),
                null, null, null, null, null, null, null, null);
        JwtService service = new JwtService(properties);
        service.init();
        User user = User.builder().id(UUID.randomUUID()).email("scout@example.com").role(UserRole.SCOUT).build();
        assertEquals("eema", service.validateToken(service.generateAccessToken(user)).getIssuer());

        String wrongIssuer = Jwts.builder().setSubject(user.getId().toString()).setIssuer("other")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(KEY)).compact();
        assertThrows(IncorrectClaimException.class, () -> service.validateToken(wrongIssuer));
    }
}
