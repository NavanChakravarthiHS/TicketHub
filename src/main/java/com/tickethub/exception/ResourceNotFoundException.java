package com.tickethub.exception;

/** Thrown when an id does not match a row the caller is allowed to see. Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
