package com.tickethub.dto;

import com.tickethub.entity.Seat;
import com.tickethub.entity.SeatStatus;

/** One seat on the availability map. The version column is an internal lock and is not sent. */
public record SeatResponse(
        Long id,
        String seatNumber,
        SeatStatus status
) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getStatus());
    }
}
