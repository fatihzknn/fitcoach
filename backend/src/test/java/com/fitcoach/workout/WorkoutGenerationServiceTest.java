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
import com.fitcoach.trainer.TrainerPhilosophy;
import com.fitcoach.workout.dto.PlanOptionsResponse;
import com.fitcoach.workout.dto.WorkoutPlanDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkoutGenerationServiceTest {

    private WorkoutGenerationService service;
    private Map<String, Exercise> exercises;
    private TrainerPhilosophy defaultTrainer;

    @BeforeEach
    void setUp() {
        service = new WorkoutGenerationService();
        exercises = buildExercises();
        defaultTrainer = trainerWith(6, 20, 10, 30, 120, 60, 2, 4, 3, "Evidence-Based");
    }

    @Test
    void beginner3DayProducesFullBodyABPlan() {
        FitnessProfile profile = profile(TrainingBackground.STARTING, 3, MainGoal.MUSCLE_GAIN);

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        assertThat(result.recommended().name()).contains("Full Body");
        assertThat(result.recommended().trainingDaysPerWeek()).isEqualTo(3);
        assertThat(result.recommended().days()).hasSize(3);
        assertThat(result.alternative().name()).isNotEqualTo(result.recommended().name());
    }

    @Test
    void returning3DayAlsoProducesFullBodyPlan() {
        FitnessProfile profile = profile(TrainingBackground.RETURNING, 3, MainGoal.FAT_LOSS);

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        assertThat(result.recommended().days()).hasSize(3);
        assertThat(result.recommended().sustainabilityWarning()).isNull();
    }

    @Test
    void beginner4DayProducesUpperLowerPlan() {
        FitnessProfile profile = profile(TrainingBackground.STARTING, 4, MainGoal.GENERAL_FITNESS);

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        assertThat(result.recommended().name()).contains("Upper");
        assertThat(result.recommended().days()).hasSize(4);
    }

    @Test
    void beginner5DayIncludesSustainabilityWarning() {
        FitnessProfile profile = profile(TrainingBackground.STARTING, 5, MainGoal.MUSCLE_GAIN);

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        assertThat(result.recommended().sustainabilityWarning()).isNotNull().isNotBlank();
        assertThat(result.alternative().sustainabilityWarning()).isNotNull().isNotBlank();
        assertThat(result.recommended().days()).hasSize(5);
    }

    @Test
    void regular3DayProducesIntermediateFullBody() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 3, MainGoal.STRENGTH);

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        assertThat(result.recommended().name()).contains("Intermediate");
        assertThat(result.alternative().name()).contains("Push");
    }

    @Test
    void strengthTrainerUsesLowerRepRangeForCompounds() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 4, MainGoal.STRENGTH);
        TrainerPhilosophy strengthTrainer = trainerWith(3, 6, 8, 12, 180, 90, 1, 5, 3, "Strength Focused");

        PlanOptionsResponse result = service.generateOptions(profile, exercises, strengthTrainer);

        assertThat(result.recommended().days()).hasSize(4);
        // Day 1 exercise 0 is a compound (Barbell Bench Press = PUSH)
        int min = result.recommended().days().get(0).exercises().get(0).repRangeMin();
        assertThat(min).isLessThanOrEqualTo(6);
    }

    @Test
    void minimalistTrainerUsesHigherRepRangeForCompounds() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 4, MainGoal.FAT_LOSS);
        TrainerPhilosophy minimalistTrainer = trainerWith(10, 15, 12, 20, 75, 45, 2, 3, 2, "Minimalist");

        PlanOptionsResponse result = service.generateOptions(profile, exercises, minimalistTrainer);

        int min = result.recommended().days().get(0).exercises().get(0).repRangeMin();
        assertThat(min).isGreaterThanOrEqualTo(10);
    }

    @Test
    void regular5DayIncludesSustainabilityWarning() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 5, MainGoal.MUSCLE_GAIN);

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        assertThat(result.recommended().sustainabilityWarning()).isNotNull();
        assertThat(result.recommended().days()).hasSize(5);
    }

    @Test
    void recommendedAndAlternativeAreAlwaysDifferent() {
        for (TrainingBackground bg : TrainingBackground.values()) {
            for (int days = 3; days <= 5; days++) {
                FitnessProfile profile = profile(bg, days, MainGoal.MUSCLE_GAIN);
                PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

                assertThat(result.recommended().name())
                        .as("Recommended and alternative should differ for %s %d-day", bg, days)
                        .isNotEqualTo(result.alternative().name());
            }
        }
    }

    @Test
    void allExerciseSlotsResolve() {
        for (TrainingBackground bg : TrainingBackground.values()) {
            for (int days = 3; days <= 5; days++) {
                FitnessProfile profile = profile(bg, days, MainGoal.GENERAL_FITNESS);
                PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);
                assertThat(result.recommended().days()).isNotEmpty();
            }
        }
    }

    @Test
    void planNameIncludesTrainerName() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 4, MainGoal.MUSCLE_GAIN);

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        assertThat(result.recommended().name()).contains("Evidence-Based");
        assertThat(result.recommended().trainerPhilosophyName()).isEqualTo("Evidence-Based");
    }

    @Test
    void beginnerParamsCappedRegardlessOfTrainer() {
        // Strength trainer requests 5 compound sets and 3-6 reps,
        // but beginners should be capped to 3 sets and ≥8 rep min
        FitnessProfile profile = profile(TrainingBackground.STARTING, 3, MainGoal.STRENGTH);
        TrainerPhilosophy strengthTrainer = trainerWith(3, 6, 8, 12, 180, 90, 1, 5, 3, "Strength Focused");

        PlanOptionsResponse result = service.generateOptions(profile, exercises, strengthTrainer);

        var firstExercise = result.recommended().days().get(0).exercises().get(0);
        assertThat(firstExercise.sets()).isLessThanOrEqualTo(3);
        assertThat(firstExercise.repRangeMin()).isGreaterThanOrEqualTo(8);
    }

    @Test
    void substitutionAppliesForRegisteredSlug() {
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 4, MainGoal.MUSCLE_GAIN);
        TrainerPhilosophy strengthTrainer = trainerWith(3, 6, 8, 12, 180, 90, 1, 5, 3,
                "Strength Focused", "strength-focused");

        PlanOptionsResponse result = service.generateOptions(profile, exercises, strengthTrainer);

        // Upper B (day index 2) starts with "Dumbbell Bench Press" in the template;
        // strength-focused prefers "Barbell Bench Press" for that slot.
        var upperB = result.recommended().days().get(2);
        assertThat(upperB.workoutName()).isEqualTo("Upper B");
        assertThat(upperB.exercises().get(0).exercise().name()).isEqualTo("Barbell Bench Press");
    }

    @Test
    void noSubstitutionForUnregisteredSlug() {
        // defaultTrainer has no stubbed slug (Mockito default: null) — behavior must
        // match the literal template names exactly, same as before substitution existed.
        FitnessProfile profile = profile(TrainingBackground.REGULAR, 3, MainGoal.MUSCLE_GAIN);

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        var day1 = result.recommended().days().get(0);
        assertThat(day1.exercises().get(0).exercise().name()).isEqualTo("Barbell Bench Press");
    }

    @Test
    void noDuplicateExerciseWithinAnyDayAcrossAllTrainersAndTemplates() {
        List<TrainerPhilosophy> trainers = List.of(
                trainerWith(6, 20, 10, 30, 120, 60, 2, 4, 3, "Evidence-Based", "evidence-based"),
                trainerWith(8, 12, 10, 15, 90, 60, 1, 4, 3, "Classic Bodybuilding", "classic-bodybuilding"),
                trainerWith(3, 6, 8, 12, 180, 90, 1, 5, 3, "Strength Focused", "strength-focused"),
                trainerWith(10, 15, 12, 20, 75, 45, 2, 3, 2, "Minimalist", "minimalist"),
                trainerWith(5, 10, 8, 15, 150, 75, 2, 4, 3, "Women's Physiology Focused", "womens-physiology-focused")
        );

        for (TrainerPhilosophy trainer : trainers) {
            for (TrainingBackground bg : TrainingBackground.values()) {
                for (int days = 3; days <= 5; days++) {
                    FitnessProfile profile = profile(bg, days, MainGoal.MUSCLE_GAIN);
                    PlanOptionsResponse result = service.generateOptions(profile, exercises, trainer);

                    for (WorkoutPlanDto planDto : List.of(result.recommended(), result.alternative())) {
                        for (var day : planDto.days()) {
                            List<String> names = day.exercises().stream()
                                    .map(we -> we.exercise().name())
                                    .toList();
                            assertThat(names)
                                    .as("Trainer %s, %s %d-day, day '%s' should have no duplicate exercises",
                                            trainer.getDisplayName(), bg, days, day.workoutName())
                                    .doesNotHaveDuplicates();
                        }
                    }
                }
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

    private TrainerPhilosophy trainerWith(int cRepMin, int cRepMax, int iRepMin, int iRepMax,
                                           int restC, int restI, int rir,
                                           int cSets, int iSets, String displayName) {
        TrainerPhilosophy t = mock(TrainerPhilosophy.class);
        when(t.getId()).thenReturn(UUID.randomUUID());
        when(t.getDisplayName()).thenReturn(displayName);
        when(t.getCompoundRepMin()).thenReturn(cRepMin);
        when(t.getCompoundRepMax()).thenReturn(cRepMax);
        when(t.getIsolationRepMin()).thenReturn(iRepMin);
        when(t.getIsolationRepMax()).thenReturn(iRepMax);
        when(t.getRestSecondsCompound()).thenReturn(restC);
        when(t.getRestSecondsIsolation()).thenReturn(restI);
        when(t.getRirTarget()).thenReturn(rir);
        when(t.getSetsCompound()).thenReturn(cSets);
        when(t.getSetsIsolation()).thenReturn(iSets);
        return t;
    }

    /** Same as above, but also stubs getSlug() so exercise-substitution logic activates. */
    private TrainerPhilosophy trainerWith(int cRepMin, int cRepMax, int iRepMin, int iRepMax,
                                           int restC, int restI, int rir,
                                           int cSets, int iSets, String displayName, String slug) {
        TrainerPhilosophy t = trainerWith(cRepMin, cRepMax, iRepMin, iRepMax, restC, restI, rir,
                cSets, iSets, displayName);
        when(t.getSlug()).thenReturn(slug);
        return t;
    }

    private Map<String, Exercise> buildExercises() {
        // Correctly maps each exercise to its actual MovementPattern
        // so compound vs isolation detection works in the generator
        record ExDef(String name, MovementPattern pattern) {}
        var defs = new ExDef[]{
                new ExDef("Barbell Bench Press",  MovementPattern.PUSH),
                new ExDef("Dumbbell Bench Press", MovementPattern.PUSH),
                new ExDef("Chest Press Machine",  MovementPattern.PUSH),
                new ExDef("Overhead Press",       MovementPattern.PUSH),
                new ExDef("Push-up",              MovementPattern.PUSH),
                new ExDef("Lat Pulldown",         MovementPattern.PULL),
                new ExDef("Cable Row",            MovementPattern.PULL),
                new ExDef("Dumbbell Row",         MovementPattern.PULL),
                new ExDef("Assisted Pull-up",     MovementPattern.PULL),
                new ExDef("Leg Press",            MovementPattern.SQUAT),
                new ExDef("Goblet Squat",         MovementPattern.SQUAT),
                new ExDef("Romanian Deadlift",    MovementPattern.HINGE),
                new ExDef("Hip Thrust",           MovementPattern.HINGE),
                new ExDef("Leg Curl",             MovementPattern.ISOLATION),
                new ExDef("Leg Extension",        MovementPattern.ISOLATION),
                new ExDef("Lateral Raise",        MovementPattern.ISOLATION),
                new ExDef("Biceps Curl",          MovementPattern.ISOLATION),
                new ExDef("Triceps Pushdown",     MovementPattern.ISOLATION),
        };

        var map = new java.util.HashMap<String, Exercise>();
        for (var def : defs) {
            map.put(def.name(), new Exercise(
                    def.name(), MuscleGroup.CHEST, def.pattern(),
                    DifficultyLevel.BEGINNER,
                    "form cue for " + def.name(),
                    "common mistake for " + def.name()
            ));
        }
        return map;
    }
}
