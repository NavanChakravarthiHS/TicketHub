package com.tickethub.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Body of create booking. One request books every listed seat for a single show. */
public record BookingRequest(
        @NotNull(message = "Show id is required")
        Long showId,

        @NotEmpty(message = "At least one seat id is required")
        List<@NotNull(message = "Seat id cannot be null") Long> seatIds
) {
}
