package com.fitcoach.workout;

import com.fitcoach.exercise.Exercise;
import com.fitcoach.exercise.domain.DifficultyLevel;
import com.fitcoach.exercise.domain.MovementPattern;
import com.fitcoach.exercise.domain.MuscleGroup;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.Sex;
import com.fitcoach.profile.domain.TrainingBackground;
import com.fitcoach.workout.dto.PlanOptionsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutGenerationServiceTest {

    private WorkoutGenerationService service;
    private Map<String, Exercise> exercises;

    @BeforeEach
    void setUp() {
        service = new WorkoutGenerationService();
        exercises = buildExercises();
    }

    @Test
    void beginner3DayProducesFullBodyABPlan() {
        FitnessProfile profile = profile(TrainingBackground.STARTING, 3, MainGoal.MUSCLE_GAIN);

        PlanOptionsResponse result = service.generateOptions(profile, exercises);

        assertThat(result.recommended().name()).contains("Full Body");
        assertThat(result.recommended().trainingDaysPerWeek()).isEqualTo(3);
        assertThat(result.recommended().days()).hasSize(3);
        assertThat(result.alternative().name()).isNotEqualTo(result.recommended().name());
    }

    @Test
    void returning3DayAlsoProducesFullBodyPlan() {
        FitnessProfile profile = profile(TrainingBackground.RETURNING, 3, MainGoal.FAT_LOSS);

        PlanOptionsResponse result = service.generateOptions(profile, exercises);

        assertThat(result.recommended().days()).hasSize(3);
        assertThat(result.recommended().sustainabilityWarning()).isNull();
    }

    @Test
    void beginner4DayProducesUpperLowerPlan() {
        FitnessProfile profile = profile(TrainingBackground.STARTING, 4, MainGoal.GENERAL_FITNESS);

        PlanOptionsResponse result = service.generateOptions(profile, exercises);

        assertThat(result.recommended().name()).contains("Upper");
        assertThat(result.recommended().days()).hasSize(4);
    }

    @Test
    void beginner5DayIncludesSustainabilityWarning() {
        FitnessProfile profile = profile(TrainingBackground.STARTING, 5, MainGoal.MUSCLE_GAIN);

        PlanOptionsResponse result = service.generateOptions(profile, exercises);

        assertThat(result.recommended().sustainabilityWarning()).isNotNull().isNotBlank();
        assertThat(result.alternative().sustainabilityWarning()).isNotNull().isNotBlank();
        assertThat(result.recommended().days()).hasSize(5);
    }

    @Test
    void regular3DayProducesIntermediateFullBody() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 3, MainGoal.STRENGTH);

        PlanOptionsResponse result = service.generateOptions(profile, exercises);

        assertThat(result.recommended().name()).contains("Intermediate");
        assertThat(result.alternative().name()).contains("Push");
    }

    @Test
    void regular4DayStrengthUsesLowerRepRange() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 4, MainGoal.STRENGTH);

        PlanOptionsResponse result = service.generateOptions(profile, exercises);

        assertThat(result.recommended().days()).hasSize(4);
        // Strength rep range should be 5-8; verify at least one exercise hits this
        int min = result.recommended().days().get(0).exercises().get(0).repRangeMin();
        assertThat(min).isLessThanOrEqualTo(8);
    }

    @Test
    void regular4DayFatLossUsesHigherRepRange() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 4, MainGoal.FAT_LOSS);

        PlanOptionsResponse result = service.generateOptions(profile, exercises);

        int min = result.recommended().days().get(0).exercises().get(0).repRangeMin();
        assertThat(min).isGreaterThanOrEqualTo(12);
    }

    @Test
    void regular5DayIncludesSustainabilityWarning() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 5, MainGoal.MUSCLE_GAIN);

        PlanOptionsResponse result = service.generateOptions(profile, exercises);

        assertThat(result.recommended().sustainabilityWarning()).isNotNull();
        assertThat(result.recommended().days()).hasSize(5);
    }

    @Test
    void recommendedAndAlternativeAreAlwaysDifferent() {
        for (TrainingBackground bg : TrainingBackground.values()) {
            for (int days = 3; days <= 5; days++) {
                FitnessProfile profile = profile(bg, days, MainGoal.MUSCLE_GAIN);
                PlanOptionsResponse result = service.generateOptions(profile, exercises);

                assertThat(result.recommended().name())
                        .as("Recommended and alternative should differ for %s %d-day", bg, days)
                        .isNotEqualTo(result.alternative().name());
            }
        }
    }

    @Test
    void allExerciseSlotsResolve() {
        // If any exercise name is wrong, the service throws IllegalStateException
        for (TrainingBackground bg : TrainingBackground.values()) {
            for (int days = 3; days <= 5; days++) {
                FitnessProfile profile = profile(bg, days, MainGoal.GENERAL_FITNESS);
                // Should not throw
                PlanOptionsResponse result = service.generateOptions(profile, exercises);
                assertThat(result.recommended().days()).isNotEmpty();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private FitnessProfile profile(TrainingBackground background, int days, MainGoal goal) {
        FitnessProfile p = new FitnessProfile(UUID.randomUUID());
        p.setTrainingBackground(background);
        p.setTrainingDaysPerWeek(days);
        p.setMainGoal(goal);
        p.setSex(Sex.MALE);
        p.setAge(25);
        p.setHeightCm(175);
        p.setWeightKg(75.0);
        p.setSessionDurationMinutes(60);
        p.setPainAreas(Set.of(PainArea.NONE));
        return p;
    }

    private Map<String, Exercise> buildExercises() {
        String[] names = {
                "Barbell Bench Press", "Dumbbell Bench Press", "Chest Press Machine",
                "Lat Pulldown", "Cable Row", "Leg Press", "Romanian Deadlift",
                "Leg Curl", "Leg Extension", "Lateral Raise", "Biceps Curl",
                "Triceps Pushdown", "Push-up", "Assisted Pull-up", "Goblet Squat",
                "Overhead Press", "Dumbbell Row", "Hip Thrust"
        };
        Map<String, Exercise> map = new HashMap<>();
        for (String name : names) {
            Exercise e = new Exercise(
                    name,
                    MuscleGroup.CHEST,
                    MovementPattern.PUSH,
                    DifficultyLevel.BEGINNER,
                    "form cue for " + name,
                    "common mistake for " + name
            );
            map.put(name, e);
        }
        return map;
    }
}
