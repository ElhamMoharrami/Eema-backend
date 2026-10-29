package com.eema.backend.exception;

public class CancellationTermsChangedException extends ConflictException {
    public CancellationTermsChangedException() {
        super("Cancellation terms must be reviewed again before confirming");
    }
}
