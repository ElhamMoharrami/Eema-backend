package com.eema.backend.exception;

/**
 * Exception thrown when authentication fails.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}