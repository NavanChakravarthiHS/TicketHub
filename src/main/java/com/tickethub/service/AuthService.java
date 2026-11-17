package com.tickethub.service;

import com.tickethub.dto.AuthResponse;
import com.tickethub.dto.LoginRequest;
import com.tickethub.dto.RegisterRequest;
import com.tickethub.entity.Role;
import com.tickethub.entity.User;
import com.tickethub.exception.ConflictException;
import com.tickethub.repository.UserRepository;
import com.tickethub.security.AppUserDetails;
import com.tickethub.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Register and login use-cases.
 *
 * <h2>Register</h2>
 * <p>Checks for a duplicate email, hashes the password with BCrypt, persists the user,
 * and immediately issues a JWT so the caller can start making authenticated requests
 * without a separate login step.</p>
 *
 * <h2>Login</h2>
 * <p>Delegates password verification to Spring Security's {@link AuthenticationManager}.
 * The manager calls {@link com.tickethub.security.AppUserDetailsService#loadUserByUsername}
 * to fetch the stored BCrypt hash and then compares it with the submitted password.
 * If they match, the manager returns an authenticated token; if not, it throws
 * {@link org.springframework.security.core.AuthenticationException} which the
 * global exception handler turns into HTTP 401.</p>
 *
 * <h2>Why not hash the password manually in login?</h2>
 * <p>Letting the AuthenticationManager do it keeps the BCrypt comparison in one place
 * (the DaoAuthenticationProvider) and ensures constant-time comparison to prevent
 * timing attacks.</p>
 */
@Service
public class AuthService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtService          jwtService;
    private final AuthenticationManager authManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authManager) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
        this.authManager     = authManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Duplicate-email check: the unique constraint on users.email is the
        // authoritative guard, but we check here first to return a 409 with a
        // human-readable message rather than leaking the SQL constraint name.
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));  // BCrypt hash
        user.setRole(Role.USER);                                        // all self-registrations are USER

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(new AppUserDetails(saved));
        return AuthResponse.of(token, saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // AuthenticationManager.authenticate() internally:
        //   1. Calls AppUserDetailsService.loadUserByUsername(email)
        //   2. Runs BCryptPasswordEncoder.matches(rawPassword, storedHash)
        //   3. Returns an authenticated token, or throws BadCredentialsException
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        AppUserDetails userDetails = (AppUserDetails) auth.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        // Re-load user entity to build the full AuthResponse (name, role, etc.)
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return AuthResponse.of(token, user);
    }
}

