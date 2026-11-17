package com.tickethub.repository;

import com.tickethub.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    /** Showtimes for one movie, soonest first. movieId matches the movie_id column. */
    List<Show> findByMovieIdOrderByStartTimeAsc(Long movieId);
}
