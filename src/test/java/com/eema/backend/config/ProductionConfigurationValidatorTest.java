package com.eema.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationValidatorTest {
    @Test void rejectsLocalStorageInProduction() {
        MockEnvironment environment = productionEnvironment();
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment, properties("local", strongSecret(), List.of("https://app.eema.example")));
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments())).hasMessageContaining("provider must be s3");
    }

    @Test void rejectsWeakJwtInProduction() {
        MockEnvironment environment = productionEnvironment();
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment,
                properties("s3", Base64.getEncoder().encodeToString("short".getBytes()), List.of("https://app.eema.example")));
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments())).hasMessageContaining("256 bits");
    }

    @Test void rejectsWildcardCorsInProduction() {
        MockEnvironment environment = productionEnvironment();
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment,
                properties("s3", strongSecret(), List.of("*")));
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments())).hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }

    @Test void rejectsSharedBuckets() {
        MockEnvironment environment = productionEnvironment().withProperty("S3_BUCKET_PRIVATE_DOCUMENTS", "media");
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment,
                properties("s3", strongSecret(), List.of("https://app.eema.example")));
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments())).hasMessageContaining("different S3 buckets");
    }

    @Test void requiresS3Credentials() {
        MockEnvironment environment = productionEnvironment().withProperty("S3_SECRET_KEY", "");
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment,
                properties("s3", strongSecret(), List.of("https://app.eema.example")));
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments())).hasMessageContaining("S3_SECRET_KEY");
    }

    @Test void acceptsCompleteProductionConfigurationWithoutCallingProviders() {
        var environment=productionEnvironment()
                .withProperty("PAYOUT_DATA_ENCRYPTION_KEY",strongSecret())
                .withProperty("PAYOUT_DATA_FINGERPRINT_KEY",strongSecret());
        new ProductionConfigurationValidator(environment,properties("s3",strongSecret(),List.of("https://app.eema.example")))
                .run(new DefaultApplicationArguments());
    }

    @ParameterizedTest
    @ValueSource(strings={"SPRING_DATASOURCE_URL","SPRING_DATASOURCE_USERNAME","SPRING_DATASOURCE_PASSWORD",
            "S3_BUCKET_PUBLIC_MEDIA","S3_BUCKET_PRIVATE_DOCUMENTS","S3_REGION","S3_ACCESS_KEY",
            "PAYOUT_DATA_ENCRYPTION_KEY","PAYOUT_DATA_FINGERPRINT_KEY"})
    void rejectsMissingRequiredProductionValue(String name) {
        var environment=productionEnvironment()
                .withProperty("PAYOUT_DATA_ENCRYPTION_KEY",strongSecret())
                .withProperty("PAYOUT_DATA_FINGERPRINT_KEY",strongSecret()).withProperty(name,"");
        var validator=new ProductionConfigurationValidator(environment,properties("s3",strongSecret(),List.of("https://app.eema.example")));
        assertThatThrownBy(()->validator.run(new DefaultApplicationArguments())).hasMessageContaining(name);
    }

    private static MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://db/eema")
                .withProperty("SPRING_DATASOURCE_USERNAME", "eema")
                .withProperty("SPRING_DATASOURCE_PASSWORD", "secret")
                .withProperty("S3_BUCKET_PUBLIC_MEDIA", "media")
                .withProperty("S3_BUCKET_PRIVATE_DOCUMENTS", "documents")
                .withProperty("S3_REGION", "eu-west-1").withProperty("S3_ACCESS_KEY", "access")
                .withProperty("S3_SECRET_KEY", "secret");
        environment.setActiveProfiles("prod");
        return environment;
    }
    private static AppProperties properties(String provider, String jwt, List<String> origins) {
        return new AppProperties(new AppProperties.JwtProperties(jwt, 15, 7),
                new AppProperties.StorageProperties(provider, "/tmp/uploads", "/api/upload/files/", 5_242_880),
                new AppProperties.CorsProperties(origins),
                new AppProperties.MollieProperties("live_example", "https://app.eema.example/booked", "https://api.eema.example/webhook", "", false, 1, 1),
                new AppProperties.ResendProperties(false, "", ""), new AppProperties.CalendlyProperties(""),
                new AppProperties.CommissionProperties(BigDecimal.TEN), "https://app.eema.example", "");
    }
    private static String strongSecret() { return Base64.getEncoder().encodeToString(new byte[32]); }
}
