package com.fitcoach.workout.dto;

import com.fitcoach.workout.WorkoutDay;

import java.util.List;
import java.util.UUID;

public record WorkoutDayDto(
        UUID id,
        int dayNumber,
        String workoutName,
        List<WorkoutExerciseDto> exercises
) {
    public static WorkoutDayDto from(WorkoutDay day) {
        return new WorkoutDayDto(
                day.getId(),
                day.getDayNumber(),
                day.getWorkoutName(),
                day.getExercises().stream().map(WorkoutExerciseDto::from).toList()
        );
    }
}
