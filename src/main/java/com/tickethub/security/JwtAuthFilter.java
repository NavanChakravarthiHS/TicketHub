package com.tickethub.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the {@code Authorization: Bearer <token>} header on every request,
 * validates the JWT, and populates the Spring Security context.
 *
 * <h2>Filter chain position</h2>
 * <p>This filter is registered {@code before} Spring Security's
 * {@code UsernamePasswordAuthenticationFilter} (see {@link com.tickethub.config.SecurityConfig}).
 * When it runs, the Security context is always empty — the filter itself is the only
 * thing that can populate it for JWT-based requests.</p>
 *
 * <h2>What OncePerRequestFilter guarantees</h2>
 * <p>Servlet containers may forward a request internally (e.g. for error pages).
 * {@link OncePerRequestFilter} ensures this code executes at most once per original
 * external request, preventing double-authentication.</p>
 *
 * <h2>Step-by-step</h2>
 * <ol>
 *   <li>Extract the token from the {@code Authorization} header.</li>
 *   <li>Ask {@link JwtService} to parse and verify the signature + expiry.</li>
 *   <li>Load the full {@link AppUserDetails} from the database (confirms the user still exists).</li>
 *   <li>Build a {@link UsernamePasswordAuthenticationToken} and store it in the context.</li>
 *   <li>Continue the filter chain — Spring Security's authorization checks run after.</li>
 * </ol>
 *
 * <p>If the header is absent, malformed, or the token is expired/invalid,
 * the filter passes the request on without touching the security context.
 * Spring Security then rejects the request with 401 (or allows it if the
 * route is public).</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService            jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService         = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain) throws ServletException, IOException {

        // ---- 1. Extract the Bearer token ------------------------------------
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);  // no token → pass through
            return;
        }
        String token = authHeader.substring(7);  // strip "Bearer "

        // ---- 2. Validate the token (signature + expiry) ---------------------
        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Invalid or expired token: pass through without authenticating.
            // Spring Security will reject the request if the route requires auth.
            filterChain.doFilter(request, response);
            return;
        }

        // ---- 3. Only authenticate if the context is empty -------------------
        // If the context is already populated (e.g. by another filter), skip.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ---- 4. Load the user and build the authentication token ------------
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,                         // credentials — not needed after JWT validation
                        userDetails.getAuthorities()  // ROLE_USER or ROLE_ADMIN
                );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // ---- 5. Store in the security context for the rest of the chain -----
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}

