package com.fitcoach.workout.dto;

import com.fitcoach.workout.domain.PlanOption;
import jakarta.validation.constraints.NotNull;

public record SelectPlanRequest(
        @NotNull(message = "Plan option is required (RECOMMENDED or ALTERNATIVE).")
        PlanOption option
) {
}
