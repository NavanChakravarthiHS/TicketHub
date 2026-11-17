package com.tickethub.dto;

import com.tickethub.entity.Role;
import com.tickethub.entity.User;

/**
 * Returned by register and login. The password hash is not included.
 * tokenType is always "Bearer" so the client sends Authorization: Bearer followed by the token.
 */
public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String name,
        String email,
        Role role
) {

    public static AuthResponse of(String token, User user) {
        return new AuthResponse(token, "Bearer", user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
