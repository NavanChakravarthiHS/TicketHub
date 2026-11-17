package com.tickethub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * One JSON shape for every error. fieldErrors is present only for validation failures,
 * because @JsonInclude skips nulls.
 *
 * Example:
 * { "timestamp": "...", "status": 404, "error": "Not Found", "message": "Movie not found: 5" }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
