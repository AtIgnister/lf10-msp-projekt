package org.lf10.stimmungsumfrage.Repositories;

import org.jspecify.annotations.NullMarked;
import org.lf10.stimmungsumfrage.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email (used for login)
    Optional<User> findByEmail(String email);

    @NullMarked
    Optional<User> findById(Long id);

    // Check if email already exists
    boolean existsByEmail(String email);
}

