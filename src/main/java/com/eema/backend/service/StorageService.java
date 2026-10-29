package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;
import java.util.Locale;
import java.util.Set;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class StorageService implements FileStorage {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final Path rootLocation;
    private final String fileUrlPrefix;
    private final VenueMediaPolicy mediaPolicy;
    private final long maxFileSizeBytes;

    public StorageService(AppProperties appProperties) {
        this.rootLocation = Paths.get(appProperties.storage().uploadDir()).toAbsolutePath().normalize();
        String configuredPrefix = appProperties.storage().fileUrlPrefix();
        this.fileUrlPrefix = configuredPrefix.endsWith("/") ? configuredPrefix : configuredPrefix + "/";
        this.mediaPolicy = new VenueMediaPolicy(appProperties);
        this.maxFileSizeBytes = appProperties.storage().maxFileSizeBytes();

        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("Image exceeds the configured maximum size");
        }

        // Extract file extension
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = filename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image file extension");
        }

        // Generate unique filename to prevent collisions and path traversal
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Failed to store empty file");
            }
            Path destination = this.rootLocation.resolve(uniqueFilename).normalize();
            if (!destination.startsWith(this.rootLocation.toAbsolutePath().normalize())) {
                throw new IllegalArgumentException("Invalid storage path");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return fileUrlPrefix + uniqueFilename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            if (!file.startsWith(rootLocation.toAbsolutePath().normalize())) {
                throw new IllegalArgumentException("Invalid file path");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    public void delete(String storageReference) {
        String filename = mediaPolicy.filename(storageReference);
        Path target = rootLocation.resolve(filename).normalize();
        if (!target.startsWith(rootLocation)) throw new IllegalArgumentException("Invalid storage path");
        try { Files.deleteIfExists(target); }
        catch (IOException exception) { throw new IllegalStateException("Failed to delete stored upload", exception); }
    }

}
