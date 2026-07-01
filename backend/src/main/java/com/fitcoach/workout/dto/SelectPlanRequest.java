package com.fitcoach.workout.dto;

import com.fitcoach.workout.domain.PlanOption;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SelectPlanRequest(
        @NotNull(message = "Plan option is required (RECOMMENDED or ALTERNATIVE).")
        PlanOption option,
        UUID trainerId
) {
}
