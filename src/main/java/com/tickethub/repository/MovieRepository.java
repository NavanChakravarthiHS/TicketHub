package com.tickethub.repository;

import com.tickethub.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

/** CRUD for movies comes from JpaRepository: save, findById, findAll, deleteById. */
public interface MovieRepository extends JpaRepository<Movie, Long> {
}
