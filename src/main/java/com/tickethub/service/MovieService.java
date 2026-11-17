package com.tickethub.service;

import com.tickethub.dto.MovieRequest;
import com.tickethub.dto.MovieResponse;
import com.tickethub.entity.Movie;
import com.tickethub.exception.ResourceNotFoundException;
import com.tickethub.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Movie use cases. Controllers call this class and never touch the repository.
 * Methods return MovieResponse so the entity stays inside this layer.
 */
@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie movie = new Movie();
        copy(request, movie);
        return MovieResponse.from(movieRepository.save(movie));
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> findAll() {
        return movieRepository.findAll().stream()
                .map(MovieResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovieResponse findById(Long id) {
        return MovieResponse.from(getMovie(id));
    }

    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = getMovie(id);
        copy(request, movie);
        // save is explicit so the write is visible here. Dirty checking would also update the row.
        return MovieResponse.from(movieRepository.save(movie));
    }

    /**
     * Deletes the movie and, through the entity cascade, its shows and seats.
     * A movie that already has bookings cannot be deleted: the database rejects it,
     * and the global handler turns that into HTTP 409.
     */
    @Transactional
    public void delete(Long id) {
        Movie movie = getMovie(id);
        movieRepository.delete(movie);
    }

    private Movie getMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + id));
    }

    private void copy(MovieRequest request, Movie movie) {
        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setLanguage(request.language());
    }
}
