package com.tickethub.controller;

import com.tickethub.dto.BookingRequest;
import com.tickethub.dto.BookingResponse;
import com.tickethub.exception.ResourceNotFoundException;
import com.tickethub.security.AppUserDetails;
import com.tickethub.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP layer for bookings.
 *
 * <h2>Ownership enforcement</h2>
 * <p>The route is {@code /api/users/{userId}/bookings/**}.
 * The {@code {userId}} in the path is the <em>target</em> user (whose bookings to act on).
 * The JWT principal is the <em>caller</em>.
 * Before delegating to the service we assert that the caller <em>is</em> the target user,
 * or is an ADMIN. This prevents user A from reading or cancelling user B's bookings
 * even with a valid token.</p>
 *
 * <ul>
 *   <li>POST   /api/users/{userId}/bookings          — book seats (USER or ADMIN)</li>
 *   <li>DELETE /api/users/{userId}/bookings/{id}     — cancel a booking</li>
 *   <li>GET    /api/users/{userId}/bookings           — booking history</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users/{userId}/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> book(
            @PathVariable Long userId,
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody BookingRequest request) {

        assertOwnerOrAdmin(principal, userId);
        BookingResponse body = bookingService.book(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @DeleteMapping("/{id}")
    public BookingResponse cancel(
            @PathVariable Long userId,
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails principal) {

        assertOwnerOrAdmin(principal, userId);
        return bookingService.cancel(userId, id);
    }

    @GetMapping
    public List<BookingResponse> history(
            @PathVariable Long userId,
            @AuthenticationPrincipal AppUserDetails principal) {

        assertOwnerOrAdmin(principal, userId);
        return bookingService.history(userId);
    }

    // -------------------------------------------------------------------------
    // Ownership guard
    // -------------------------------------------------------------------------

    /**
     * Throws a 404 (not 403) if the caller is neither the target user nor an ADMIN.
     *
     * <p>Returning 404 instead of 403 is deliberate: a 403 would reveal that the
     * resource exists and belongs to someone else. A 404 leaks nothing.</p>
     */
    private void assertOwnerOrAdmin(AppUserDetails principal, Long targetUserId) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !principal.getId().equals(targetUserId)) {
            throw new ResourceNotFoundException("Booking not found");
        }
    }
}
