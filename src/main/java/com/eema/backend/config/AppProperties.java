package com.eema.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(

        @Valid JwtProperties jwt, @Valid StorageProperties storage, @Valid CorsProperties cors,
        @Valid MollieProperties mollie, @Valid ResendProperties resend, @Valid CalendlyProperties calendly,
        @Valid CommissionProperties commission,

        String adminEmailAllowlist

) {

    public record JwtProperties(@NotBlank String secret, @Positive int accessTokenExpiryMinutes,
                                @Positive int refreshTokenExpiryDays) {
    }

    public record StorageProperties(@NotBlank String uploadDir, @NotBlank String fileUrlPrefix) {
    }

    public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
    }

    public record MollieProperties(@NotBlank String apiKey, @NotBlank String redirectUrl, @NotBlank String webhookUrl) {
    }

    public record ResendProperties(@NotBlank String apiKey, @NotBlank String fromEmail) {
    }

    public record CalendlyProperties(@NotBlank String baseUrl) {
    }

    public record CommissionProperties(@DecimalMin(value = "0.0", inclusive = false) BigDecimal percentage) {
    }
}