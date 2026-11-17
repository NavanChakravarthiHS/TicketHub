package com.tickethub.config;

import com.tickethub.security.AppUserDetailsService;
import com.tickethub.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration: JWT stateless filter chain + role-based rules.
 *
 * <h2>Filter chain (in execution order)</h2>
 * <ol>
 *   <li><b>SecurityContextPersistenceFilter</b> — clears the context between requests
 *       (STATELESS policy ensures no HttpSession is created).</li>
 *   <li><b>JwtAuthFilter</b> (our custom filter, registered here before step 3) —
 *       reads {@code Authorization: Bearer <token>}, validates the JWT, and writes
 *       the authenticated principal into the SecurityContext.</li>
 *   <li><b>UsernamePasswordAuthenticationFilter</b> (Spring's built-in) — not used
 *       for JWT flows; it handles form-login which we have disabled.</li>
 *   <li><b>ExceptionTranslationFilter</b> — catches {@code AccessDeniedException} and
 *       {@code AuthenticationException}. With our custom entry point, it writes the
 *       JSON error response instead of redirecting to a login page.</li>
 *   <li><b>AuthorizationFilter</b> — enforces the rules declared in
 *       {@code authorizeHttpRequests} below.</li>
 * </ol>
 *
 * <h2>Route rules (first match wins)</h2>
 * <ul>
 *   <li>{@code /auth/**}                         — public (register, login)</li>
 *   <li>{@code GET /api/movies/**}               — public (browse catalogue)</li>
 *   <li>{@code GET /api/movies/{id}/shows}       — public</li>
 *   <li>{@code GET /api/shows/{id}/seats}        — public</li>
 *   <li>{@code POST/PUT/DELETE /api/movies/**}   — ADMIN only</li>
 *   <li>{@code POST /api/shows/**}               — ADMIN only</li>
 *   <li>{@code /api/users/{userId}/bookings/**}  — USER or ADMIN (ownership enforced in service)</li>
 *   <li>Everything else                          — authenticated</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter         jwtAuthFilter;
    private final AppUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          AppUserDetailsService userDetailsService) {
        this.jwtAuthFilter      = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    // -------------------------------------------------------------------------
    // Filter chain
    // -------------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // REST APIs don't use browser sessions or CSRF cookies
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ---- route-level rules ------------------------------------------
            .authorizeHttpRequests(auth -> auth

                // Public: register and login
                .requestMatchers("/auth/**").permitAll()

                // Public: OpenAPI / Swagger docs (useful during development)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Public: browsing (read-only catalogue)
                .requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/movies/*/shows").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/shows/*/seats").permitAll()

                // Admin-only: movie catalogue management
                .requestMatchers(HttpMethod.POST,   "/api/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/movies/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/movies/**").hasRole("ADMIN")

                // Admin-only: show creation
                .requestMatchers(HttpMethod.POST, "/api/shows/**").hasRole("ADMIN")

                // Authenticated users: booking (ownership is verified in BookingService)
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")

                // All other endpoints require any valid authentication
                .anyRequest().authenticated()
            )

            // ---- JWT filter: runs before Spring's own auth filter -----------
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ---- return 401 JSON instead of redirecting to a login page -----
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(401);
                    response.getWriter().write(
                        "{\"status\":401,\"error\":\"Unauthorized\"," +
                        "\"message\":\"Authentication required. " +
                        "Provide a valid Bearer token.\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(403);
                    response.getWriter().write(
                        "{\"status\":403,\"error\":\"Forbidden\"," +
                        "\"message\":\"You do not have permission to perform this action.\"}");
                })
            );

        return http.build();
    }

    // -------------------------------------------------------------------------
    // Authentication infrastructure
    // -------------------------------------------------------------------------

    /**
     * BCrypt with the default strength (10 rounds).
     * This bean is injected into AuthService for hashing and into
     * DaoAuthenticationProvider for verification.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Wires our UserDetailsService and PasswordEncoder into Spring's standard
     * DAO-based authentication provider. The AuthenticationManager (below) uses
     * this provider when {@code authManager.authenticate()} is called in AuthService.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the AuthenticationManager as a Spring bean so it can be injected
     * into AuthService. Without this @Bean, Spring Boot does not expose it by default.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
