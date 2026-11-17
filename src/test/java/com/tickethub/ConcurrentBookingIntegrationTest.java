package com.tickethub;

import com.tickethub.dto.BookingRequest;
import com.tickethub.dto.BookingResponse;
import com.tickethub.dto.MovieRequest;
import com.tickethub.dto.MovieResponse;
import com.tickethub.dto.ShowRequest;
import com.tickethub.dto.ShowResponse;
import com.tickethub.entity.Role;
import com.tickethub.entity.SeatStatus;
import com.tickethub.entity.User;
import com.tickethub.repository.SeatRepository;
import com.tickethub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: 10 threads race to book the same single seat.
 *
 * <h2>Why RANDOM_PORT?</h2>
 * <p>{@code webEnvironment = RANDOM_PORT} starts the full embedded Tomcat on a free port.
 * Every HTTP call made by {@link TestRestTemplate} travels through the full Spring stack:
 * DispatcherServlet → Controller → Service → JPA/Hibernate → H2.
 * Each request is handled on its own Tomcat worker thread, inside its own
 * {@code @Transactional} boundary. This is the closest you can get to a real production
 * race without an external database.</p>
 *
 * <h2>Why not call BookingService directly?</h2>
 * <p>If you inject and call {@code bookingService.book()} from the test thread, the Spring
 * proxy wraps the call in a transaction that is NEVER committed until the method returns.
 * With 10 threads all inside the test's own JVM, Hibernate's session cache would see
 * dirty reads from the same connection pool and the race would not be real. HTTP calls
 * force each request into its own independent transaction with a commit at the end of
 * each request — that is the scenario the optimistic lock is designed for.</p>
 *
 * <h2>Why @DirtiesContext?</h2>
 * <p>The H2 in-memory database is shared across the Spring context. DirtiesContext tells
 * Spring to tear down and recreate the context (and with it, the database) after this test
 * class, so a failed mid-test state cannot leak into other test classes.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcurrentBookingIntegrationTest {

    private static final int THREAD_COUNT = 10;

    @LocalServerPort
    int port;

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository   userRepository;
    @Autowired SeatRepository   seatRepository;

    /** IDs created during @BeforeEach — shared by all threads in the test. */
    private Long userId;
    private Long seatId;
    private Long showId;

    // -------------------------------------------------------------------------
    // Setup: one user, one movie, one show, one seat to fight over
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        // 1. Persist a user directly — no auth endpoint yet
        User user = new User();
        user.setName("Race Tester");
        user.setEmail("race@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("secret"));
        user.setRole(Role.USER);
        userId = userRepository.save(user).getId();

        // 2. Create a movie via the API
        MovieRequest movieReq = new MovieRequest(
                "Race Movie", "A film about concurrency", 90, "English");
        ResponseEntity<MovieResponse> movieResp =
                restTemplate.postForEntity(url("/api/movies"), movieReq, MovieResponse.class);
        assertThat(movieResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long movieId = movieResp.getBody().id();

        // 3. Create a show (and its 50 seats) via the API
        ShowRequest showReq = new ShowRequest(
                movieId, "Test Theatre",
                LocalDateTime.now().plusDays(1),
                BigDecimal.valueOf(100));
        ResponseEntity<ShowResponse> showResp =
                restTemplate.postForEntity(url("/api/shows"), showReq, ShowResponse.class);
        assertThat(showResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        showId = showResp.getBody().id();

        // 4. Pick the very first seat (A1) — all 10 threads will target this one seat
        seatId = seatRepository.findByShowIdOrderBySeatNumberAsc(showId)
                .get(0).getId();
    }

    // -------------------------------------------------------------------------
    // The concurrency test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Exactly one of 10 concurrent booking requests succeeds; the rest get 409")
    void exactlyOneBookingSucceedsUnderConcurrentLoad() throws InterruptedException {

        /*
         * HOW THE RACE IS STAGED
         * ──────────────────────
         * A CountDownLatch with count=THREAD_COUNT acts as a starting gate.
         * Each thread decrements the latch (countDown()) and then immediately waits
         * (await()) until the latch reaches zero. The latch hits zero only when all
         * THREAD_COUNT threads have called countDown() — meaning every thread is
         * sitting at the starting line before any of them are allowed to proceed.
         * This maximises the chance that every request arrives at the server at
         * nearly the same instant, making the race condition observable in a test.
         */
        CountDownLatch startGate = new CountDownLatch(THREAD_COUNT);

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount  = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        @SuppressWarnings("unchecked")
        Future<ResponseEntity<String>>[] futures = new Future[THREAD_COUNT];

        // Submit all 10 tasks before any starts
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures[i] = pool.submit(() -> {
                // Each thread signals it is ready, then waits for all others
                startGate.countDown();
                startGate.await();   // blocks until count reaches 0

                BookingRequest req = new BookingRequest(showId, List.of(seatId));
                return restTemplate.postForEntity(
                        url("/api/users/" + userId + "/bookings"),
                        req,
                        String.class);    // String so we can read the error body too
            });
        }

        // Collect results
        for (Future<ResponseEntity<String>> f : futures) {
            ResponseEntity<String> resp;
            try {
                resp = f.get();
            } catch (Exception e) {
                throw new AssertionError("Thread threw unexpected exception", e);
            }

            if (resp.getStatusCode() == HttpStatus.CREATED) {
                successCount.incrementAndGet();
            } else if (resp.getStatusCode() == HttpStatus.CONFLICT) {
                conflictCount.incrementAndGet();
            } else {
                // Any other status (400, 404, 500) is an unexpected failure
                throw new AssertionError(
                        "Unexpected status " + resp.getStatusCode() +
                        " body: " + resp.getBody());
            }
        }

        pool.shutdown();

        // ---- assertions -------------------------------------------------------

        /*
         * EXACTLY ONE SUCCESS
         * The @Version column guarantees that only one transaction can commit a
         * status change on the same seat row. Every other transaction either:
         *   (a) sees status=BOOKED after the winner committed  → ConflictException → 409
         *   (b) passes the status check but loses the version race              → 409
         *       (ObjectOptimisticLockingFailureException mapped by GlobalExceptionHandler)
         */
        assertThat(successCount.get())
                .as("exactly one booking should succeed")
                .isEqualTo(1);

        assertThat(conflictCount.get())
                .as("the remaining " + (THREAD_COUNT - 1) + " requests should get 409")
                .isEqualTo(THREAD_COUNT - 1);

        assertThat(successCount.get() + conflictCount.get())
                .as("total responses must equal total requests")
                .isEqualTo(THREAD_COUNT);

        // ---- database state ---------------------------------------------------

        /*
         * Final state: the seat in the database must be BOOKED.
         * If the seat were still AVAILABLE it would mean the winning transaction
         * was rolled back, which contradicts the 201 we observed.
         */
        var finalSeat = seatRepository.findById(seatId).orElseThrow();
        assertThat(finalSeat.getStatus())
                .as("seat must be BOOKED after the race")
                .isEqualTo(SeatStatus.BOOKED);

        /*
         * Final state: the version column must be exactly 1.
         * It starts at 0 (never updated). One winning UPDATE incremented it to 1.
         * No other transaction committed, so it cannot be higher than 1.
         */
        assertThat(finalSeat.getVersion())
                .as("version must be exactly 1 — only one UPDATE committed")
                .isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
