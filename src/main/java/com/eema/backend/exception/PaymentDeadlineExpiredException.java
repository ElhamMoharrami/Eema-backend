package com.eema.backend.exception;

/** The durable expiry/received provider association must commit before returning 409. */
public class PaymentDeadlineExpiredException extends ConflictException {
    public PaymentDeadlineExpiredException(String message) { super(message); }
}
