package com.fitcoach.workout.dto;

public record PlanOptionsResponse(
        WorkoutPlanDto recommended,
        WorkoutPlanDto alternative
) {
}
