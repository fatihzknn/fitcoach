package com.fitcoach.exercise.dto;

import com.fitcoach.exercise.Exercise;
import com.fitcoach.exercise.domain.DifficultyLevel;
import com.fitcoach.exercise.domain.MovementPattern;
import com.fitcoach.exercise.domain.MuscleGroup;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ExerciseDto(
        UUID id,
        String name,
        MuscleGroup primaryMuscleGroup,
        Set<MuscleGroup> secondaryMuscleGroups,
        MovementPattern movementPattern,
        DifficultyLevel difficultyLevel,
        String videoUrl,
        String formCue,
        String commonMistake,
        List<ExerciseDto> alternatives
) {
    public static ExerciseDto from(Exercise exercise) {
        return new ExerciseDto(
                exercise.getId(),
                exercise.getName(),
                exercise.getPrimaryMuscleGroup(),
                exercise.getSecondaryMuscleGroups(),
                exercise.getMovementPattern(),
                exercise.getDifficultyLevel(),
                exercise.getVideoUrl(),
                exercise.getFormCue(),
                exercise.getCommonMistake(),
                exercise.getAlternatives().stream()
                        .map(ExerciseDto::fromSimple)
                        .toList()
        );
    }

    /** Flat reference used when listing alternatives — prevents recursive nesting. */
    public static ExerciseDto fromSimple(Exercise exercise) {
        return new ExerciseDto(
                exercise.getId(),
                exercise.getName(),
                exercise.getPrimaryMuscleGroup(),
                exercise.getSecondaryMuscleGroups(),
                exercise.getMovementPattern(),
                exercise.getDifficultyLevel(),
                exercise.getVideoUrl(),
                exercise.getFormCue(),
                exercise.getCommonMistake(),
                List.of()
        );
    }
}
