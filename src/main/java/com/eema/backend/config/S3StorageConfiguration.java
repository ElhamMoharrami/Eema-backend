package com.eema.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageConfiguration {

    @Bean
    S3Client s3Client(@Value("${app.storage.s3.endpoint:}") String endpoint,
                      @Value("${app.storage.s3.region}") String region,
                      @Value("${app.storage.s3.access-key}") String accessKey,
                      @Value("${app.storage.s3.secret-key}") String secretKey,
                      @Value("${app.storage.s3.path-style-access:false}") boolean pathStyleAccess) {
        require(region, "S3_REGION");
        require(accessKey, "S3_ACCESS_KEY");
        require(secretKey, "S3_SECRET_KEY");
        var builder = S3Client.builder()
                .region(Region.of(region))
                .httpClient(UrlConnectionHttpClient.create())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(pathStyleAccess);
        if (endpoint != null && !endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        return builder.build();
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required when S3 storage is selected");
    }
}
