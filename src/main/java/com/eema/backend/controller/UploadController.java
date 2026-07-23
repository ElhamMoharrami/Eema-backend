package com.eema.backend.controller;

import com.eema.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    /**
     * Upload a venue photo.
     *
     * @param file the photo file (must be non-empty)
     * @return JSON with upload status and file URL
     */
    @PostMapping("/venue-photo")
    public ResponseEntity<Map<String, String>> uploadVenuePhoto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "File cannot be empty"));
        }

        try {
            String fileUrl = storageService.store(file);
            Map<String, String> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("url", fileUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Serve uploaded files (optional - can be replaced by Spring static resource configuration).
     * Allows direct access to uploaded files via GET /api/files/{filename}
     */
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);

        // Determine content type based on file extension
        String contentType = determineContentType(file.getFilename());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    private String determineContentType(String filename) {
        if (filename == null) return null;
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".webp")) return "image/webp";
        return null; // Let Spring determine default
    }
}