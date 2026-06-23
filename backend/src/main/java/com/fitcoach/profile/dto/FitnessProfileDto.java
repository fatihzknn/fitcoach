package com.fitcoach.profile.dto;

import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.Sex;
import com.fitcoach.profile.domain.TrainingBackground;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public record FitnessProfileDto(
        UUID id,
        MainGoal mainGoal,
        TrainingBackground trainingBackground,
        int trainingDaysPerWeek,
        int sessionDurationMinutes,
        int age,
        int heightCm,
        double weightKg,
        Sex sex,
        Set<PainArea> painAreas,
        boolean onboardingCompleted,
        Instant onboardingCompletedAt
) {
    public static FitnessProfileDto from(FitnessProfile p) {
        return new FitnessProfileDto(
                p.getId(),
                p.getMainGoal(),
                p.getTrainingBackground(),
                p.getTrainingDaysPerWeek(),
                p.getSessionDurationMinutes(),
                p.getAge(),
                p.getHeightCm(),
                p.getWeightKg(),
                p.getSex(),
                new TreeSet<>(p.getPainAreas()),
                p.isOnboardingCompleted(),
                p.getOnboardingCompletedAt());
    }
}
