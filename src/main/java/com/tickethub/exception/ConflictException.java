package com.tickethub.exception;

/**
 * Thrown when the request clashes with data that already exists.
 * Examples: email already registered, seat already BOOKED. Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
