package com.eema.backend.service;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/** Validate the bytes that will be stored, without rewriting images or their metadata. */
final class ImageUploadValidator {
    // Resource budget: at most 25MP (~100MB ARGB) per decoded image, two decoders per JVM.
    // These are upload safety limits, not a listing-quality or resizing policy.
    static final int MAX_DIMENSION = 10_000;
    static final long MAX_PIXELS = 25_000_000;
    private static final Semaphore DECODERS = new Semaphore(2, true);
    private static final Map<String, String> TYPES = Map.of(
            ".jpg", "image/jpeg", ".jpeg", "image/jpeg", ".png", "image/png", ".webp", "image/webp");

    record Validated(byte[] bytes, String extension, String contentType) {}

    private ImageUploadValidator() {}

    static Validated validate(MultipartFile file, long maxBytes) {
        String name = StringUtils.cleanPath(file.getOriginalFilename());
        if (name == null || name.isBlank()) throw new IllegalArgumentException("File name cannot be empty");
        int dot = name.lastIndexOf('.');
        String extension = dot > 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : "";
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!TYPES.containsKey(extension) || !TYPES.containsValue(type))
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        if (!TYPES.get(extension).equals(type))
            throw new IllegalArgumentException("Image type and file extension must match");
        if (file.isEmpty()) throw new IllegalArgumentException("File cannot be empty");
        if (file.getSize() > maxBytes)
            throw new IllegalArgumentException("Image exceeds the configured maximum size");
        if (maxBytes <= 0 || maxBytes >= Integer.MAX_VALUE)
            throw new IllegalStateException("Invalid upload size configuration");

        boolean acquired = false;
        try {
            DECODERS.acquire();
            acquired = true;
            byte[] bytes;
            try (var stream = file.getInputStream()) {
                bytes = stream.readNBytes((int) maxBytes + 1);
            }
            if (bytes.length == 0) throw new IllegalArgumentException("File cannot be empty");
            if (bytes.length > maxBytes)
                throw new IllegalArgumentException("Image exceeds the configured maximum size");
            decode(bytes, type);
            return new Validated(bytes, extension, type);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Image validation interrupted", e);
        } catch (IOException e) {
            throw corrupt();
        } finally {
            if (acquired) DECODERS.release();
        }
    }

    private static void decode(byte[] bytes, String type) throws IOException {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw corrupt();
            ImageReader reader = readers.next();
            try {
                String decodedType = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg", "jpg" -> "image/jpeg";
                    case "png" -> "image/png";
                    case "webp" -> "image/webp";
                    default -> "";
                };
                if (!type.equals(decodedType))
                    throw new IllegalArgumentException("Image content does not match its declared type");
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS)
                    throw new IllegalArgumentException("Image must be at most 10000 pixels per side and 25 million pixels total");
                AtomicBoolean warning = new AtomicBoolean();
                reader.addIIOReadWarningListener((source, message) -> warning.set(true));
                var image = reader.read(0);
                if (image == null) throw corrupt();
                try {
                    if (warning.get()) throw corrupt();
                } finally {
                    image.flush();
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private static IllegalArgumentException corrupt() {
        return new IllegalArgumentException("Invalid or corrupt image; upload a valid JPEG, PNG, or WebP photo");
    }
}
