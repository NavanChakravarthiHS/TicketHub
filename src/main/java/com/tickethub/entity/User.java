package com.tickethub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A person who can log in. The table is named "users" because USER is reserved in MySQL.
 * Getters and setters come from Lombok. @Data is avoided: it generates equals/hashCode
 * from every field, which breaks once Hibernate replaces a lazy collection with a proxy.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash. The plain password is never stored. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * One user, many bookings. mappedBy = "user" means Booking.user owns the foreign key.
     * This side is only for navigation; it does not create a column on users.
     */
    @OneToMany(mappedBy = "user")
    private List<Booking> bookings = new ArrayList<>();
}
