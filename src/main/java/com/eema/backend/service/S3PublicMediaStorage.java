package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3PublicMediaStorage implements FileStorage {
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
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
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = extension(original);
        if (file.isEmpty()) throw new IllegalArgumentException("Failed to store empty file");
        if (!TYPES.contains(type) || !EXTENSIONS.contains(extension))
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        if (file.getSize() > maxBytes) throw new IllegalArgumentException("Image exceeds the configured maximum size");
        String filename = UUID.randomUUID() + extension;
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(prefix + filename).contentType(type).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return urlPrefix + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read uploaded image", exception);
        }
    }

    @Override public Resource loadAsResource(String filename) {
        validateFilename(filename);
        byte[] bytes = client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(prefix + filename).build()).asByteArray();
        return new ByteArrayResource(bytes) { @Override public String getFilename() { return filename; } };
    }

    @Override public void delete(String storageReference) {
        String filename = mediaPolicy.filename(storageReference);
        validateFilename(filename);
        client.deleteObject(request -> request.bucket(bucket).key(prefix + filename));
    }

    private static String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot).toLowerCase(Locale.ROOT) : "";
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
