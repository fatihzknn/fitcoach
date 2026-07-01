package com.fitcoach.workout.dto;

import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.workout.WorkoutPlan;

import java.util.List;
import java.util.UUID;

public record WorkoutPlanDto(
        UUID id,
        String name,
        MainGoal goal,
        int trainingDaysPerWeek,
        boolean isActive,
        String sustainabilityWarning,
        List<WorkoutDayDto> days,
        UUID trainerPhilosophyId,
        String trainerPhilosophyName
) {
    public static WorkoutPlanDto from(WorkoutPlan plan) {
        return new WorkoutPlanDto(
                plan.getId(),
                plan.getName(),
                plan.getGoal(),
                plan.getTrainingDaysPerWeek(),
                plan.isActive(),
                plan.getSustainabilityWarning(),
                plan.getDays().stream().map(WorkoutDayDto::from).toList(),
                plan.getTrainerPhilosophyId(),
                plan.getTrainerPhilosophyName()
        );
    }
}
