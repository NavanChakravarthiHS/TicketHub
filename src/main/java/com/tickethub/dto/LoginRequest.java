package com.tickethub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body of POST /auth/login. The password rules are not repeated: a wrong password is an auth failure, not a validation error. */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
