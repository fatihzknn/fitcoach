package com.fitcoach.exercise;

import com.fitcoach.exercise.domain.DifficultyLevel;
import com.fitcoach.exercise.domain.MuscleGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    Optional<Exercise> findByName(String name);
    List<Exercise> findByPrimaryMuscleGroup(MuscleGroup muscleGroup);
    List<Exercise> findByDifficultyLevel(DifficultyLevel difficultyLevel);
}
