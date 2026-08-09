package com.fitcoach.workout.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CustomPlanDayRequest(
        @NotBlank(message = "Day name is required.")
        @Size(max = 100, message = "Day name is too long.")
        String workoutName,

        @NotEmpty(message = "Each day needs at least one exercise.")
        @Size(max = 12, message = "A day can have at most 12 exercises.")
        List<@Valid CustomPlanExerciseRequest> exercises
) {
}
