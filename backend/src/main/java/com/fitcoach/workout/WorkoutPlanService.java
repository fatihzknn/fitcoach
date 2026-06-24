package com.fitcoach.workout;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.exercise.Exercise;
import com.fitcoach.exercise.ExerciseRepository;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.FitnessProfileRepository;
import com.fitcoach.workout.domain.PlanOption;
import com.fitcoach.workout.dto.PlanOptionsResponse;
import com.fitcoach.workout.dto.SelectPlanRequest;
import com.fitcoach.workout.dto.WorkoutDayDto;
import com.fitcoach.workout.dto.WorkoutExerciseDto;
import com.fitcoach.workout.dto.WorkoutPlanDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkoutPlanService {

    private final WorkoutPlanRepository planRepository;
    private final FitnessProfileRepository profileRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutGenerationService generationService;

    public WorkoutPlanService(WorkoutPlanRepository planRepository,
                               FitnessProfileRepository profileRepository,
                               ExerciseRepository exerciseRepository,
                               WorkoutGenerationService generationService) {
        this.planRepository = planRepository;
        this.profileRepository = profileRepository;
        this.exerciseRepository = exerciseRepository;
        this.generationService = generationService;
    }

    @Transactional(readOnly = true)
    public PlanOptionsResponse getPlanOptions(CurrentUser currentUser) {
        FitnessProfile profile = requireProfile(currentUser.id());
        Map<String, Exercise> byName = loadExercisesByName();
        return generationService.generateOptions(profile, byName);
    }

    @Transactional
    public WorkoutPlanDto selectPlan(CurrentUser currentUser, SelectPlanRequest request) {
        FitnessProfile profile = requireProfile(currentUser.id());

        List<Exercise> allExercises = exerciseRepository.findAll();
        Map<String, Exercise> byName = allExercises.stream()
                .collect(Collectors.toMap(Exercise::getName, e -> e));
        Map<UUID, Exercise> byId = allExercises.stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));

        PlanOptionsResponse options = generationService.generateOptions(profile, byName);
        WorkoutPlanDto chosenDto = request.option() == PlanOption.RECOMMENDED
                ? options.recommended()
                : options.alternative();

        // Deactivate any existing active plan for this user
        planRepository.findByUserIdAndIsActiveTrue(currentUser.id())
                .ifPresent(existing -> {
                    existing.deactivate();
                    planRepository.save(existing);
                });

        // Build and persist the selected plan
        WorkoutPlan plan = new WorkoutPlan(
                currentUser.id(),
                chosenDto.name(),
                chosenDto.goal(),
                chosenDto.trainingDaysPerWeek()
        );
        plan.setSustainabilityWarning(chosenDto.sustainabilityWarning());
        plan.activate();

        for (WorkoutDayDto dayDto : chosenDto.days()) {
            WorkoutDay day = new WorkoutDay(plan, dayDto.dayNumber(), dayDto.workoutName());
            int order = 1;
            for (WorkoutExerciseDto weDto : dayDto.exercises()) {
                Exercise exercise = byId.get(weDto.exercise().id());
                if (exercise == null) {
                    throw new NotFoundException("Exercise not found: " + weDto.exercise().name());
                }
                WorkoutExercise we = new WorkoutExercise(
                        day, exercise, order++,
                        weDto.sets(), weDto.repRangeMin(), weDto.repRangeMax(),
                        weDto.rirGuidance(), weDto.restSeconds()
                );
                day.addExercise(we);
            }
            plan.addDay(day);
        }

        WorkoutPlan saved = planRepository.save(plan);
        return WorkoutPlanDto.from(saved);
    }

    @Transactional(readOnly = true)
    public WorkoutPlanDto getActivePlan(CurrentUser currentUser) {
        WorkoutPlan plan = planRepository.findByUserIdAndIsActiveTrue(currentUser.id())
                .orElseThrow(() -> new NotFoundException("No active workout plan. Select a plan first."));
        return WorkoutPlanDto.from(plan);
    }

    private FitnessProfile requireProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .filter(FitnessProfile::isOnboardingCompleted)
                .orElseThrow(() -> new NotFoundException("Complete onboarding before generating a workout plan."));
    }

    private Map<String, Exercise> loadExercisesByName() {
        return exerciseRepository.findAll().stream()
                .collect(Collectors.toMap(Exercise::getName, e -> e));
    }
}
