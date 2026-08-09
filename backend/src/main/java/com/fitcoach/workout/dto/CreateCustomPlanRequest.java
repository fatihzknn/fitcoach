package com.fitcoach.workout.dto;

import com.fitcoach.profile.domain.MainGoal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCustomPlanRequest(
        @NotBlank(message = "Plan name is required.")
        @Size(max = 100, message = "Plan name is too long.")
        String name,

        @NotNull(message = "Choose a main goal.")
        MainGoal goal,

        @NotEmpty(message = "A plan needs at least one day.")
        @Size(max = 7, message = "A plan can have at most 7 days.")
        List<@Valid CustomPlanDayRequest> days
) {
}
