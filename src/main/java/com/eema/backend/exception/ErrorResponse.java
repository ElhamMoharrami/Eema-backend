package com.eema.backend.exception;

import java.time.Instant;

/**
 * Error response structure for API error responses.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}