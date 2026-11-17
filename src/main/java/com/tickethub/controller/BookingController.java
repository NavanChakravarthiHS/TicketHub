package com.tickethub.controller;

import com.tickethub.dto.BookingRequest;
import com.tickethub.dto.BookingResponse;
import com.tickethub.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * <p>All routes include {@code /users/{userId}} so the caller's identity is explicit
 * in this step. When JWT is added in Step 6, userId will be extracted from the
 * security principal instead, and these path variables will be removed.</p>
 *
 * <ul>
 *   <li>POST   /api/users/{userId}/bookings          — book seats</li>
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

    /**
     * Book one or more seats for a show.
     *
     * <p>Possible responses:
     * <ul>
     *   <li>201 Created   — booking confirmed, body is {@link BookingResponse}</li>
     *   <li>400 Bad Request — invalid JSON, failed @Valid, or seat ids not for this show</li>
     *   <li>404 Not Found  — user or show does not exist</li>
     *   <li>409 Conflict   — one or more seats already BOOKED (either sequential or concurrent)</li>
     * </ul>
     */
    @PostMapping
    public ResponseEntity<BookingResponse> book(@PathVariable Long userId,
                                                @Valid @RequestBody BookingRequest request) {
        BookingResponse body = bookingService.book(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * Cancel a confirmed booking. The booking row is kept (status → CANCELLED)
     * and the seats are freed so another user can book them.
     *
     * <p>Possible responses:
     * <ul>
     *   <li>200 OK        — cancellation confirmed, body is the updated {@link BookingResponse}</li>
     *   <li>404 Not Found — booking does not exist or belongs to a different user</li>
     *   <li>409 Conflict  — booking is already cancelled</li>
     * </ul>
     */
    @DeleteMapping("/{id}")
    public BookingResponse cancel(@PathVariable Long userId,
                                  @PathVariable Long id) {
        return bookingService.cancel(userId, id);
    }

    /**
     * Full booking history for a user, newest first.
     * Cancelled bookings are included.
     */
    @GetMapping
    public List<BookingResponse> history(@PathVariable Long userId) {
        return bookingService.history(userId);
    }
}

