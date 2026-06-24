package com.fitcoach.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record LogSetRequest(
        @NotNull(message = "workoutExerciseId is required")
        UUID workoutExerciseId,

        @Min(value = 1, message = "setNumber must be at least 1")
        int setNumber,

        BigDecimal weightKg,

        @Min(value = 0, message = "repsCompleted cannot be negative")
        int repsCompleted,

        Integer rirActual
) {}
