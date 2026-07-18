package com.eema.backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Application properties configuration.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    private final JwtProperties jwt = new JwtProperties();
    private final StorageProperties storage = new StorageProperties();
    private final CorsProperties cors = new CorsProperties();
    private final MollieProperties mollie = new MollieProperties();
    private final ResendProperties resend = new ResendProperties();
    private final CalendlyProperties calendly = new CalendlyProperties();
    @Setter
    private String adminEmailAllowlist;

    public JwtProperties getJwt() {
        return jwt;
    }

    public StorageProperties getStorage() {
        return storage;
    }

    public CorsProperties getCors() {
        return cors;
    }

    public MollieProperties getMollie() {
        return mollie;
    }

    public ResendProperties getResend() {
        return resend;
    }

    public CalendlyProperties getCalendly() {
        return calendly;
    }

    public String getAdminEmailAllowlist() {
        return adminEmailAllowlist;
    }

    /**
     * JWT properties.
     */
    public static class JwtProperties {
        @NotBlank
        private String secret;

        @Positive
        private int accessTokenExpiryMinutes;

        @Positive
        private int refreshTokenExpiryDays;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getAccessTokenExpiryMinutes() {
            return accessTokenExpiryMinutes;
        }

        public void setAccessTokenExpiryMinutes(int accessTokenExpiryMinutes) {
            this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        }

        public int getRefreshTokenExpiryDays() {
            return refreshTokenExpiryDays;
        }

        public void setRefreshTokenExpiryDays(int refreshTokenExpiryDays) {
            this.refreshTokenExpiryDays = refreshTokenExpiryDays;
        }
    }

    /**
     * Storage properties.
     */
    public static class StorageProperties {
        @NotBlank
        private String uploadDir;

        @NotBlank
        private String fileUrlPrefix;

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }

        public void setFileUrlPrefix(String fileUrlPrefix) {
            this.fileUrlPrefix = fileUrlPrefix;
        }
    }

    /**
     * CORS properties.
     */
    public static class CorsProperties {
        @NotEmpty
        private List<String> allowedOrigins;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(java.util.List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    /**
     * Mollie properties.
     */
    @Validated
    public static class MollieProperties {
        @NotBlank
        private String apiKey;  // test_* in dev, live_* in prod

        @NotBlank
        private String webhookSecret;

        @NotBlank
        private String redirectUrl;  // redirect after payment (e.g. https://eema.co/booked)

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public void setRedirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
        }
    }

    /**
     * Resend properties.
     */
    @Validated
    public static class ResendProperties {
        @NotBlank
        private String apiKey;  // Resend API key

        @NotBlank
        private String fromEmail;  // From email address for sending emails

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getFromEmail() {
            return fromEmail;
        }

        public void setFromEmail(String fromEmail) {
            this.fromEmail = fromEmail;
        }
    }

    /**
     * Calendly properties.
     */
    public static class CalendlyProperties {
        @NotBlank
        private String baseUrl;  // Base URL for Calendly links, e.g. https://calendly.com/eema

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}