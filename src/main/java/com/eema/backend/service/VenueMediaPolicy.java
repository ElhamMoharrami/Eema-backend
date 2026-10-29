package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class VenueMediaPolicy {
    public static final int MAX_PHOTOS = 12;
    private static final Pattern STORED_FILENAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.(?:jpg|jpeg|png|webp)",
            Pattern.CASE_INSENSITIVE);

    private final String prefix;

    public VenueMediaPolicy(AppProperties properties) {
        this.prefix = normalizePrefix(properties.storage().fileUrlPrefix());
    }

    public List<String> validatePhotos(List<String> references) {
        if (references == null) throw new BadRequestException("photoPaths is required");
        if (references.size() > MAX_PHOTOS) throw new BadRequestException("A venue can have at most " + MAX_PHOTOS + " photos");
        Set<String> unique = new HashSet<>();
        for (String reference : references) {
            if (!isStoredReference(reference)) throw new BadRequestException("Invalid Eema photo reference");
            if (!unique.add(reference)) throw new BadRequestException("Duplicate photo references are not allowed");
        }
        return List.copyOf(references);
    }

    public void validateClipUrl(String clipUrl) {
        if (clipUrl == null || clipUrl.isBlank()) return;
        if (clipUrl.length() > 500) throw new BadRequestException("clipUrl is too long");
        try {
            URI uri = URI.create(clipUrl);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) throw new BadRequestException("clipUrl must be an absolute HTTP(S) URL");
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("clipUrl must be a valid URL");
        }
    }

    public void validateTimezone(String timezone) {
        try { ZoneId.of(timezone); }
        catch (DateTimeException exception) { throw new BadRequestException("timezone must be a valid IANA timezone"); }
    }

    public boolean isStoredReference(String reference) {
        if (reference == null || !reference.startsWith(prefix)) return false;
        String filename = reference.substring(prefix.length());
        return !filename.contains("/") && !filename.contains("\\") && STORED_FILENAME.matcher(filename).matches();
    }

    String filename(String reference) {
        if (!isStoredReference(reference)) throw new IllegalArgumentException("Invalid Eema storage reference");
        return reference.substring(prefix.length());
    }

    private String normalizePrefix(String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("FILE_URL_PREFIX is missing");
        return value.endsWith("/") ? value : value + "/";
    }
}
