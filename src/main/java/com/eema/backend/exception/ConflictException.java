package com.eema.backend.exception;

/**
 * Exception thrown when there is a conflict (e.g., duplicate resource).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}