package com.tickethub.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One screening of a movie at a theatre. Theatre is a plain name, not its own table.
 * The table is "shows" so it does not clash with the SQL SHOW statement.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many shows belong to one movie. LAZY means the movie row is loaded only when
     * getMovie() is called. optional = false makes movie_id NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private String theatreName;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Seats exist only for this show. Cascade ALL plus orphanRemoval means creating
     * or deleting the show creates or deletes its seats in the same operation.
     */
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    /** Bookings point at this show. Deleting a show that already has bookings will fail, which is what we want. */
    @OneToMany(mappedBy = "show")
    private List<Booking> bookings = new ArrayList<>();
}
