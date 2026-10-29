package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.User;
import com.eema.backend.exception.CancellationTermsChangedException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Objects;

/** Consent metadata only: financial authority remains the locked booking and policy evaluator. */
@Component
public class CancellationPreviewProof {
    private static final String PURPOSE = "eema:cancellation-preview:v1";
    private final SecretKey key;

    public CancellationPreviewProof(AppProperties properties) {
        try {
            byte[] root = Base64.getDecoder().decode(properties.jwt().secret());
            if (root.length < 32) throw new IllegalArgumentException("Secret must contain at least 32 bytes");
            // Separate signing domain: a preview proof cannot validate as an access JWT.
            Mac derivation = Mac.getInstance("HmacSHA256");
            derivation.init(new SecretKeySpec(root, "HmacSHA256"));
            key = Keys.hmacShaKeyFor(derivation.doFinal(PURPOSE.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cancellation preview signing key configuration is invalid");
        }
    }

    public String issue(Booking booking, User actor, BookingCancellationPolicy.CancellationDecision decision,
                        OffsetDateTime now) {
        return Jwts.builder().setIssuer(PURPOSE).setSubject(actor.getId().toString())
                .claim("role", actor.getRole().name()).claim("booking", booking.getId().toString())
                .claim("result", fingerprint(booking, decision))
                .setExpiration(Date.from(now.plusMinutes(10).toInstant())).signWith(key).compact();
    }

    public void validate(String token, Booking booking, User actor,
                         BookingCancellationPolicy.CancellationDecision decision, OffsetDateTime now) {
        try {
            if (token == null || token.length() > 2048) throw new IllegalArgumentException();
            var claims = Jwts.parserBuilder().setSigningKey(key).requireIssuer(PURPOSE)
                    .setClock(() -> Date.from(now.toInstant())).build().parseClaimsJws(token).getBody();
            if (!now.toInstant().isBefore(claims.getExpiration().toInstant())
                    || !actor.getId().toString().equals(claims.getSubject())
                    || !actor.getRole().name().equals(claims.get("role", String.class))
                    || !booking.getId().toString().equals(claims.get("booking", String.class))
                    || !fingerprint(booking, decision).equals(claims.get("result", String.class))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            // Never propagate JWT exceptions: their messages can contain the proof.
            throw new CancellationTermsChangedException();
        }
    }

    private String fingerprint(Booking booking, BookingCancellationPolicy.CancellationDecision decision) {
        String canonical = String.join("|", decision.toString(),
                Objects.toString(booking.getCancellationPolicyVersion()),
                Objects.toString(booking.getMolliePaymentId()),
                Objects.toString(booking.getGracePeriodEndsAt()), Objects.toString(booking.getFullRefundUntil()),
                Objects.toString(booking.getPartialRefundUntil()), Objects.toString(booking.getPartialRefundPercentage()),
                Objects.toString(booking.getCommissionPercentage()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable");
        }
    }
}
