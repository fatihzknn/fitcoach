package com.fitcoach.workout;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.exercise.Exercise;
import com.fitcoach.exercise.ExerciseRepository;
import com.fitcoach.profile.FitnessProfile;
import com.fitcoach.profile.FitnessProfileRepository;
import com.fitcoach.trainer.TrainerPhilosophy;
import com.fitcoach.trainer.TrainerPhilosophyRepository;
import com.fitcoach.trainer.TrainerVisibility;
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
    private final TrainerPhilosophyRepository trainerRepository;

    public WorkoutPlanService(WorkoutPlanRepository planRepository,
                               FitnessProfileRepository profileRepository,
                               ExerciseRepository exerciseRepository,
                               WorkoutGenerationService generationService,
                               TrainerPhilosophyRepository trainerRepository) {
        this.planRepository = planRepository;
        this.profileRepository = profileRepository;
        this.exerciseRepository = exerciseRepository;
        this.generationService = generationService;
        this.trainerRepository = trainerRepository;
    }

    @Transactional(readOnly = true)
    public PlanOptionsResponse getPlanOptions(CurrentUser currentUser, UUID trainerId) {
        FitnessProfile profile = requireProfile(currentUser.id());
        Map<String, Exercise> byName = loadExercisesByName();
        TrainerPhilosophy trainer = resolveTrainer(trainerId, profile);
        return generationService.generateOptions(profile, byName, trainer);
    }

    @Transactional
    public WorkoutPlanDto selectPlan(CurrentUser currentUser, SelectPlanRequest request) {
        FitnessProfile profile = requireProfile(currentUser.id());

        List<Exercise> allExercises = exerciseRepository.findAll();
        Map<String, Exercise> byName = allExercises.stream()
                .collect(Collectors.toMap(Exercise::getName, e -> e));
        Map<UUID, Exercise> byId = allExercises.stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));

        TrainerPhilosophy trainer = resolveTrainer(request.trainerId(), profile);
        PlanOptionsResponse options = generationService.generateOptions(profile, byName, trainer);
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
        plan.setTrainerPhilosophy(trainer.getId(), trainer.getDisplayName());
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

    /**
     * Returns the trainer by ID, or the first (default) trainer if no ID supplied.
     * Rejects a trainer that isn't visible to the user's profile sex (e.g. a
     * physiology-specific philosophy requested by someone it isn't meant for) — the
     * normal UI flow only ever offers sex-appropriate trainers via GET /api/trainers,
     * so this only triggers on direct/malformed API use.
     */
    private TrainerPhilosophy resolveTrainer(UUID trainerId, FitnessProfile profile) {
        if (trainerId != null) {
            TrainerPhilosophy trainer = trainerRepository.findById(trainerId)
                    .orElseThrow(() -> new NotFoundException("Trainer philosophy not found."));
            if (!TrainerVisibility.isVisible(trainer.getTargetSex(), profile.getSex().name())) {
                throw new NotFoundException("Trainer philosophy not found.");
            }
            return trainer;
        }
        return trainerRepository.findAllByOrderBySortOrderAsc()
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No trainer philosophies seeded."));
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
