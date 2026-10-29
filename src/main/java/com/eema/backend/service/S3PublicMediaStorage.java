package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3PublicMediaStorage implements FileStorage {
    private final S3Client client;
    private final String bucket;
    private final String prefix;
    private final String urlPrefix;
    private final long maxBytes;
    private final VenueMediaPolicy mediaPolicy;

    public S3PublicMediaStorage(S3Client client, AppProperties properties,
                                @Value("${app.storage.s3.public-media-bucket}") String bucket,
                                @Value("${app.storage.s3.public-media-prefix:venue-media/}") String prefix) {
        this.client = client;
        this.bucket = bucket;
        this.prefix = normalizedPrefix(prefix);
        this.urlPrefix = trailingSlash(properties.storage().fileUrlPrefix());
        this.maxBytes = properties.storage().maxFileSizeBytes();
        this.mediaPolicy = new VenueMediaPolicy(properties);
    }

    @Override public String store(MultipartFile file) {
        var image=ImageUploadValidator.validate(file,maxBytes);
        String filename=UUID.randomUUID()+image.extension();
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(prefix+filename).contentType(image.contentType())
                        .ifNoneMatch("*").build(),RequestBody.fromBytes(image.bytes()));
        return urlPrefix+filename;
    }

    @Override public Resource loadAsResource(String filename) {
        if(!mediaPolicy.isStoredReference(urlPrefix+filename)) throw new com.eema.backend.exception.NotFoundException("Media not found");
        try {
            byte[] bytes=client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(prefix+filename).build()).asByteArray();
            return new ByteArrayResource(bytes) { @Override public String getFilename() { return filename; } };
        } catch(software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            throw new com.eema.backend.exception.NotFoundException("Media not found");
        }
    }

    @Override public void delete(String storageReference) {
        String filename = mediaPolicy.filename(storageReference);
        validateFilename(filename);
        client.deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder().bucket(bucket).key(prefix+filename).build());
    }

    private static void validateFilename(String filename) {
        if (filename == null || !filename.matches("[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp)"))
            throw new IllegalArgumentException("Invalid media reference");
    }
    private static String normalizedPrefix(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.startsWith("/") || value.contains("..")) throw new IllegalArgumentException("Invalid S3 media prefix");
        return trailingSlash(value);
    }
    private static String trailingSlash(String value) { return value.endsWith("/") ? value : value + "/"; }
}
