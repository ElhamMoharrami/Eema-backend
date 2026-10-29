package com.eema.backend.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

@Component
public class OneTimeTokenCodec {
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    @org.springframework.beans.factory.annotation.Autowired
    public OneTimeTokenCodec(com.eema.backend.config.AppProperties properties) {
        this.key = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
    }

    /** Isolated unit fixtures only; production always uses the configured persistent key. */
    public OneTimeTokenCodec() { key = new byte[32]; random.nextBytes(key); }

    public String reconstruct(String nonce) {
        if (nonce == null || !nonce.matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Invalid auth nonce");
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            byte[] signature = mac.doFinal(("eema/auth-notification/v1:" + nonce).getBytes(StandardCharsets.UTF_8));
            return nonce + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException impossible) { throw new IllegalStateException("HMAC unavailable", impossible); }
    }

    public String nonce(String raw) {
        String nonce = raw.substring(0, raw.indexOf('.'));
        if (!MessageDigest.isEqual(raw.getBytes(StandardCharsets.UTF_8), reconstruct(nonce).getBytes(StandardCharsets.UTF_8)))
            throw new IllegalArgumentException("Invalid auth token derivation");
        return nonce;
    }

    public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return reconstruct(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("Token is required");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
