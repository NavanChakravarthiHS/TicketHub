package com.tickethub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /auth/register. A record is immutable, so the controller cannot
 * change the request after validation. Annotations are checked when the controller
 * parameter is marked @Valid.
 */
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        /*
         * BCrypt only uses the first 72 bytes of a password. The max size keeps
         * a longer password from being silently truncated.
         */
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {
}
