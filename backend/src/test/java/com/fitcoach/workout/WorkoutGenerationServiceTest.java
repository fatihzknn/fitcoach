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
    void noDuplicateExerciseWithinAnyDayAcrossAllTrainersTemplatesAndPainAreas() {
        List<TrainerPhilosophy> trainers = List.of(
                trainerWith(6, 20, 10, 30, 120, 60, 2, 4, 3, "Evidence-Based", "evidence-based"),
                trainerWith(8, 12, 10, 15, 90, 60, 1, 4, 3, "Classic Bodybuilding", "classic-bodybuilding"),
                trainerWith(3, 6, 8, 12, 180, 90, 1, 5, 3, "Strength Focused", "strength-focused"),
                trainerWith(10, 15, 12, 20, 75, 45, 2, 3, 2, "Minimalist", "minimalist"),
                trainerWith(5, 10, 8, 15, 150, 75, 2, 4, 3, "Women's Physiology Focused", "womens-physiology-focused")
        );
        List<Set<PainArea>> painCombos = List.of(
                Set.of(PainArea.NONE),
                Set.of(PainArea.KNEE),
                Set.of(PainArea.LOWER_BACK),
                Set.of(PainArea.SHOULDER),
                Set.of(PainArea.KNEE, PainArea.LOWER_BACK, PainArea.SHOULDER)
        );

        for (TrainerPhilosophy trainer : trainers) {
            for (Set<PainArea> painAreas : painCombos) {
                for (TrainingBackground bg : TrainingBackground.values()) {
                    for (int days = 3; days <= 5; days++) {
                        FitnessProfile profile = profileWithPain(bg, days, MainGoal.MUSCLE_GAIN, painAreas);
                        PlanOptionsResponse result = service.generateOptions(profile, exercises, trainer);

                        for (WorkoutPlanDto planDto : List.of(result.recommended(), result.alternative())) {
                            for (var day : planDto.days()) {
                                List<String> names = day.exercises().stream()
                                        .map(we -> we.exercise().name())
                                        .toList();
                                assertThat(names)
                                        .as("Trainer %s, pain %s, %s %d-day, day '%s' should have no duplicate exercises",
                                                trainer.getDisplayName(), painAreas, bg, days, day.workoutName())
                                        .doesNotHaveDuplicates();
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void kneeProfileAvoidsLegExtension() {
        // STARTING+3's alternative (Machine-Friendly Full Body), "Full Body B" has
        // Leg Extension but not Leg Curl, so the substitution isn't blocked by the
        // in-day collision guard — a clean day to verify the avoidance itself. (Other
        // days in this template legitimately keep Leg Extension because Leg Curl is
        // already present there — that's the collision guard working correctly, not
        // a place this test should assert on.)
        FitnessProfile profile = profileWithPain(TrainingBackground.STARTING, 3, MainGoal.MUSCLE_GAIN,
                Set.of(PainArea.KNEE));

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        List<String> fullBodyB = exercisesInDay(result.alternative(), "Full Body B");
        assertThat(fullBodyB).doesNotContain("Leg Extension");
        assertThat(fullBodyB).contains("Leg Curl");
    }

    @Test
    void lowerBackProfileAvoidsRomanianDeadlift() {
        // REGULAR+3's recommended (Full Body 3-Day), "Full Body A" has Romanian
        // Deadlift but not Hip Thrust, so the substitution isn't blocked by the
        // in-day collision guard.
        FitnessProfile profile = profileWithPain(TrainingBackground.REGULAR, 3, MainGoal.MUSCLE_GAIN,
                Set.of(PainArea.LOWER_BACK));

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        List<String> fullBodyA = exercisesInDay(result.recommended(), "Full Body A");
        assertThat(fullBodyA).doesNotContain("Romanian Deadlift");
        assertThat(fullBodyA).contains("Hip Thrust");
    }

    @Test
    void shoulderProfileAvoidsOverheadPress() {
        // STARTING+4's recommended (Upper/Lower Split), "Upper A" has Overhead Press
        // but not Chest Press Machine, so the substitution isn't blocked. ("Upper B"
        // in the same plan legitimately keeps Overhead Press because Chest Press
        // Machine is already present there.)
        FitnessProfile profile = profileWithPain(TrainingBackground.STARTING, 4, MainGoal.MUSCLE_GAIN,
                Set.of(PainArea.SHOULDER));

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        List<String> upperA = exercisesInDay(result.recommended(), "Upper A");
        assertThat(upperA).doesNotContain("Overhead Press");
        assertThat(upperA).contains("Chest Press Machine");
    }

    @Test
    void noPainProfileKeepsOverheadPress() {
        // Regression guard: a NONE-pain profile must still get the literal template
        // exercise in the same day, same as before pain avoidance existed.
        FitnessProfile profile = profile(TrainingBackground.STARTING, 4, MainGoal.MUSCLE_GAIN);

        PlanOptionsResponse result = service.generateOptions(profile, exercises, defaultTrainer);

        assertThat(exercisesInDay(result.recommended(), "Upper A")).contains("Overhead Press");
    }

    @Test
    void painAvoidanceTakesPriorityOverTrainerPreference() {
        // Both the LOWER_BACK avoidance rule and the womens-physiology-focused trainer
        // target the same substitution (Romanian Deadlift -> Hip Thrust) — same outcome
        // either way, but this confirms pain resolution runs first without error and
        // the two sources don't conflict when they happen to agree.
        FitnessProfile profile = profileWithPain(TrainingBackground.REGULAR, 3, MainGoal.MUSCLE_GAIN,
                Set.of(PainArea.LOWER_BACK));
        TrainerPhilosophy womensTrainer = trainerWith(5, 10, 8, 15, 150, 75, 2, 4, 3,
                "Women's Physiology Focused", "womens-physiology-focused");

        PlanOptionsResponse result = service.generateOptions(profile, exercises, womensTrainer);

        List<String> fullBodyA = exercisesInDay(result.recommended(), "Full Body A");
        assertThat(fullBodyA).doesNotContain("Romanian Deadlift");
        assertThat(fullBodyA).contains("Hip Thrust");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> allExerciseNames(WorkoutPlanDto plan) {
        return plan.days().stream()
                .flatMap(day -> day.exercises().stream())
                .map(we -> we.exercise().name())
                .toList();
    }

    /** Exercise names for the first day matching this workout name (day names can
     *  repeat across an alternating split, e.g. "Full Body A" on both day 1 and 3). */
    private List<String> exercisesInDay(WorkoutPlanDto plan, String workoutName) {
        return plan.days().stream()
                .filter(day -> day.workoutName().equals(workoutName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No day named '" + workoutName + "' in plan " + plan.name()))
                .exercises().stream()
                .map(we -> we.exercise().name())
                .toList();
    }

    private FitnessProfile profile(TrainingBackground background, int days, MainGoal goal) {
        return profileWithPain(background, days, goal, Set.of(PainArea.NONE));
    }

    private FitnessProfile profileWithPain(TrainingBackground background, int days, MainGoal goal,
                                            Set<PainArea> painAreas) {
        FitnessProfile p = new FitnessProfile(UUID.randomUUID());
        p.setTrainingBackground(background);
        p.setTrainingDaysPerWeek(days);
        p.setMainGoal(goal);
        p.setSex(Sex.MALE);
        p.setAge(25);
        p.setHeightCm(175);
        p.setWeightKg(75.0);
        p.setSessionDurationMinutes(60);
        p.setPainAreas(painAreas);
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
