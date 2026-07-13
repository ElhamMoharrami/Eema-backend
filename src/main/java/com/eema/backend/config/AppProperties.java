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

        public String getFileUrlPrefix() {
            return fileUrlPrefix;
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
}