package com.fitcoach.workout;

import com.fitcoach.exercise.Exercise;
import com.fitcoach.exercise.domain.MovementPattern;
import com.fitcoach.exercise.dto.ExerciseDto;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.TrainingBackground;
import com.fitcoach.trainer.TrainerPhilosophy;
import com.fitcoach.workout.dto.PlanOptionsResponse;
import com.fitcoach.workout.dto.WorkoutDayDto;
import com.fitcoach.workout.dto.WorkoutExerciseDto;
import com.fitcoach.workout.dto.WorkoutPlanDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic workout plan generator. Takes the user's fitness profile and a trainer
 * philosophy, and returns two plan options without writing to the database.
 *
 * The split structure (Full Body / Upper-Lower / PPL) is chosen by experience level + days.
 * Volume (sets, rep ranges, rest, RIR) comes from the trainer philosophy, with beginner
 * safety caps applied automatically.
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
                buildDay(1, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Lat Pulldown",        tp),
                        slot(ex, "Leg Press",           tp),
                        slot(ex, "Romanian Deadlift",   tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Biceps Curl",         tp)
                )),
                buildDay(2, "Full Body B", List.of(
                        slot(ex, "Dumbbell Bench Press", tp),
                        slot(ex, "Cable Row",            tp),
                        slot(ex, "Leg Press",            tp),
                        slot(ex, "Leg Curl",             tp),
                        slot(ex, "Leg Extension",        tp),
                        slot(ex, "Triceps Pushdown",     tp)
                )),
                buildDay(3, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Lat Pulldown",        tp),
                        slot(ex, "Leg Press",           tp),
                        slot(ex, "Romanian Deadlift",   tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Biceps Curl",         tp)
                ))
        );
        return plan("Full Body 3-Day (A/B)", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildBeginnerMachineFriendly(FitnessProfile p, Map<String, Exercise> ex,
                                                         String warning, TemplateParams tp,
                                                         TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Lat Pulldown",        tp),
                        slot(ex, "Leg Press",           tp),
                        slot(ex, "Leg Curl",            tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Biceps Curl",         tp)
                )),
                buildDay(2, "Full Body B", List.of(
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Cable Row",           tp),
                        slot(ex, "Leg Press",           tp),
                        slot(ex, "Leg Extension",       tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Triceps Pushdown",    tp)
                )),
                buildDay(3, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Lat Pulldown",        tp),
                        slot(ex, "Leg Press",           tp),
                        slot(ex, "Leg Curl",            tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Biceps Curl",         tp)
                ))
        );
        return plan("Machine-Friendly Full Body", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildBeginnerUpperLower(FitnessProfile p, Map<String, Exercise> ex,
                                                    String warning, TemplateParams tp,
                                                    TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Upper A", List.of(
                        slot(ex, "Dumbbell Bench Press", tp),
                        slot(ex, "Lat Pulldown",         tp),
                        slot(ex, "Overhead Press",       tp),
                        slot(ex, "Dumbbell Row",         tp),
                        slot(ex, "Biceps Curl",          tp),
                        slot(ex, "Triceps Pushdown",     tp)
                )),
                buildDay(2, "Lower A", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Leg Extension",     tp),
                        slot(ex, "Hip Thrust",        tp)
                )),
                buildDay(3, "Upper B", List.of(
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Cable Row",           tp),
                        slot(ex, "Overhead Press",      tp),
                        slot(ex, "Lat Pulldown",        tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Triceps Pushdown",    tp)
                )),
                buildDay(4, "Lower B", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Goblet Squat",      tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Hip Thrust",        tp)
                ))
        );
        return plan("Upper / Lower Split", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildBeginnerFullBodyX4(FitnessProfile p, Map<String, Exercise> ex,
                                                    String warning, TemplateParams tp,
                                                    TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine",  tp),
                        slot(ex, "Lat Pulldown",         tp),
                        slot(ex, "Leg Press",            tp),
                        slot(ex, "Romanian Deadlift",    tp),
                        slot(ex, "Lateral Raise",        tp)
                )),
                buildDay(2, "Full Body B", List.of(
                        slot(ex, "Dumbbell Bench Press", tp),
                        slot(ex, "Cable Row",            tp),
                        slot(ex, "Leg Press",            tp),
                        slot(ex, "Leg Curl",             tp),
                        slot(ex, "Biceps Curl",          tp)
                )),
                buildDay(3, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine",  tp),
                        slot(ex, "Lat Pulldown",         tp),
                        slot(ex, "Leg Press",            tp),
                        slot(ex, "Romanian Deadlift",    tp),
                        slot(ex, "Lateral Raise",        tp)
                )),
                buildDay(4, "Full Body B", List.of(
                        slot(ex, "Dumbbell Bench Press", tp),
                        slot(ex, "Cable Row",            tp),
                        slot(ex, "Leg Press",            tp),
                        slot(ex, "Leg Curl",             tp),
                        slot(ex, "Biceps Curl",          tp)
                ))
        );
        return plan("Full Body 4-Day (A/B/A/B)", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildBeginnerUpperLowerPlusArms(FitnessProfile p, Map<String, Exercise> ex,
                                                            String warning, TemplateParams tp,
                                                            TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Upper", List.of(
                        slot(ex, "Dumbbell Bench Press", tp),
                        slot(ex, "Lat Pulldown",         tp),
                        slot(ex, "Overhead Press",       tp),
                        slot(ex, "Cable Row",            tp),
                        slot(ex, "Lateral Raise",        tp)
                )),
                buildDay(2, "Lower", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Leg Extension",     tp),
                        slot(ex, "Hip Thrust",        tp)
                )),
                buildDay(3, "Upper", List.of(
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Dumbbell Row",        tp),
                        slot(ex, "Overhead Press",      tp),
                        slot(ex, "Lat Pulldown",        tp),
                        slot(ex, "Lateral Raise",       tp)
                )),
                buildDay(4, "Lower", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Goblet Squat",      tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Hip Thrust",        tp)
                )),
                buildDay(5, "Arms & Shoulders", List.of(
                        slot(ex, "Biceps Curl",      tp),
                        slot(ex, "Triceps Pushdown", tp),
                        slot(ex, "Lateral Raise",    tp),
                        slot(ex, "Overhead Press",   tp)
                ))
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
                buildDay(1, "Full Body A", List.of(
                        slot(ex, "Barbell Bench Press", tp),
                        slot(ex, "Lat Pulldown",        tp),
                        slot(ex, "Leg Press",           tp),
                        slot(ex, "Romanian Deadlift",   tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Biceps Curl",         tp)
                )),
                buildDay(2, "Full Body B", List.of(
                        slot(ex, "Dumbbell Bench Press", tp),
                        slot(ex, "Cable Row",            tp),
                        slot(ex, "Leg Press",            tp),
                        slot(ex, "Hip Thrust",           tp),
                        slot(ex, "Overhead Press",       tp),
                        slot(ex, "Triceps Pushdown",     tp)
                )),
                buildDay(3, "Full Body C", List.of(
                        slot(ex, "Barbell Bench Press", tp),
                        slot(ex, "Dumbbell Row",        tp),
                        slot(ex, "Goblet Squat",        tp),
                        slot(ex, "Romanian Deadlift",   tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Biceps Curl",         tp)
                ))
        );
        return plan("Full Body 3-Day (Intermediate)", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildPPL(FitnessProfile p, Map<String, Exercise> ex,
                                     String warning, TemplateParams tp,
                                     TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Push", List.of(
                        slot(ex, "Barbell Bench Press", tp),
                        slot(ex, "Overhead Press",      tp),
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Triceps Pushdown",    tp)
                )),
                buildDay(2, "Pull", List.of(
                        slot(ex, "Lat Pulldown",    tp),
                        slot(ex, "Cable Row",       tp),
                        slot(ex, "Dumbbell Row",    tp),
                        slot(ex, "Biceps Curl",     tp),
                        slot(ex, "Assisted Pull-up", tp)
                )),
                buildDay(3, "Legs", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Leg Extension",     tp),
                        slot(ex, "Hip Thrust",        tp)
                ))
        );
        return plan("Push / Pull / Legs", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildRegularUpperLower(FitnessProfile p, Map<String, Exercise> ex,
                                                   String warning, TemplateParams tp,
                                                   TrainerPhilosophy trainer) {
        String name = "Upper / Lower (" + labelForGoal(p.getMainGoal()) + ")";
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Upper A", List.of(
                        slot(ex, "Barbell Bench Press", tp),
                        slot(ex, "Lat Pulldown",        tp),
                        slot(ex, "Overhead Press",      tp),
                        slot(ex, "Cable Row",           tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Biceps Curl",         tp)
                )),
                buildDay(2, "Lower A", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Leg Extension",     tp),
                        slot(ex, "Hip Thrust",        tp)
                )),
                buildDay(3, "Upper B", List.of(
                        slot(ex, "Dumbbell Bench Press", tp),
                        slot(ex, "Dumbbell Row",         tp),
                        slot(ex, "Overhead Press",       tp),
                        slot(ex, "Lat Pulldown",         tp),
                        slot(ex, "Lateral Raise",        tp),
                        slot(ex, "Triceps Pushdown",     tp)
                )),
                buildDay(4, "Lower B", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Goblet Squat",      tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Hip Thrust",        tp)
                ))
        );
        return plan(name, p, warning, days, trainer);
    }

    private WorkoutPlanDto buildPPLPartial(FitnessProfile p, Map<String, Exercise> ex,
                                            String warning, boolean isRegular,
                                            TemplateParams tp, TrainerPhilosophy trainer) {
        String name = isRegular ? "Push / Pull / Legs (4-Day)" : "PPL + Upper Intro";
        List<WorkoutDayDto> days = new ArrayList<>();
        days.add(buildDay(1, "Push", List.of(
                slot(ex, "Barbell Bench Press", tp),
                slot(ex, "Overhead Press",      tp),
                slot(ex, "Lateral Raise",       tp),
                slot(ex, "Triceps Pushdown",    tp)
        )));
        days.add(buildDay(2, "Pull", List.of(
                slot(ex, "Lat Pulldown",  tp),
                slot(ex, "Cable Row",     tp),
                slot(ex, "Dumbbell Row",  tp),
                slot(ex, "Biceps Curl",   tp)
        )));
        days.add(buildDay(3, "Legs", List.of(
                slot(ex, "Leg Press",         tp),
                slot(ex, "Romanian Deadlift", tp),
                slot(ex, "Leg Curl",          tp),
                slot(ex, "Hip Thrust",        tp)
        )));
        days.add(buildDay(4, "Upper", List.of(
                slot(ex, "Dumbbell Bench Press", tp),
                slot(ex, "Dumbbell Row",         tp),
                slot(ex, "Overhead Press",       tp),
                slot(ex, "Lateral Raise",        tp),
                slot(ex, "Biceps Curl",          tp)
        )));
        return plan(name, p, warning, days, trainer);
    }

    private WorkoutPlanDto buildPPLPlusUpperLower(FitnessProfile p, Map<String, Exercise> ex,
                                                   String warning, TemplateParams tp,
                                                   TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Push", List.of(
                        slot(ex, "Barbell Bench Press", tp),
                        slot(ex, "Overhead Press",      tp),
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Triceps Pushdown",    tp)
                )),
                buildDay(2, "Pull", List.of(
                        slot(ex, "Lat Pulldown",    tp),
                        slot(ex, "Cable Row",       tp),
                        slot(ex, "Dumbbell Row",    tp),
                        slot(ex, "Assisted Pull-up", tp),
                        slot(ex, "Biceps Curl",     tp)
                )),
                buildDay(3, "Legs", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Leg Extension",     tp),
                        slot(ex, "Hip Thrust",        tp)
                )),
                buildDay(4, "Upper", List.of(
                        slot(ex, "Dumbbell Bench Press", tp),
                        slot(ex, "Lat Pulldown",         tp),
                        slot(ex, "Overhead Press",       tp),
                        slot(ex, "Dumbbell Row",         tp),
                        slot(ex, "Lateral Raise",        tp)
                )),
                buildDay(5, "Lower", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Goblet Squat",      tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Hip Thrust",        tp)
                ))
        );
        return plan("PPL + Upper / Lower (5-Day)", p, warning, days, trainer);
    }

    private WorkoutPlanDto buildRegularUpperLowerX5(FitnessProfile p, Map<String, Exercise> ex,
                                                     String warning, TemplateParams tp,
                                                     TrainerPhilosophy trainer) {
        List<WorkoutDayDto> days = List.of(
                buildDay(1, "Upper A", List.of(
                        slot(ex, "Barbell Bench Press", tp),
                        slot(ex, "Lat Pulldown",        tp),
                        slot(ex, "Overhead Press",      tp),
                        slot(ex, "Cable Row",           tp),
                        slot(ex, "Lateral Raise",       tp)
                )),
                buildDay(2, "Lower A", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Leg Curl",          tp),
                        slot(ex, "Hip Thrust",        tp)
                )),
                buildDay(3, "Upper B", List.of(
                        slot(ex, "Dumbbell Bench Press", tp),
                        slot(ex, "Dumbbell Row",         tp),
                        slot(ex, "Overhead Press",       tp),
                        slot(ex, "Lat Pulldown",         tp),
                        slot(ex, "Triceps Pushdown",     tp)
                )),
                buildDay(4, "Lower B", List.of(
                        slot(ex, "Leg Press",         tp),
                        slot(ex, "Romanian Deadlift", tp),
                        slot(ex, "Goblet Squat",      tp),
                        slot(ex, "Leg Extension",     tp),
                        slot(ex, "Hip Thrust",        tp)
                )),
                buildDay(5, "Upper C", List.of(
                        slot(ex, "Chest Press Machine", tp),
                        slot(ex, "Cable Row",           tp),
                        slot(ex, "Lateral Raise",       tp),
                        slot(ex, "Biceps Curl",         tp),
                        slot(ex, "Triceps Pushdown",    tp)
                ))
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
                trainer.getDisplayName()
        );
    }

    private WorkoutDayDto buildDay(int dayNumber, String workoutName, List<WorkoutExerciseDto> exercises) {
        return new WorkoutDayDto(UUID.randomUUID(), dayNumber, workoutName, exercises);
    }

    /** Auto-detects compound vs isolation by MovementPattern and applies trainer params accordingly. */
    private WorkoutExerciseDto slot(Map<String, Exercise> exercises, String name, TemplateParams tp) {
        Exercise exercise = exercises.get(name);
        if (exercise == null) {
            throw new IllegalStateException("Exercise not found in library: " + name);
        }
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
