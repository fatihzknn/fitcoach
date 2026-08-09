package com.fitcoach.workout;

import com.fitcoach.exercise.Exercise;
import com.fitcoach.exercise.domain.MovementPattern;
import com.fitcoach.exercise.dto.ExerciseDto;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.domain.BarbellComfort;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.TrainingBackground;
import com.fitcoach.trainer.TrainerPhilosophy;
import com.fitcoach.workout.dto.PlanOptionsResponse;
import com.fitcoach.workout.dto.WorkoutDayDto;
import com.fitcoach.workout.dto.WorkoutExerciseDto;
import com.fitcoach.workout.dto.WorkoutPlanDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Deterministic workout plan generator. Takes the user's fitness profile and a trainer
 * philosophy, and returns two plan options without writing to the database.
 *
 * The split structure (Full Body / Upper-Lower / PPL) is chosen by experience level + days.
 * Volume (sets, rep ranges, rest, RIR) comes from the trainer philosophy, with beginner
 * safety caps applied automatically. Which specific exercise fills a slot can also vary by
 * trainer (see TrainerExercisePreferences) — e.g. a "strength-focused" trainer prefers
 * barbell movements where a template calls for a generic bench-press slot.
 */
@Service
public class WorkoutGenerationService {

    private static final String SUSTAINABILITY_WARNING =
            "5 sessions per week is demanding. Ensure 7–8 hours of sleep and listen to your body.";

    // ─────────────────────────────────────────────────────────────────────────
    // Template parameters derived from the trainer philosophy
    // ─────────────────────────────────────────────────────────────────────────

    private record TemplateParams(
            int cSets, int cRepMin, int cRepMax, String cRir, int cRest,
            int iSets, int iRepMin, int iRepMax, String iRir, int iRest
    ) {}

    private TemplateParams buildParams(FitnessProfile profile, TrainerPhilosophy trainer) {
        boolean beginner = isBeginner(profile);

        // Beginners cap sets and use higher RIR (train further from failure)
        int cSets = beginner ? Math.min(trainer.getSetsCompound(), 3) : trainer.getSetsCompound();
        int iSets = beginner ? Math.min(trainer.getSetsIsolation(), 2) : trainer.getSetsIsolation();

        // For very low rep ranges (strength trainer), shift beginners into a safer window
        int cRepMin = beginner ? Math.max(trainer.getCompoundRepMin(), 8) : trainer.getCompoundRepMin();
        int cRepMax = beginner ? Math.max(trainer.getCompoundRepMax(), 12) : trainer.getCompoundRepMax();
        int iRepMin = trainer.getIsolationRepMin();
        int iRepMax = trainer.getIsolationRepMax();

        int rir = beginner ? Math.max(trainer.getRirTarget(), 2) : trainer.getRirTarget();
        String cRir = rir + " RIR";
        String iRir = beginner ? (rir + "-" + (rir + 1) + " RIR") : (rir + " RIR");

        return new TemplateParams(
                cSets, cRepMin, cRepMax, cRir, trainer.getRestSecondsCompound(),
                iSets, iRepMin, iRepMax, iRir, trainer.getRestSecondsIsolation()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public PlanOptionsResponse generateOptions(FitnessProfile profile,
                                               Map<String, Exercise> exercises,
                                               TrainerPhilosophy trainer) {
        boolean beginner = isBeginner(profile);
        int days = profile.getTrainingDaysPerWeek();
        TemplateParams tp = buildParams(profile, trainer);

        WorkoutPlanDto recommended;
        WorkoutPlanDto alternative;

        if (beginner) {
            switch (days) {
                case 3 -> {
                    recommended = buildBeginnerFullBodyAB(profile, exercises, null, tp, trainer);
                    alternative = buildBeginnerMachineFriendly(profile, exercises, null, tp, trainer);
                }
                case 4 -> {
                    recommended = buildBeginnerUpperLower(profile, exercises, null, tp, trainer);
                    alternative = buildBeginnerFullBodyX4(profile, exercises, null, tp, trainer);
                }
                default -> {
                    recommended = buildBeginnerUpperLowerPlusArms(profile, exercises, SUSTAINABILITY_WARNING, tp, trainer);
                    alternative = buildPPLPartial(profile, exercises, SUSTAINABILITY_WARNING, false, tp, trainer);
                }
            }
        } else {
            switch (days) {
                case 3 -> {
                    recommended = buildRegularFullBody(profile, exercises, null, tp, trainer);
                    alternative = buildPPL(profile, exercises, null, tp, trainer);
                }
                case 4 -> {
                    recommended = buildRegularUpperLower(profile, exercises, null, tp, trainer);
                    alternative = buildPPLPartial(profile, exercises, null, true, tp, trainer);
                }
                default -> {
                    recommended = buildPPLPlusUpperLower(profile, exercises, SUSTAINABILITY_WARNING, tp, trainer);
                    alternative = buildRegularUpperLowerX5(profile, exercises, SUSTAINABILITY_WARNING, tp, trainer);
                }
            }
        }

        return new PlanOptionsResponse(recommended, alternative);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Beginner templates
    // ─────────────────────────────────────────────────────────────────────────

    private WorkoutPlanDto buildBeginnerFullBodyAB(FitnessProfile p, Map<String, Exercise> ex,
                                                    String warning, TemplateParams tp,
                                                    TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Full Body A", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Lat Pulldown", "Leg Press",
                        "Romanian Deadlift", "Lateral Raise", "Biceps Curl")),
                buildDay(2, "Full Body B", daySlots(ex, tp, trainer, p,
                        "Dumbbell Bench Press", "Cable Row", "Leg Press",
                        "Leg Curl", "Leg Extension", "Triceps Pushdown")),
                buildDay(3, "Full Body A", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Lat Pulldown", "Leg Press",
                        "Romanian Deadlift", "Lateral Raise", "Biceps Curl"))
        );
        return plan("Full Body 3-Day (A/B)", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildBeginnerMachineFriendly(FitnessProfile p, Map<String, Exercise> ex,
                                                         String warning, TemplateParams tp,
                                                         TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Full Body A", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Lat Pulldown", "Leg Press",
                        "Leg Curl", "Lateral Raise", "Biceps Curl")),
                buildDay(2, "Full Body B", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Cable Row", "Leg Press",
                        "Leg Extension", "Lateral Raise", "Triceps Pushdown")),
                buildDay(3, "Full Body A", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Lat Pulldown", "Leg Press",
                        "Leg Curl", "Lateral Raise", "Biceps Curl"))
        );
        return plan("Machine-Friendly Full Body", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildBeginnerUpperLower(FitnessProfile p, Map<String, Exercise> ex,
                                                    String warning, TemplateParams tp,
                                                    TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Upper A", daySlots(ex, tp, trainer, p,
                        "Dumbbell Bench Press", "Lat Pulldown", "Overhead Press",
                        "Dumbbell Row", "Biceps Curl", "Triceps Pushdown")),
                buildDay(2, "Lower A", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Leg Curl",
                        "Leg Extension", "Hip Thrust")),
                buildDay(3, "Upper B", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Cable Row", "Overhead Press",
                        "Lat Pulldown", "Lateral Raise", "Triceps Pushdown")),
                buildDay(4, "Lower B", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Goblet Squat",
                        "Leg Curl", "Hip Thrust"))
        );
        return plan("Upper / Lower Split", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildBeginnerFullBodyX4(FitnessProfile p, Map<String, Exercise> ex,
                                                    String warning, TemplateParams tp,
                                                    TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Full Body A", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Lat Pulldown", "Leg Press",
                        "Romanian Deadlift", "Lateral Raise")),
                buildDay(2, "Full Body B", daySlots(ex, tp, trainer, p,
                        "Dumbbell Bench Press", "Cable Row", "Leg Press",
                        "Leg Curl", "Biceps Curl")),
                buildDay(3, "Full Body A", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Lat Pulldown", "Leg Press",
                        "Romanian Deadlift", "Lateral Raise")),
                buildDay(4, "Full Body B", daySlots(ex, tp, trainer, p,
                        "Dumbbell Bench Press", "Cable Row", "Leg Press",
                        "Leg Curl", "Biceps Curl"))
        );
        return plan("Full Body 4-Day (A/B/A/B)", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildBeginnerUpperLowerPlusArms(FitnessProfile p, Map<String, Exercise> ex,
                                                            String warning, TemplateParams tp,
                                                            TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Upper", daySlots(ex, tp, trainer, p,
                        "Dumbbell Bench Press", "Lat Pulldown", "Overhead Press",
                        "Cable Row", "Lateral Raise")),
                buildDay(2, "Lower", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Leg Curl",
                        "Leg Extension", "Hip Thrust")),
                buildDay(3, "Upper", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Dumbbell Row", "Overhead Press",
                        "Lat Pulldown", "Lateral Raise")),
                buildDay(4, "Lower", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Goblet Squat",
                        "Leg Curl", "Hip Thrust")),
                buildDay(5, "Arms & Shoulders", daySlots(ex, tp, trainer, p,
                        "Biceps Curl", "Triceps Pushdown", "Lateral Raise", "Overhead Press"))
        );
        return plan("Upper / Lower + Arms Focus", p, warning, days, trainer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Regular (intermediate) templates
    // ─────────────────────────────────────────────────────────────────────────

    private WorkoutPlanDto buildRegularFullBody(FitnessProfile p, Map<String, Exercise> ex,
                                                 String warning, TemplateParams tp,
                                                 TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Full Body A", daySlots(ex, tp, trainer, p,
                        "Barbell Bench Press", "Lat Pulldown", "Leg Press",
                        "Romanian Deadlift", "Lateral Raise", "Biceps Curl")),
                buildDay(2, "Full Body B", daySlots(ex, tp, trainer, p,
                        "Dumbbell Bench Press", "Cable Row", "Leg Press",
                        "Hip Thrust", "Overhead Press", "Triceps Pushdown")),
                buildDay(3, "Full Body C", daySlots(ex, tp, trainer, p,
                        "Barbell Bench Press", "Dumbbell Row", "Goblet Squat",
                        "Romanian Deadlift", "Lateral Raise", "Biceps Curl"))
        );
        return plan("Full Body 3-Day (Intermediate)", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildPPL(FitnessProfile p, Map<String, Exercise> ex,
                                     String warning, TemplateParams tp,
                                     TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Push", daySlots(ex, tp, trainer, p,
                        "Barbell Bench Press", "Overhead Press", "Chest Press Machine",
                        "Lateral Raise", "Triceps Pushdown")),
                buildDay(2, "Pull", daySlots(ex, tp, trainer, p,
                        "Lat Pulldown", "Cable Row", "Dumbbell Row",
                        "Biceps Curl", "Assisted Pull-up")),
                buildDay(3, "Legs", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Leg Curl",
                        "Leg Extension", "Hip Thrust"))
        );
        return plan("Push / Pull / Legs", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildRegularUpperLower(FitnessProfile p, Map<String, Exercise> ex,
                                                   String warning, TemplateParams tp,
                                                   TrainerPhilosophy trainer) {
        String name = "Upper / Lower (" + labelForGoal(p.getMainGoal()) + ")";
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Upper A", daySlots(ex, tp, trainer, p,
                        "Barbell Bench Press", "Lat Pulldown", "Overhead Press",
                        "Cable Row", "Lateral Raise", "Biceps Curl")),
                buildDay(2, "Lower A", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Leg Curl",
                        "Leg Extension", "Hip Thrust")),
                buildDay(3, "Upper B", daySlots(ex, tp, trainer, p,
                        "Dumbbell Bench Press", "Dumbbell Row", "Overhead Press",
                        "Lat Pulldown", "Lateral Raise", "Triceps Pushdown")),
                buildDay(4, "Lower B", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Goblet Squat",
                        "Leg Curl", "Hip Thrust"))
        );
        return plan(name, p, warning, days, trainer);
    }

    private WorkoutPlanDto buildPPLPartial(FitnessProfile p, Map<String, Exercise> ex,
                                            String warning, boolean isRegular,
                                            TemplateParams tp, TrainerPhilosophy trainer) {
        String name = isRegular ? "Push / Pull / Legs (4-Day)" : "PPL + Upper Intro";
        List<WorkoutDayDto> days = new ArrayList<>();
        days.add(buildDay(1, "Push", daySlots(ex, tp, trainer, p,
                "Barbell Bench Press", "Overhead Press", "Lateral Raise", "Triceps Pushdown")));
        days.add(buildDay(2, "Pull", daySlots(ex, tp, trainer, p,
                "Lat Pulldown", "Cable Row", "Dumbbell Row", "Biceps Curl")));
        days.add(buildDay(3, "Legs", daySlots(ex, tp, trainer, p,
                "Leg Press", "Romanian Deadlift", "Leg Curl", "Hip Thrust")));
        days.add(buildDay(4, "Upper", daySlots(ex, tp, trainer, p,
                "Dumbbell Bench Press", "Dumbbell Row", "Overhead Press", "Lateral Raise", "Biceps Curl")));
        return plan(name, p, warning, days, trainer);
    }

    private WorkoutPlanDto buildPPLPlusUpperLower(FitnessProfile p, Map<String, Exercise> ex,
                                                   String warning, TemplateParams tp,
                                                   TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Push", daySlots(ex, tp, trainer, p,
                        "Barbell Bench Press", "Overhead Press", "Chest Press Machine",
                        "Lateral Raise", "Triceps Pushdown")),
                buildDay(2, "Pull", daySlots(ex, tp, trainer, p,
                        "Lat Pulldown", "Cable Row", "Dumbbell Row",
                        "Assisted Pull-up", "Biceps Curl")),
                buildDay(3, "Legs", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Leg Curl",
                        "Leg Extension", "Hip Thrust")),
                buildDay(4, "Upper", daySlots(ex, tp, trainer, p,
                        "Dumbbell Bench Press", "Lat Pulldown", "Overhead Press",
                        "Dumbbell Row", "Lateral Raise")),
                buildDay(5, "Lower", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Goblet Squat",
                        "Leg Curl", "Hip Thrust"))
        );
        return plan("PPL + Upper / Lower (5-Day)", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildRegularUpperLowerX5(FitnessProfile p, Map<String, Exercise> ex,
                                                     String warning, TemplateParams tp,
                                                     TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Upper A", daySlots(ex, tp, trainer, p,
                        "Barbell Bench Press", "Lat Pulldown", "Overhead Press",
                        "Cable Row", "Lateral Raise")),
                buildDay(2, "Lower A", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Leg Curl", "Hip Thrust")),
                buildDay(3, "Upper B", daySlots(ex, tp, trainer, p,
                        "Dumbbell Bench Press", "Dumbbell Row", "Overhead Press",
                        "Lat Pulldown", "Triceps Pushdown")),
                buildDay(4, "Lower B", daySlots(ex, tp, trainer, p,
                        "Leg Press", "Romanian Deadlift", "Goblet Squat",
                        "Leg Extension", "Hip Thrust")),
                buildDay(5, "Upper C", daySlots(ex, tp, trainer, p,
                        "Chest Press Machine", "Cable Row", "Lateral Raise",
                        "Biceps Curl", "Triceps Pushdown"))
        );
        return plan("Upper / Lower × 5 (High Frequency)", p, warning, days, trainer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private WorkoutPlanDto plan(String baseName, FitnessProfile p, String warning,
                                 List<WorkoutDayDto> days, TrainerPhilosophy trainer) {
        String name = baseName + " · " + trainer.getDisplayName();
        return new WorkoutPlanDto(
                UUID.randomUUID(),
                name,
                p.getMainGoal(),
                p.getTrainingDaysPerWeek(),
                false,
                warning,
                days,
                trainer.getId(),
                trainer.getDisplayName(),
                false, // a freshly-generated candidate plan is never deload-due
                false  // never custom — this is the deterministic-generator path
        );
    }

    private WorkoutDayDto buildDay(int dayNumber, String workoutName, List<WorkoutExerciseDto> exercises) {
        return new WorkoutDayDto(UUID.randomUUID(), dayNumber, workoutName, exercises);
    }

    /**
     * Resolves a whole day's exercise slots at once so trainer-preferred substitutions
     * (see TrainerExercisePreferences) can be checked against the other exercises
     * already placed in the same day — several templates deliberately place multiple
     * members of the same movement family in one day (e.g. all four pull variants on
     * a "Pull" day, or both Barbell Bench Press and Chest Press Machine on a "Push"
     * day), so a substitution must never introduce a duplicate, whether the collision
     * is with another slot's *literal* canonical name or with another slot's
     * *already-substituted* result.
     */
    private List<WorkoutExerciseDto> daySlots(Map<String, Exercise> exercises, TemplateParams tp,
                                               TrainerPhilosophy trainer, FitnessProfile profile,
                                               String... names) {
        Set<String> canonicalNamesInDay = new HashSet<>(List.of(names));
        Set<String> usedNames = new HashSet<>();
        List<WorkoutExerciseDto> result = new ArrayList<>(names.length);
        for (String name : names) {
            result.add(slot(exercises, name, tp, trainer, profile.getPainAreas(), profile.getBarbellComfort(),
                    canonicalNamesInDay, usedNames));
        }
        return result;
    }

    /**
     * Auto-detects compound vs isolation by MovementPattern and applies trainer params
     * accordingly. Resolves which exercise actually fills this slot in priority order:
     * (1) a pain-avoidance substitute for a reported pain area — safety before style;
     * (2) the trainer's preferred substitute; (3) the literal canonical name. Barbell
     * comfort is then applied as a second-pass filter over whatever that chain already
     * resolved to — not a fourth parallel alternative — so it also catches a trainer
     * preference that *introduces* a barbell lift the template didn't call for (e.g.
     * strength-focused's "Chest Press Machine" -> "Barbell Bench Press"). Each candidate
     * is only used if it wouldn't collide with another exercise already placed in this
     * same day — either literally (the template itself lists it for another slot) or as
     * the result of an earlier slot's own substitution.
     */
    private WorkoutExerciseDto slot(Map<String, Exercise> exercises, String name, TemplateParams tp,
                                     TrainerPhilosophy trainer, Set<PainArea> painAreas,
                                     BarbellComfort barbellComfort,
                                     Set<String> canonicalNamesInDay, Set<String> usedNames) {
        String resolvedName = safeCandidate(PainAvoidancePreferences.resolve(painAreas, name),
                        name, exercises, canonicalNamesInDay, usedNames)
                .or(() -> safeCandidate(TrainerExercisePreferences.resolve(trainer.getSlug(), name),
                        name, exercises, canonicalNamesInDay, usedNames))
                .orElse(name);

        resolvedName = safeCandidate(BarbellComfortPreferences.resolve(barbellComfort, resolvedName),
                        resolvedName, exercises, canonicalNamesInDay, usedNames)
                .orElse(resolvedName);

        Exercise exercise = exercises.get(resolvedName);
        if (exercise == null) {
            throw new IllegalStateException("Exercise not found in library: " + resolvedName);
        }
        usedNames.add(resolvedName);

        boolean isolation = exercise.getMovementPattern() == MovementPattern.ISOLATION;
        return new WorkoutExerciseDto(
                UUID.randomUUID(),
                0,
                isolation ? tp.iSets()   : tp.cSets(),
                isolation ? tp.iRepMin() : tp.cRepMin(),
                isolation ? tp.iRepMax() : tp.cRepMax(),
                isolation ? tp.iRir()    : tp.cRir(),
                isolation ? tp.iRest()   : tp.cRest(),
                ExerciseDto.from(exercise)
        );
    }

    /** A candidate substitute is only safe to use if it exists, differs from the
     *  canonical name, and won't collide with another exercise already in this day. */
    private Optional<String> safeCandidate(Optional<String> candidate, String canonicalName,
                                            Map<String, Exercise> exercises,
                                            Set<String> canonicalNamesInDay, Set<String> usedNames) {
        return candidate
                .filter(exercises::containsKey)
                .filter(c -> !c.equals(canonicalName))
                .filter(c -> !canonicalNamesInDay.contains(c))
                .filter(c -> !usedNames.contains(c));
    }

    private boolean isBeginner(FitnessProfile profile) {
        return profile.getTrainingBackground() == TrainingBackground.STARTING
                || profile.getTrainingBackground() == TrainingBackground.RETURNING;
    }

    private String labelForGoal(MainGoal goal) {
        return switch (goal) {
            case STRENGTH        -> "Strength Focus";
            case FAT_LOSS        -> "Fat Loss Focus";
            case MUSCLE_GAIN     -> "Muscle Gain Focus";
            case GENERAL_FITNESS -> "General Fitness";
        };
    }
}
