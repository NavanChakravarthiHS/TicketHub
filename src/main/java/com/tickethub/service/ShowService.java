package com.tickethub.service;

import com.tickethub.dto.SeatResponse;
import com.tickethub.dto.ShowRequest;
import com.tickethub.dto.ShowResponse;
import com.tickethub.entity.Movie;
import com.tickethub.entity.Seat;
import com.tickethub.entity.SeatStatus;
import com.tickethub.entity.Show;
import com.tickethub.exception.ResourceNotFoundException;
import com.tickethub.repository.MovieRepository;
import com.tickethub.repository.SeatRepository;
import com.tickethub.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates a show and its seats in one transaction. If seat insert fails, the show is rolled back too.
 * Responses are built inside the transaction because movie and seats are lazy.
 */
@Service
public class ShowService {

    /** 5 rows (A-E) of 10 seats. */
    private static final int SEAT_COUNT = 50;
    private static final int SEATS_PER_ROW = 10;

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;

    public ShowService(ShowRepository showRepository,
                       MovieRepository movieRepository,
                       SeatRepository seatRepository) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public ShowResponse create(ShowRequest request) {
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + request.movieId()));

        Show show = new Show();
        show.setMovie(movie);
        show.setTheatreName(request.theatreName());
        show.setStartTime(request.startTime());
        show.setPrice(request.price());

        // Both sides are set. Show.seats owns the cascade, Seat.show owns the foreign key.
        for (int i = 0; i < SEAT_COUNT; i++) {
            show.getSeats().add(newSeat(show, i));
        }

        return ShowResponse.from(showRepository.save(show));
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> findByMovie(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie not found: " + movieId);
        }
        return showRepository.findByMovieIdOrderByStartTimeAsc(movieId).stream()
                .map(ShowResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> findSeats(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ResourceNotFoundException("Show not found: " + showId);
        }
        return seatRepository.findByShowIdOrderBySeatNumberAsc(showId).stream()
                .map(SeatResponse::from)
                .toList();
    }

    /** Index 0 is A1, index 10 is B1, index 49 is E10. */
    private Seat newSeat(Show show, int index) {
        char row = (char) ('A' + (index / SEATS_PER_ROW));
        int number = (index % SEATS_PER_ROW) + 1;

        Seat seat = new Seat();
        seat.setShow(show);
        seat.setSeatNumber(row + String.valueOf(number));
        seat.setStatus(SeatStatus.AVAILABLE);
        return seat;
    }
}
