// ValidationErrorResponse.java
package com.eema.backend.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Error response for validation errors, extends the standard error response with validation errors.
 */
public record ValidationErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
}