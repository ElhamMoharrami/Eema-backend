package com.eema.backend.exception;

/**
 * Exception thrown when access is forbidden.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}