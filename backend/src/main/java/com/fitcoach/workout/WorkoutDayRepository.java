package com.fitcoach.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkoutDayRepository extends JpaRepository<WorkoutDay, UUID> {}
