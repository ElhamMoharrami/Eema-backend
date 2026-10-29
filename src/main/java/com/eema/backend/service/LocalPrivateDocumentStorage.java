package com.eema.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.io.IOException;
import java.nio.file.*;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalPrivateDocumentStorage implements PrivateDocumentStorage {
    private final Path root;

    public LocalPrivateDocumentStorage(@Value("${app.documents.storage-dir:/tmp/eema-private-documents}") String directory) {
        root = Paths.get(directory).toAbsolutePath().normalize();
        try { Files.createDirectories(root); }
        catch (IOException e) { throw new IllegalStateException("Could not initialize private document storage", e); }
    }

    @Override public String store(String reference, byte[] content) {
        Path target = resolve(reference);
        try {
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), ".document-", ".tmp");
            try {
                Files.write(temporary, content, StandardOpenOption.TRUNCATE_EXISTING);
                try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target); }
            } finally { Files.deleteIfExists(temporary); }
            return reference;
        } catch (FileAlreadyExistsException e) {
            throw new IllegalStateException("Private document already exists", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store private document", e);
        }
    }
    @Override public byte[] load(String reference) {
        try { return Files.readAllBytes(resolve(reference)); }
        catch (IOException e) { throw new IllegalStateException("Could not read private document", e); }
    }
    @Override public void delete(String reference) {
        try { Files.deleteIfExists(resolve(reference)); }
        catch (IOException e) { throw new IllegalStateException("Could not delete private document", e); }
    }
    private Path resolve(String reference) {
        if (reference == null || reference.isBlank() || reference.startsWith("/")) throw new IllegalArgumentException("Invalid document reference");
        Path target = root.resolve(reference).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid document reference");
        return target;
    }
}
