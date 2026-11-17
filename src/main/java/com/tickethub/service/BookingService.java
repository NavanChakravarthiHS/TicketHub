package com.tickethub.service;

import com.tickethub.dto.BookingRequest;
import com.tickethub.dto.BookingResponse;
import com.tickethub.entity.Booking;
import com.tickethub.entity.BookingStatus;
import com.tickethub.entity.Seat;
import com.tickethub.entity.SeatStatus;
import com.tickethub.entity.Show;
import com.tickethub.entity.User;
import com.tickethub.exception.BadRequestException;
import com.tickethub.exception.ConflictException;
import com.tickethub.exception.ResourceNotFoundException;
import com.tickethub.repository.BookingRepository;
import com.tickethub.repository.SeatRepository;
import com.tickethub.repository.ShowRepository;
import com.tickethub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Core booking logic.
 *
 * <h2>How double-booking is prevented</h2>
 * <p>Every {@link Seat} row carries a {@code @Version} column managed by Hibernate.
 * When we set a seat's status to BOOKED and call {@code save()}, Hibernate issues:</p>
 * <pre>UPDATE seats SET status='BOOKED', version=N+1 WHERE id=? AND version=N</pre>
 * <p>If two concurrent transactions read the same seat at version 0 and both try to update it,
 * only one UPDATE will match the WHERE clause. The second transaction's UPDATE affects 0 rows,
 * so Hibernate throws {@link org.springframework.orm.ObjectOptimisticLockingFailureException}.
 * The global exception handler turns that into HTTP 409 with a human-readable message.</p>
 *
 * <h2>Application-level guard</h2>
 * <p>Before the version check even has a chance to fire, we check
 * {@code seat.getStatus() == AVAILABLE}. This handles the common sequential case
 * (request B runs after request A has already committed) with a clear 409 message
 * rather than relying solely on the database error path.</p>
 *
 * <h2>All-or-nothing</h2>
 * <p>The whole method runs in a single transaction. If the seat status check fails,
 * or if the optimistic lock fires for any seat in the list, every UPDATE is rolled back
 * and no seats in the request end up partially booked.</p>
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository    seatRepository;
    private final ShowRepository    showRepository;
    private final UserRepository    userRepository;

    public BookingService(BookingRepository bookingRepository,
                          SeatRepository seatRepository,
                          ShowRepository showRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.seatRepository    = seatRepository;
        this.showRepository    = showRepository;
        this.userRepository    = userRepository;
    }

    // -------------------------------------------------------------------------
    // Book seats
    // -------------------------------------------------------------------------

    /**
     * Books one or more seats for a show in a single transaction.
     *
     * <p>Steps (all inside the same transaction):
     * <ol>
     *   <li>Load user and show (404 if either is missing).</li>
     *   <li>Load only the requested seat rows that belong to this show
     *       (a shorter result means unknown or cross-show ids → 400).</li>
     *   <li>Check every seat is AVAILABLE (sequential 409 guard).</li>
     *   <li>Mark every seat BOOKED — Hibernate will append version+1 to each UPDATE.</li>
     *   <li>Persist the Booking row and build the response while entities are still in-session.</li>
     * </ol>
     *
     * @param userId  the authenticated user's id (will be replaced by the JWT principal in Step 6)
     * @param request contains the showId and the list of seatIds to reserve
     * @return the confirmed booking
     */
    @Transactional
    public BookingResponse book(Long userId, BookingRequest request) {
        User user = getUser(userId);
        Show show = getShow(request.showId());

        // ---- guard: all requested seat ids must belong to this show ----------
        List<Seat> seats = seatRepository.findByShowIdAndIdIn(request.showId(), request.seatIds());
        if (seats.size() != request.seatIds().size()) {
            throw new BadRequestException(
                    "One or more seat ids are invalid or do not belong to show " + request.showId());
        }

        // ---- guard: application-level availability check (sequential case) ---
        List<String> takenSeats = seats.stream()
                .filter(s -> s.getStatus() != SeatStatus.AVAILABLE)
                .map(Seat::getSeatNumber)
                .sorted()
                .toList();
        if (!takenSeats.isEmpty()) {
            throw new ConflictException("Seats already booked: " + takenSeats);
        }

        // ---- mark BOOKED — optimistic lock fires here for the concurrent case --
        // Hibernate issues: UPDATE seats SET status='BOOKED', version=N+1
        //                   WHERE id=? AND version=N
        // If another transaction already incremented version, this UPDATE hits 0 rows
        // and Hibernate throws ObjectOptimisticLockingFailureException → HTTP 409.
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
            seatRepository.save(seat);
        }

        // ---- build and persist booking row -----------------------------------
        BigDecimal total = show.getPrice()
                .multiply(BigDecimal.valueOf(seats.size()));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.getSeats().addAll(seats);
        booking.setTotalAmount(total);
        booking.setStatus(BookingStatus.CONFIRMED);

        return BookingResponse.from(bookingRepository.save(booking));
    }

    // -------------------------------------------------------------------------
    // Cancel booking
    // -------------------------------------------------------------------------

    /**
     * Cancels a confirmed booking and frees the seats.
     *
     * <p>Cancellation is also transactional: if the seat status update fails (e.g. the
     * seat was somehow already AVAILABLE from a data fix), the booking status stays
     * CONFIRMED and no partial state is written.</p>
     *
     * @param userId    the authenticated user's id
     * @param bookingId the booking to cancel (must belong to this user)
     */
    @Transactional
    public BookingResponse cancel(Long userId, Long bookingId) {
        // findByIdAndUserId returns empty if the booking belongs to someone else,
        // giving a 404 instead of leaking that the booking exists.
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking " + bookingId + " is already cancelled");
        }

        // Free every seat so it can be booked again
        for (Seat seat : booking.getSeats()) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    // -------------------------------------------------------------------------
    // Booking history
    // -------------------------------------------------------------------------

    /**
     * Returns every booking ever made by this user, newest first.
     * CANCELLED bookings are included so the full history is visible.
     *
     * @param userId the authenticated user's id
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> history(Long userId) {
        // 404 if the user does not exist so callers get a clear error
        getUser(userId);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BookingResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Show getShow(Long id) {
        return showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + id));
    }
}

