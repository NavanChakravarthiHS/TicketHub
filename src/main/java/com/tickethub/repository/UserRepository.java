package com.tickethub.repository;

import com.tickethub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data implements this interface at startup. The method name is the query:
 * findByEmail becomes "select * from users where email = ?".
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
