package com.fitcoach.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {
    Optional<WorkoutPlan> findByUserIdAndIsActiveTrue(UUID userId);
    List<WorkoutPlan> findAllByUserId(UUID userId);
}
