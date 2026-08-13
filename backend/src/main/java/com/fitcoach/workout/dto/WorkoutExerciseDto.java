package com.fitcoach.workout.dto;

import com.fitcoach.exercise.dto.ExerciseDto;
import com.fitcoach.workout.WorkoutExercise;

import java.util.UUID;

public record WorkoutExerciseDto(
        UUID id,
        int orderIndex,
        int sets,
        int repRangeMin,
        int repRangeMax,
        String rirGuidance,
        int restSeconds,
        ExerciseDto exercise,
        /** Non-null when the user has swapped this slot for a different exercise —
         *  persisted for the life of the plan. {@code exercise} above always stays
         *  the original template pick. */
        ExerciseDto substitutedExercise
) {
    public static WorkoutExerciseDto from(WorkoutExercise we) {
        return new WorkoutExerciseDto(
                we.getId(),
                we.getOrderIndex(),
                we.getSets(),
                we.getRepRangeMin(),
                we.getRepRangeMax(),
                we.getRirGuidance(),
                we.getRestSeconds(),
                ExerciseDto.from(we.getExercise()),
                we.getSubstitutedExercise() != null ? ExerciseDto.from(we.getSubstitutedExercise()) : null
        );
    }
}
