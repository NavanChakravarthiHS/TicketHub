package com.tickethub.dto;

import com.tickethub.entity.Booking;
import com.tickethub.entity.BookingStatus;
import com.tickethub.entity.Seat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * One booking in the user's history. Call from() inside the service transaction:
 * show, movie, and seats are lazy.
 */
public record BookingResponse(
        Long id,
        Long showId,
        String movieTitle,
        String theatreName,
        LocalDateTime startTime,
        List<String> seatNumbers,
        BigDecimal totalAmount,
        BookingStatus status,
        LocalDateTime createdAt
) {

    public static BookingResponse from(Booking booking) {
        List<String> seatNumbers = booking.getSeats().stream()
                .map(Seat::getSeatNumber)
                .sorted()
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getShow().getId(),
                booking.getShow().getMovie().getTitle(),
                booking.getShow().getTheatreName(),
                booking.getShow().getStartTime(),
                seatNumbers,
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
