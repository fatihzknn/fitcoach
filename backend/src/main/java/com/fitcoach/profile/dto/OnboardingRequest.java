package com.fitcoach.profile.dto;

import com.fitcoach.profile.domain.BarbellComfort;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.Sex;
import com.fitcoach.profile.domain.TrainingBackground;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Onboarding answers (Steps 1–6). Constraints mirror the allowed choices in the UI:
 * 3–5 training days, 45/60/75-minute sessions, and sensible bounds for age/height/weight.
 */
public record OnboardingRequest(
        @NotNull(message = "Choose a main goal.")
        MainGoal mainGoal,

        @NotNull(message = "Choose your training background.")
        TrainingBackground trainingBackground,

        @NotNull(message = "Choose how many days per week you can train.")
        @Min(value = 3, message = "Training days must be between 3 and 5.")
        @Max(value = 5, message = "Training days must be between 3 and 5.")
        Integer trainingDaysPerWeek,

        @NotNull(message = "Choose your session length.")
        Integer sessionDurationMinutes,

        @NotNull(message = "Enter your age.")
        @Min(value = 13, message = "Age must be at least 13.")
        @Max(value = 100, message = "Enter a valid age.")
        Integer age,

        @NotNull(message = "Enter your height.")
        @Min(value = 120, message = "Enter a valid height in cm.")
        @Max(value = 230, message = "Enter a valid height in cm.")
        Integer heightCm,

        @NotNull(message = "Enter your weight.")
        @DecimalMin(value = "30.0", message = "Enter a valid weight in kg.")
        @DecimalMax(value = "300.0", message = "Enter a valid weight in kg.")
        Double weightKg,

        @NotNull(message = "Choose your sex.")
        Sex sex,

        @NotNull(message = "Select any pain or injury areas (or None).")
        Set<PainArea> painAreas,

        @NotNull(message = "Choose your comfort with barbell lifts.")
        BarbellComfort barbellComfort
) {
    @AssertTrue(message = "Session length must be 45, 60, or 75 minutes.")
    public boolean isSessionDurationValid() {
        return sessionDurationMinutes == null
                || sessionDurationMinutes == 45
                || sessionDurationMinutes == 60
                || sessionDurationMinutes == 75;
    }
}
