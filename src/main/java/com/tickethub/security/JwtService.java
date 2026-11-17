package com.tickethub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and validates JWTs using JJWT 0.12.x.
 *
 * <h2>Token structure</h2>
 * <pre>
 * Header  : {"alg":"HS256","typ":"JWT"}
 * Payload : {"sub":"user@example.com","userId":1,"iat":...,"exp":...}
 * Signature: HMAC-SHA256(base64(header)+"."+base64(payload), secret)
 * </pre>
 *
 * <h2>Secret key</h2>
 * <p>The secret is read from {@code jwt.secret} in application.properties.
 * It must be at least 32 characters (256 bits) for HMAC-SHA256.
 * In production, inject it via an environment variable or a secrets manager —
 * never commit the real secret to source control.</p>
 *
 * <h2>Why userId in the payload?</h2>
 * <p>The subject ({@code sub}) is the email, which is the Spring Security username.
 * The userId is added as a custom claim so the controller can call
 * {@code bookingService.book(userId, ...)} without an extra database look-up.</p>
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long      expirationMs;

    public JwtService(
            @Value("${jwt.secret}")          String secret,
            @Value("${jwt.expiration-ms}")   long   expirationMs) {
        // Keys.hmacShaKeyFor requires the byte array to be at least 256 bits for HS256
        this.key          = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // -------------------------------------------------------------------------
    // Token creation
    // -------------------------------------------------------------------------

    /**
     * Builds a signed JWT for the authenticated user.
     *
     * @param userDetails the authenticated principal
     * @return compact JWT string: base64url(header).base64url(payload).signature
     */
    public String generateToken(AppUserDetails userDetails) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userDetails.getUsername())          // "sub" = email
                .claim("userId", userDetails.getId())        // custom claim
                .issuedAt(new Date(now))                     // "iat"
                .expiration(new Date(now + expirationMs))    // "exp"
                .signWith(key)                               // HS256 by default
                .compact();
    }

    // -------------------------------------------------------------------------
    // Token validation and extraction
    // -------------------------------------------------------------------------

    /**
     * Returns the email (subject) if the token is valid; throws {@link JwtException}
     * or {@link IllegalArgumentException} otherwise.
     *
     * <p>JJWT 0.12.x verifies the signature and the expiry in one call.
     * Any tampering, wrong key, or expired token causes an exception —
     * the caller ({@link JwtAuthFilter}) catches these and skips authentication.</p>
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the {@code userId} custom claim.
     * The JSON number is deserialized as an {@link Integer} for small ids,
     * so we cast through {@link Number} to safely get a {@link Long}.
     */
    public Long extractUserId(String token) {
        Object raw = parseClaims(token).get("userId");
        return ((Number) raw).longValue();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

