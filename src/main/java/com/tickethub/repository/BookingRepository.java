package com.tickethub.repository;

import com.tickethub.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** History for one user, newest booking first. */
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Load a booking only when it belongs to this user.
     * An empty result means "not found" for that user, even if the id exists for someone else.
     */
    Optional<Booking> findByIdAndUserId(Long id, Long userId);
}
