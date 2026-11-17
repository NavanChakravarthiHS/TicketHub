package com.tickethub.exception;

/**
 * Thrown when the JSON is well formed but the business rule fails.
 * Example: a seat id belongs to a different show. Mapped to HTTP 400.
 * Bean Validation failures do not use this class; Spring raises those itself.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
