package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class StorageService implements FileStorage {


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
        var image = ImageUploadValidator.validate(file,maxFileSizeBytes);
        String filename = UUID.randomUUID()+image.extension();
        Path temporary = null;
        try {
            temporary = Files.createTempFile(rootLocation,"upload-",".tmp");
            Files.write(temporary,image.bytes());
            // Never overwrite an existing generated key, even in the unlikely event of a collision.
            Files.move(temporary,rootLocation.resolve(filename));
            return fileUrlPrefix+filename;
        } catch(IOException e) {
            if(temporary!=null) try {Files.deleteIfExists(temporary);} catch(IOException cleanup) {e.addSuppressed(cleanup);}
            throw new IllegalStateException("Failed to store image",e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        if(!mediaPolicy.isStoredReference(fileUrlPrefix+filename))
            throw new com.eema.backend.exception.NotFoundException("Media not found");
        Path file=rootLocation.resolve(filename).normalize();
        if(!file.startsWith(rootLocation) || !Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS) || !Files.isReadable(file))
            throw new com.eema.backend.exception.NotFoundException("Media not found");
        try {return new UrlResource(file.toUri());}
        catch(MalformedURLException e) {throw new IllegalStateException("Failed to read image",e);}
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
