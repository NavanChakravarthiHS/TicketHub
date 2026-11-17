package com.tickethub.controller;

import com.tickethub.dto.AuthResponse;
import com.tickethub.dto.LoginRequest;
import com.tickethub.dto.RegisterRequest;
import com.tickethub.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints: register and login.
 * Both are permitted without a token (see SecurityConfig).
 *
 * <ul>
 *   <li>POST /auth/register — creates a USER account and returns a JWT</li>
 *   <li>POST /auth/login    — verifies credentials and returns a JWT</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new account.
     * Returns 201 with a JWT on success.
     * Returns 400 if validation fails, 409 if email is already taken.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Login with email + password.
     * Returns 200 with a JWT on success.
     * Returns 401 if the credentials are wrong.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

