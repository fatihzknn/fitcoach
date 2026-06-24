package com.fitcoach.session.dto;

import com.fitcoach.session.SetLog;

import java.math.BigDecimal;
import java.util.UUID;

public record SetLogDto(
        UUID id,
        UUID workoutExerciseId,
        int setNumber,
        BigDecimal weightKg,
        int repsCompleted,
        Integer rirActual,
        String notes
) {
    public static SetLogDto from(SetLog s) {
        return new SetLogDto(
                s.getId(),
                s.getWorkoutExercise().getId(),
                s.getSetNumber(),
                s.getWeightKg(),
                s.getRepsCompleted(),
                s.getRirActual(),
                s.getNotes()
        );
    }
}
