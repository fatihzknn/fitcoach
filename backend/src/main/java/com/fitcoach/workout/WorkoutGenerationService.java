package com.fitcoach.workout;

import com.fitcoach.exercise.Exercise;
import com.fitcoach.exercise.dto.ExerciseDto;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.TrainingBackground;
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
 * Deterministic workout plan generator. Receives the user's fitness profile and
 * a pre-loaded exercise map, and returns two plan options (recommended + alternative)
 * without writing to the database.
 */
@Service
public class WorkoutGenerationService {

    private static final String SUSTAINABILITY_WARNING =
            "5 sessions per week is demanding. Ensure 7–8 hours of sleep and listen to your body.";

    public PlanOptionsResponse generateOptions(FitnessProfile profile, Map<String, Exercise> exercises) {
        boolean isBeginner = profile.getTrainingBackground() == TrainingBackground.STARTING
                          || profile.getTrainingBackground() == TrainingBackground.RETURNING;
        int days = profile.getTrainingDaysPerWeek();
        MainGoal goal = profile.getMainGoal();

        WorkoutPlanDto recommended;
        WorkoutPlanDto alternative;

        if (isBeginner) {
            switch (days) {
                case 3 -> {
                    recommended = buildBeginnerFullBodyAB(profile, exercises, null);
                    alternative = buildBeginnerMachineFriendly(profile, exercises, null);
                }
                case 4 -> {
                    recommended = buildBeginnerUpperLower(profile, exercises, null);
                    alternative = buildBeginnerFullBodyX4(profile, exercises, null);
                }
                default -> { // 5 days
                    recommended = buildBeginnerUpperLowerPlusArms(profile, exercises, SUSTAINABILITY_WARNING);
                    alternative = buildPPLPartial(profile, exercises, SUSTAINABILITY_WARNING, false);
                }
            }
        } else { // REGULAR
            switch (days) {
                case 3 -> {
                    recommended = buildRegularFullBody(profile, exercises, null);
                    alternative = buildPPL(profile, exercises, null);
                }
                case 4 -> {
                    recommended = buildRegularUpperLower(profile, exercises, null);
                    alternative = buildPPLPartial(profile, exercises, null, true);
                }
                default -> { // 5 days
                    recommended = buildPPLPlusUpperLower(profile, exercises, SUSTAINABILITY_WARNING);
                    alternative = buildRegularUpperLowerX5(profile, exercises, SUSTAINABILITY_WARNING);
                }
            }
        }

        return new PlanOptionsResponse(recommended, alternative);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Beginner templates
    // ─────────────────────────────────────────────────────────────────────────

    private WorkoutPlanDto buildBeginnerFullBodyAB(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        String name = "Full Body 3-Day (A/B)";
        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine",  3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Lat Pulldown",         3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Leg Press",            3, 12, 15, "3 RIR",   90),
                        slot(ex, "Romanian Deadlift",    3, 10, 12, "3 RIR",   60),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR",   45),
                        slot(ex, "Biceps Curl",          2, 12, 15, "3 RIR",   45)
                )),
                buildDay(null, 2, "Full Body B", List.of(
                        slot(ex, "Dumbbell Bench Press", 3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Cable Row",            3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Leg Press",            3, 12, 15, "3 RIR",   90),
                        slot(ex, "Leg Curl",             3, 12, 15, "3 RIR",   45),
                        slot(ex, "Leg Extension",        3, 12, 15, "3 RIR",   45),
                        slot(ex, "Triceps Pushdown",     2, 12, 15, "3 RIR",   45)
                )),
                buildDay(null, 3, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine",  3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Lat Pulldown",         3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Leg Press",            3, 12, 15, "3 RIR",   90),
                        slot(ex, "Romanian Deadlift",    3, 10, 12, "3 RIR",   60),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR",   45),
                        slot(ex, "Biceps Curl",          2, 12, 15, "3 RIR",   45)
                ))
        );
        return plan(name, p, warning, days);
    }

    private WorkoutPlanDto buildBeginnerMachineFriendly(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        String name = "Machine-Friendly Full Body";
        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine",  3, 12, 15, "3 RIR", 75),
                        slot(ex, "Lat Pulldown",         3, 12, 15, "3 RIR", 60),
                        slot(ex, "Leg Press",            3, 15, 20, "3 RIR", 90),
                        slot(ex, "Leg Curl",             3, 12, 15, "3 RIR", 45),
                        slot(ex, "Lateral Raise",        3, 15, 20, "3 RIR", 45),
                        slot(ex, "Biceps Curl",          2, 12, 15, "3 RIR", 45)
                )),
                buildDay(null, 2, "Full Body B", List.of(
                        slot(ex, "Chest Press Machine",  3, 12, 15, "3 RIR", 75),
                        slot(ex, "Cable Row",            3, 12, 15, "3 RIR", 60),
                        slot(ex, "Leg Press",            3, 15, 20, "3 RIR", 90),
                        slot(ex, "Leg Extension",        3, 12, 15, "3 RIR", 45),
                        slot(ex, "Lateral Raise",        3, 15, 20, "3 RIR", 45),
                        slot(ex, "Triceps Pushdown",     2, 12, 15, "3 RIR", 45)
                )),
                buildDay(null, 3, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine",  3, 12, 15, "3 RIR", 75),
                        slot(ex, "Lat Pulldown",         3, 12, 15, "3 RIR", 60),
                        slot(ex, "Leg Press",            3, 15, 20, "3 RIR", 90),
                        slot(ex, "Leg Curl",             3, 12, 15, "3 RIR", 45),
                        slot(ex, "Lateral Raise",        3, 15, 20, "3 RIR", 45),
                        slot(ex, "Biceps Curl",          2, 12, 15, "3 RIR", 45)
                ))
        );
        return plan(name, p, warning, days);
    }

    private WorkoutPlanDto buildBeginnerUpperLower(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        String name = "Upper / Lower Split";
        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Upper A", List.of(
                        slot(ex, "Dumbbell Bench Press", 3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Lat Pulldown",         3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Overhead Press",       3, 10, 12, "2-3 RIR", 75),
                        slot(ex, "Dumbbell Row",         3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Biceps Curl",          2, 12, 15, "3 RIR",   45),
                        slot(ex, "Triceps Pushdown",     2, 12, 15, "3 RIR",   45)
                )),
                buildDay(null, 2, "Lower A", List.of(
                        slot(ex, "Leg Press",         3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Romanian Deadlift", 3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Leg Curl",          3, 12, 15, "3 RIR",   45),
                        slot(ex, "Leg Extension",     3, 12, 15, "3 RIR",   45),
                        slot(ex, "Hip Thrust",        3, 12, 15, "3 RIR",   60)
                )),
                buildDay(null, 3, "Upper B", List.of(
                        slot(ex, "Chest Press Machine", 3, 10, 12, "2-3 RIR", 75),
                        slot(ex, "Cable Row",           3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Overhead Press",      3, 10, 12, "2-3 RIR", 75),
                        slot(ex, "Lat Pulldown",        3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Lateral Raise",       3, 12, 15, "3 RIR",   45),
                        slot(ex, "Triceps Pushdown",    2, 12, 15, "3 RIR",   45)
                )),
                buildDay(null, 4, "Lower B", List.of(
                        slot(ex, "Leg Press",         3, 12, 15, "3 RIR",   90),
                        slot(ex, "Romanian Deadlift", 3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Goblet Squat",      3, 12, 15, "3 RIR",   75),
                        slot(ex, "Leg Curl",          3, 12, 15, "3 RIR",   45),
                        slot(ex, "Hip Thrust",        3, 12, 15, "3 RIR",   60)
                ))
        );
        return plan(name, p, warning, days);
    }

    private WorkoutPlanDto buildBeginnerFullBodyX4(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        String name = "Full Body 4-Day (A/B/A/B)";
        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine",  3, 12, 15, "3 RIR", 75),
                        slot(ex, "Lat Pulldown",         3, 12, 15, "3 RIR", 60),
                        slot(ex, "Leg Press",            3, 12, 15, "3 RIR", 90),
                        slot(ex, "Romanian Deadlift",    3, 10, 12, "3 RIR", 75),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR", 45)
                )),
                buildDay(null, 2, "Full Body B", List.of(
                        slot(ex, "Dumbbell Bench Press", 3, 12, 15, "3 RIR", 75),
                        slot(ex, "Cable Row",            3, 12, 15, "3 RIR", 60),
                        slot(ex, "Leg Press",            3, 12, 15, "3 RIR", 90),
                        slot(ex, "Leg Curl",             3, 12, 15, "3 RIR", 45),
                        slot(ex, "Biceps Curl",          2, 12, 15, "3 RIR", 45)
                )),
                buildDay(null, 3, "Full Body A", List.of(
                        slot(ex, "Chest Press Machine",  3, 12, 15, "3 RIR", 75),
                        slot(ex, "Lat Pulldown",         3, 12, 15, "3 RIR", 60),
                        slot(ex, "Leg Press",            3, 12, 15, "3 RIR", 90),
                        slot(ex, "Romanian Deadlift",    3, 10, 12, "3 RIR", 75),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR", 45)
                )),
                buildDay(null, 4, "Full Body B", List.of(
                        slot(ex, "Dumbbell Bench Press", 3, 12, 15, "3 RIR", 75),
                        slot(ex, "Cable Row",            3, 12, 15, "3 RIR", 60),
                        slot(ex, "Leg Press",            3, 12, 15, "3 RIR", 90),
                        slot(ex, "Leg Curl",             3, 12, 15, "3 RIR", 45),
                        slot(ex, "Biceps Curl",          2, 12, 15, "3 RIR", 45)
                ))
        );
        return plan(name, p, warning, days);
    }

    private WorkoutPlanDto buildBeginnerUpperLowerPlusArms(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        String name = "Upper / Lower + Arms Focus";
        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Upper", List.of(
                        slot(ex, "Dumbbell Bench Press", 3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Lat Pulldown",         3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Overhead Press",       3, 10, 12, "2-3 RIR", 75),
                        slot(ex, "Cable Row",            3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR",   45)
                )),
                buildDay(null, 2, "Lower", List.of(
                        slot(ex, "Leg Press",         3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Romanian Deadlift", 3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Leg Curl",          3, 12, 15, "3 RIR",   45),
                        slot(ex, "Leg Extension",     3, 12, 15, "3 RIR",   45),
                        slot(ex, "Hip Thrust",        3, 12, 15, "3 RIR",   60)
                )),
                buildDay(null, 3, "Upper", List.of(
                        slot(ex, "Chest Press Machine", 3, 10, 12, "2-3 RIR", 75),
                        slot(ex, "Dumbbell Row",        3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Overhead Press",      3, 10, 12, "2-3 RIR", 75),
                        slot(ex, "Lat Pulldown",        3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Lateral Raise",       3, 12, 15, "3 RIR",   45)
                )),
                buildDay(null, 4, "Lower", List.of(
                        slot(ex, "Leg Press",         3, 12, 15, "3 RIR",   90),
                        slot(ex, "Romanian Deadlift", 3, 10, 12, "2-3 RIR", 90),
                        slot(ex, "Goblet Squat",      3, 12, 15, "3 RIR",   75),
                        slot(ex, "Leg Curl",          3, 12, 15, "3 RIR",   45),
                        slot(ex, "Hip Thrust",        3, 12, 15, "3 RIR",   60)
                )),
                buildDay(null, 5, "Arms & Shoulders", List.of(
                        slot(ex, "Biceps Curl",      3, 12, 15, "3 RIR", 45),
                        slot(ex, "Triceps Pushdown", 3, 12, 15, "3 RIR", 45),
                        slot(ex, "Lateral Raise",    4, 12, 15, "3 RIR", 45),
                        slot(ex, "Overhead Press",   3, 10, 12, "3 RIR", 60),
                        slot(ex, "Biceps Curl",      2, 15, 20, "4 RIR", 30)
                ))
        );
        return plan(name, p, warning, days);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Regular templates
    // ─────────────────────────────────────────────────────────────────────────

    private WorkoutPlanDto buildRegularFullBody(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        String name = "Full Body 3-Day (Intermediate)";
        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Full Body A", List.of(
                        slot(ex, "Barbell Bench Press",  4, 8, 10, "2 RIR", 120),
                        slot(ex, "Lat Pulldown",         4, 8, 10, "2 RIR",  90),
                        slot(ex, "Leg Press",            4, 8, 10, "2 RIR", 120),
                        slot(ex, "Romanian Deadlift",    3, 8, 10, "2 RIR",  90),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR",  45),
                        slot(ex, "Biceps Curl",          3, 10, 12, "2 RIR",  45)
                )),
                buildDay(null, 2, "Full Body B", List.of(
                        slot(ex, "Dumbbell Bench Press", 4, 8, 10, "2 RIR", 90),
                        slot(ex, "Cable Row",            4, 8, 10, "2 RIR", 75),
                        slot(ex, "Leg Press",            4, 8, 10, "2 RIR", 120),
                        slot(ex, "Hip Thrust",           4, 10, 12, "2 RIR", 75),
                        slot(ex, "Overhead Press",       3, 8, 10, "2 RIR",  90),
                        slot(ex, "Triceps Pushdown",     3, 10, 12, "2 RIR",  45)
                )),
                buildDay(null, 3, "Full Body C", List.of(
                        slot(ex, "Barbell Bench Press",  4, 8, 10, "2 RIR", 120),
                        slot(ex, "Dumbbell Row",         4, 8, 10, "2 RIR",  75),
                        slot(ex, "Goblet Squat",         4, 10, 12, "2 RIR", 90),
                        slot(ex, "Romanian Deadlift",    3, 8, 10, "2 RIR",  90),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR",  45),
                        slot(ex, "Biceps Curl",          2, 10, 12, "2 RIR",  45)
                ))
        );
        return plan(name, p, warning, days);
    }

    private WorkoutPlanDto buildPPL(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        String name = "Push / Pull / Legs";
        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Push", List.of(
                        slot(ex, "Barbell Bench Press", 4, 6, 8,  "2 RIR", 120),
                        slot(ex, "Overhead Press",      3, 8, 10, "2 RIR",  90),
                        slot(ex, "Chest Press Machine", 3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Lateral Raise",       4, 12, 15, "3 RIR",  45),
                        slot(ex, "Triceps Pushdown",    3, 12, 15, "3 RIR",  45)
                )),
                buildDay(null, 2, "Pull", List.of(
                        slot(ex, "Lat Pulldown",    4, 8, 10,  "2 RIR",  90),
                        slot(ex, "Cable Row",       3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Dumbbell Row",    3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Biceps Curl",     4, 10, 12, "2-3 RIR", 45),
                        slot(ex, "Assisted Pull-up",2, 6, 10, "2 RIR",   90)
                )),
                buildDay(null, 3, "Legs", List.of(
                        slot(ex, "Leg Press",         4, 8, 10,  "2 RIR", 120),
                        slot(ex, "Romanian Deadlift", 3, 8, 10,  "2 RIR",  90),
                        slot(ex, "Leg Curl",          3, 10, 12, "2-3 RIR", 45),
                        slot(ex, "Leg Extension",     3, 10, 12, "2-3 RIR", 45),
                        slot(ex, "Hip Thrust",        3, 10, 12, "2-3 RIR", 60)
                ))
        );
        return plan(name, p, warning, days);
    }

    private WorkoutPlanDto buildRegularUpperLower(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        int[] repRange = repRangeForGoal(p.getMainGoal());
        int rest = restForGoal(p.getMainGoal());
        String name = "Upper / Lower (" + labelForGoal(p.getMainGoal()) + ")";

        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Upper A", List.of(
                        slot(ex, "Barbell Bench Press",  4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Lat Pulldown",         4, repRange[0], repRange[1], "2 RIR", rest - 15),
                        slot(ex, "Overhead Press",       3, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Cable Row",            3, repRange[0], repRange[1], "2 RIR", rest - 15),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR", 45),
                        slot(ex, "Biceps Curl",          3, 10, 12, "2 RIR", 45)
                )),
                buildDay(null, 2, "Lower A", List.of(
                        slot(ex, "Leg Press",         4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Romanian Deadlift", 4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Leg Curl",          3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Leg Extension",     3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Hip Thrust",        3, 10, 12, "2-3 RIR", 60)
                )),
                buildDay(null, 3, "Upper B", List.of(
                        slot(ex, "Dumbbell Bench Press", 4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Dumbbell Row",         4, repRange[0], repRange[1], "2 RIR", rest - 15),
                        slot(ex, "Overhead Press",       3, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Lat Pulldown",         3, repRange[0], repRange[1], "2 RIR", rest - 15),
                        slot(ex, "Lateral Raise",        4, 12, 15, "3 RIR", 45),
                        slot(ex, "Triceps Pushdown",     3, 10, 12, "2 RIR", 45)
                )),
                buildDay(null, 4, "Lower B", List.of(
                        slot(ex, "Leg Press",         4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Romanian Deadlift", 3, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Goblet Squat",      3, 10, 12, "2-3 RIR", 75),
                        slot(ex, "Leg Curl",          3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Hip Thrust",        4, 10, 12, "2-3 RIR", 60)
                ))
        );
        return plan(name, p, warning, days);
    }

    private WorkoutPlanDto buildPPLPartial(FitnessProfile p, Map<String, Exercise> ex, String warning, boolean isRegular) {
        String name = isRegular ? "Push / Pull / Legs (4-Day)" : "PPL + Upper Intro";
        List<WorkoutDayDto> days = new ArrayList<>();

        days.add(buildDay(null, 1, "Push", List.of(
                slot(ex, "Barbell Bench Press", 4, 6, 10, "2 RIR", 120),
                slot(ex, "Overhead Press",      3, 8, 10, "2 RIR",  90),
                slot(ex, "Lateral Raise",       3, 12, 15, "3 RIR", 45),
                slot(ex, "Triceps Pushdown",    3, 10, 12, "3 RIR", 45)
        )));
        days.add(buildDay(null, 2, "Pull", List.of(
                slot(ex, "Lat Pulldown",  4, 8, 10,  "2 RIR",  90),
                slot(ex, "Cable Row",     3, 10, 12, "2-3 RIR", 60),
                slot(ex, "Dumbbell Row",  3, 10, 12, "2-3 RIR", 60),
                slot(ex, "Biceps Curl",   3, 10, 12, "2-3 RIR", 45)
        )));
        days.add(buildDay(null, 3, "Legs", List.of(
                slot(ex, "Leg Press",         4, 8, 10,  "2 RIR", 120),
                slot(ex, "Romanian Deadlift", 3, 8, 10,  "2 RIR",  90),
                slot(ex, "Leg Curl",          3, 10, 12, "3 RIR",  45),
                slot(ex, "Hip Thrust",        3, 10, 12, "3 RIR",  60)
        )));
        days.add(buildDay(null, 4, "Upper", List.of(
                slot(ex, "Dumbbell Bench Press", 3, 10, 12, "2-3 RIR", 75),
                slot(ex, "Dumbbell Row",         3, 10, 12, "2-3 RIR", 60),
                slot(ex, "Overhead Press",       3, 10, 12, "2-3 RIR", 75),
                slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR",   45),
                slot(ex, "Biceps Curl",          2, 12, 15, "3 RIR",   45)
        )));

        return plan(name, p, warning, days);
    }

    private WorkoutPlanDto buildPPLPlusUpperLower(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        String name = "PPL + Upper / Lower (5-Day)";
        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Push", List.of(
                        slot(ex, "Barbell Bench Press", 4, 6, 8,  "2 RIR", 120),
                        slot(ex, "Overhead Press",      3, 8, 10, "2 RIR",  90),
                        slot(ex, "Chest Press Machine", 3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Lateral Raise",       4, 12, 15, "3 RIR",  45),
                        slot(ex, "Triceps Pushdown",    3, 10, 12, "3 RIR",  45)
                )),
                buildDay(null, 2, "Pull", List.of(
                        slot(ex, "Lat Pulldown",    4, 6, 10,  "2 RIR",  90),
                        slot(ex, "Cable Row",       3, 8, 10,  "2 RIR",  75),
                        slot(ex, "Dumbbell Row",    3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Assisted Pull-up",2, 5, 10, "2 RIR",   90),
                        slot(ex, "Biceps Curl",     4, 10, 12, "2-3 RIR", 45)
                )),
                buildDay(null, 3, "Legs", List.of(
                        slot(ex, "Leg Press",         4, 8, 10,  "2 RIR", 120),
                        slot(ex, "Romanian Deadlift", 4, 8, 10,  "2 RIR",  90),
                        slot(ex, "Leg Curl",          3, 10, 12, "2-3 RIR", 45),
                        slot(ex, "Leg Extension",     3, 10, 12, "2-3 RIR", 45),
                        slot(ex, "Hip Thrust",        3, 10, 12, "2-3 RIR", 60)
                )),
                buildDay(null, 4, "Upper", List.of(
                        slot(ex, "Dumbbell Bench Press", 4, 8, 10,  "2 RIR",  90),
                        slot(ex, "Lat Pulldown",         4, 8, 10,  "2 RIR",  75),
                        slot(ex, "Overhead Press",       3, 8, 10,  "2 RIR",  90),
                        slot(ex, "Dumbbell Row",         3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR",   45)
                )),
                buildDay(null, 5, "Lower", List.of(
                        slot(ex, "Leg Press",         4, 8, 10,  "2 RIR", 120),
                        slot(ex, "Romanian Deadlift", 3, 8, 10,  "2 RIR",  90),
                        slot(ex, "Goblet Squat",      3, 10, 12, "2-3 RIR", 75),
                        slot(ex, "Leg Curl",          3, 10, 12, "2-3 RIR", 45),
                        slot(ex, "Hip Thrust",        4, 10, 12, "2-3 RIR", 60)
                ))
        );
        return plan(name, p, warning, days);
    }

    private WorkoutPlanDto buildRegularUpperLowerX5(FitnessProfile p, Map<String, Exercise> ex, String warning) {
        int[] repRange = repRangeForGoal(p.getMainGoal());
        int rest = restForGoal(p.getMainGoal());
        String name = "Upper / Lower × 5 (High Frequency)";

        List<WorkoutDayDto> days = List.of(
                buildDay(null, 1, "Upper A", List.of(
                        slot(ex, "Barbell Bench Press",  4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Lat Pulldown",         4, repRange[0], repRange[1], "2 RIR", rest - 15),
                        slot(ex, "Overhead Press",       3, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Cable Row",            3, repRange[0], repRange[1], "2 RIR", rest - 15),
                        slot(ex, "Lateral Raise",        3, 12, 15, "3 RIR", 45)
                )),
                buildDay(null, 2, "Lower A", List.of(
                        slot(ex, "Leg Press",         4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Romanian Deadlift", 4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Leg Curl",          3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Hip Thrust",        3, 10, 12, "2-3 RIR", 60)
                )),
                buildDay(null, 3, "Upper B", List.of(
                        slot(ex, "Dumbbell Bench Press", 4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Dumbbell Row",         4, repRange[0], repRange[1], "2 RIR", rest - 15),
                        slot(ex, "Overhead Press",       3, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Lat Pulldown",         3, repRange[0], repRange[1], "2 RIR", rest - 15),
                        slot(ex, "Triceps Pushdown",     3, 10, 12, "2 RIR", 45)
                )),
                buildDay(null, 4, "Lower B", List.of(
                        slot(ex, "Leg Press",         4, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Romanian Deadlift", 3, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Goblet Squat",      3, 10, 12, "2-3 RIR", 75),
                        slot(ex, "Leg Extension",     3, 10, 12, "2-3 RIR", 60),
                        slot(ex, "Hip Thrust",        4, 10, 12, "2-3 RIR", 60)
                )),
                buildDay(null, 5, "Upper C", List.of(
                        slot(ex, "Chest Press Machine", 3, repRange[0], repRange[1], "2 RIR", rest),
                        slot(ex, "Cable Row",           3, repRange[0], repRange[1], "2 RIR", rest - 15),
                        slot(ex, "Lateral Raise",       4, 12, 15, "3 RIR", 45),
                        slot(ex, "Biceps Curl",         3, 10, 12, "2 RIR", 45),
                        slot(ex, "Triceps Pushdown",    3, 10, 12, "2 RIR", 45)
                ))
        );
        return plan(name, p, warning, days);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private WorkoutPlanDto plan(String name, FitnessProfile p, String warning, List<WorkoutDayDto> days) {
        return new WorkoutPlanDto(
                UUID.randomUUID(), // preview id — replaced with DB id on save
                name,
                p.getMainGoal(),
                p.getTrainingDaysPerWeek(),
                false,
                warning,
                days
        );
    }

    private WorkoutDayDto buildDay(UUID planId, int dayNumber, String workoutName,
                                   List<WorkoutExerciseDto> exercises) {
        return new WorkoutDayDto(UUID.randomUUID(), dayNumber, workoutName, exercises);
    }

    private WorkoutExerciseDto slot(Map<String, Exercise> exercises, String name,
                                    int sets, int repMin, int repMax,
                                    String rirGuidance, int restSeconds) {
        Exercise exercise = exercises.get(name);
        if (exercise == null) {
            throw new IllegalStateException("Exercise not found in library: " + name);
        }
        return new WorkoutExerciseDto(
                UUID.randomUUID(), // preview id
                0, // orderIndex set when building plan — assigned by position in list
                sets, repMin, repMax, rirGuidance, restSeconds,
                ExerciseDto.from(exercise)
        );
    }

    private int[] repRangeForGoal(MainGoal goal) {
        return switch (goal) {
            case STRENGTH  -> new int[]{5, 8};
            case FAT_LOSS  -> new int[]{12, 15};
            default        -> new int[]{8, 12}; // MUSCLE_GAIN, GENERAL_FITNESS
        };
    }

    private int restForGoal(MainGoal goal) {
        return switch (goal) {
            case STRENGTH  -> 180;
            case FAT_LOSS  -> 60;
            default        -> 90;
        };
    }

    private String labelForGoal(MainGoal goal) {
        return switch (goal) {
            case STRENGTH       -> "Strength Focus";
            case FAT_LOSS       -> "Fat Loss Focus";
            case MUSCLE_GAIN    -> "Muscle Gain Focus";
            case GENERAL_FITNESS -> "General Fitness";
        };
    }
}
