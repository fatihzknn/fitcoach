package com.fitcoach.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FitnessProfileRepository extends JpaRepository<FitnessProfile, UUID> {
    Optional<FitnessProfile> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
