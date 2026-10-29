package com.eema.backend.exception;

/** A conflict that intentionally commits the REQUESTED -> EXPIRED transition. */
public class RequestDeadlineExpiredException extends ConflictException {
    public RequestDeadlineExpiredException(String message) {
        super(message);
    }
}
