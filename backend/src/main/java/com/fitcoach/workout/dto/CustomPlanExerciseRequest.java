package com.fitcoach.workout.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CustomPlanExerciseRequest(
        @NotNull(message = "Exercise is required.")
        UUID exerciseId,

        @Min(value = 1, message = "Sets must be at least 1.")
        @Max(value = 10, message = "Sets must be at most 10.")
        int sets,

        @Min(value = 1, message = "Rep range min must be at least 1.")
        int repRangeMin,

        @Min(value = 1, message = "Rep range max must be at least 1.")
        int repRangeMax,

        @NotBlank(message = "RIR guidance is required.")
        @Size(max = 20, message = "RIR guidance is too long.")
        String rirGuidance,

        @Min(value = 0, message = "Rest seconds cannot be negative.")
        int restSeconds
) {
    @AssertTrue(message = "Rep range max must be greater than or equal to rep range min.")
    public boolean isRepRangeValid() {
        return repRangeMax >= repRangeMin;
    }
}
