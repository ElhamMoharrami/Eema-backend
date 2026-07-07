package com.eema.backend.exception;

/**
 * Exception thrown when a resource is not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}