package com.tickethub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One physical seat for one show. A1 for the 7pm show is a different row from A1 for the 10pm show.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_number"})
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many seats belong to one show. The foreign key is seats.show_id. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    /**
     * Optimistic lock. Hibernate adds "AND version = ?" to every update and then
     * increments the column. If two requests book this seat at the same time, the
     * second update matches zero rows and Spring throws ObjectOptimisticLockingFailureException.
     * The booking service will turn that into HTTP 409.
     */
    @Version
    private Long version;
}
