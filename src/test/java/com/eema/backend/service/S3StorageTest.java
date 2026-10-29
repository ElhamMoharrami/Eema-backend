package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3StorageTest {
    private final S3Client client = mock(S3Client.class);

    @Test void publicUploadUsesConfiguredPublicBucketAndGeneratedKey() {
        S3PublicMediaStorage storage = new S3PublicMediaStorage(client, properties(), "venue-media", "media/");
        String reference = storage.store(new MockMultipartFile("file", "../../photo.jpg", "image/jpeg", new byte[]{1, 2}));
        assertThat(reference).matches("/api/upload/files/[0-9a-f-]{36}\\.jpg");
        verify(client).putObject(org.mockito.ArgumentMatchers.argThat((PutObjectRequest request) ->
                request.bucket().equals("venue-media") && request.key().matches("media/[0-9a-f-]{36}\\.jpg")), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test void rejectsOversizedAndUnsupportedImages() {
        S3PublicMediaStorage storage = new S3PublicMediaStorage(client, properties(), "venue-media", "media/");
        assertThatThrownBy(() -> storage.store(new MockMultipartFile("file", "x.gif", "image/gif", new byte[]{1})))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void privateStorageRejectsTraversal() {
        S3PrivateDocumentStorage storage = new S3PrivateDocumentStorage(client, "documents", "private/");
        assertThatThrownBy(() -> storage.store("../receipt.pdf", new byte[]{1})).isInstanceOf(IllegalArgumentException.class);
    }

    private static AppProperties properties() {
        return new AppProperties(null, new AppProperties.StorageProperties("s3", "/tmp/unused", "/api/upload/files/", 10),
                new AppProperties.CorsProperties(List.of("https://app.example")), null, null, null, null, null, null);
    }
}
