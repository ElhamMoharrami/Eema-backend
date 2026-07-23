package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class StorageService {

    private final Path rootLocation;
    private final String fileUrlPrefix;

    public StorageService(AppProperties appProperties) {
        this.rootLocation = Paths.get(appProperties.getStorage().getUploadDir());
        this.fileUrlPrefix = appProperties.getStorage().getFileUrlPrefix();

        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage", e);
        }
    }

    public String store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        // Extract file extension
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = filename.substring(dotIndex);
        }

        // Generate unique filename to prevent collisions and path traversal
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Failed to store empty file");
            }
            Files.copy(file.getInputStream(), this.rootLocation.resolve(uniqueFilename), StandardCopyOption.REPLACE_EXISTING);
            return fileUrlPrefix + uniqueFilename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public void deleteAll() {
        try {
            Files.walk(rootLocation)
                    .filter(path -> !path.equals(rootLocation))
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete files", e);
        }
    }
}