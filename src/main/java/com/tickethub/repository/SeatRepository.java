package com.tickethub.repository;

import com.tickethub.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    /** Every seat for the seat map of one show. */
    List<Seat> findByShowIdOrderBySeatNumberAsc(Long showId);

    /**
     * Seats the user asked to book, but only if they belong to that show.
     * A shorter result than the requested ids means some ids were missing or were for another show.
     */
    List<Seat> findByShowIdAndIdIn(Long showId, Collection<Long> ids);
}
