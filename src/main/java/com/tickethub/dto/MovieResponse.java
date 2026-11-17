package com.tickethub.dto;

import com.tickethub.entity.Movie;

/** Movie fields the API is allowed to show. The shows collection stays on the entity. */
public record MovieResponse(
        Long id,
        String title,
        String description,
        Integer durationMinutes,
        String language
) {

    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getDurationMinutes(),
                movie.getLanguage()
        );
    }
}
