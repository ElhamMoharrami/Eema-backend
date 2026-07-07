package com.eema.backend.repository;

import com.eema.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email (case-insensitive).
     *
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if a user exists with the given email (case-insensitive).
     *
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmailIgnoreCase(String email);
}