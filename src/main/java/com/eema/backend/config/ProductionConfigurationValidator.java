package com.eema.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionConfigurationValidator implements ApplicationRunner {
    private final Environment environment;
    private final AppProperties properties;

    @Override public void run(ApplicationArguments args) {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) return;
        require("SPRING_DATASOURCE_URL");
        require("SPRING_DATASOURCE_USERNAME");
        require("SPRING_DATASOURCE_PASSWORD");
        validateJwt();
        validateHttps(properties.frontendBaseUrl(), "APP_FRONTEND_BASE_URL");
        validateCors();
        validateStorage();
        validateMollie();
        validateEmail();
        validatePayoutDestination();
    }
    private void validatePayoutDestination() {
        if (!environment.getProperty("app.payout-destination.enabled", Boolean.class, true)) return;
        require("PAYOUT_DATA_ENCRYPTION_KEY");
        require("PAYOUT_DATA_FINGERPRINT_KEY");
    }

    private void validateJwt() {
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(properties.jwt().secret()); }
        catch (IllegalArgumentException e) { throw invalid("JWT_SECRET must be valid Base64"); }
        if (decoded.length < 32) throw invalid("JWT_SECRET must contain at least 256 bits");
    }
    private void validateCors() {
        for (String origin : properties.cors().allowedOrigins()) {
            if (origin.contains("*") || !isHttps(origin) || isLocal(origin))
                throw invalid("CORS_ALLOWED_ORIGINS must contain explicit non-local HTTPS origins in production");
        }
    }
    private void validateStorage() {
        if (!"s3".equals(properties.storage().provider()))
            throw invalid("app.storage.provider must be s3 in production");
        String publicBucket = require("S3_BUCKET_PUBLIC_MEDIA");
        String privateBucket = require("S3_BUCKET_PRIVATE_DOCUMENTS");
        require("S3_REGION"); require("S3_ACCESS_KEY"); require("S3_SECRET_KEY");
        if (publicBucket.equals(privateBucket)) throw invalid("Public media and private documents must use different S3 buckets");
    }
    private void validateMollie() {
        String key = properties.mollie().apiKey();
        if (key == null || key.isBlank()) throw invalid("MOLLIE_API_KEY is required in production");
        String mode = key.startsWith("live_") ? "live" : key.startsWith("test_") ? "test" : "unknown";
        log.info("Mollie configured: yes, mode={}", mode);
        validateHttps(properties.mollie().webhookUrl(), "MOLLIE_WEBHOOK_URL");
        validateHttps(properties.mollie().redirectUrl(), "MOLLIE_REDIRECT_URL");
    }
    private void validateEmail() {
        if (!properties.resend().enabled()) return;
        if (blank(properties.resend().apiKey()) || blank(properties.resend().fromEmail()))
            throw invalid("RESEND_API_KEY and RESEND_FROM_EMAIL are required when email is enabled");
    }
    private String require(String key) {
        String value = environment.getProperty(key);
        if (blank(value)) throw invalid(key + " is required in production");
        return value;
    }
    private static void validateHttps(String value, String name) {
        if (!isHttps(value) || isLocal(value)) throw invalid(name + " must be a non-local HTTPS URL in production");
    }
    private static boolean isHttps(String value) {
        try { return value != null && "https".equalsIgnoreCase(URI.create(value).getScheme()); }
        catch (IllegalArgumentException e) { return false; }
    }
    private static boolean isLocal(String value) {
        try { String host = URI.create(value).getHost(); return host == null || host.equals("localhost") || host.equals("127.0.0.1"); }
        catch (IllegalArgumentException e) { return true; }
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static IllegalStateException invalid(String message) { return new IllegalStateException("Invalid production configuration: " + message); }
}
