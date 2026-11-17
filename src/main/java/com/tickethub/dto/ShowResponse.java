package com.tickethub.dto;

import com.tickethub.entity.Show;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One showtime. movieTitle is copied here so the client does not need a second request.
 * Call from() while the database session is still open: movie is lazy, and
 * spring.jpa.open-in-view is false.
 */
public record ShowResponse(
        Long id,
        Long movieId,
        String movieTitle,
        String theatreName,
        LocalDateTime startTime,
        BigDecimal price
) {

    public static ShowResponse from(Show show) {
        return new ShowResponse(
                show.getId(),
                show.getMovie().getId(),
                show.getMovie().getTitle(),
                show.getTheatreName(),
                show.getStartTime(),
                show.getPrice()
        );
    }
}
