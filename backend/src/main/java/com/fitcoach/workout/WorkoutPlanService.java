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
import com.fitcoach.workout.dto.CreateCustomPlanRequest;
import com.fitcoach.workout.dto.CustomPlanDayRequest;
import com.fitcoach.workout.dto.CustomPlanExerciseRequest;
import com.fitcoach.workout.dto.PlanOptionsResponse;
import com.fitcoach.workout.dto.SelectPlanRequest;
import com.fitcoach.workout.dto.WorkoutDayDto;
import com.fitcoach.workout.dto.WorkoutExerciseDto;
import com.fitcoach.workout.dto.WorkoutPlanDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        return getPlanOptionsForUser(currentUser.id(), trainerId);
    }

    /** Same as getPlanOptions, but for an explicit user — used by the trainer
     *  portal (com.fitcoach.roster) to generate options on a client's behalf,
     *  after the caller has already verified trainer ownership of that client. */
    @Transactional(readOnly = true)
    public PlanOptionsResponse getPlanOptionsForUser(UUID userId, UUID trainerId) {
        FitnessProfile profile = requireProfile(userId);
        Map<String, Exercise> byName = loadExercisesByName();
        TrainerPhilosophy trainer = resolveTrainer(trainerId, profile);
        return generationService.generateOptions(profile, byName, trainer);
    }

    @Transactional
    public WorkoutPlanDto selectPlan(CurrentUser currentUser, SelectPlanRequest request) {
        return selectPlanForUser(currentUser.id(), request);
    }

    /** Same as selectPlan, but for an explicit user — used by the trainer portal
     *  to assign a plan to a client, after ownership has already been verified. */
    @Transactional
    public WorkoutPlanDto selectPlanForUser(UUID userId, SelectPlanRequest request) {
        FitnessProfile profile = requireProfile(userId);

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

        deactivateExistingActivePlan(userId);

        // Build and persist the selected plan
        WorkoutPlan plan = new WorkoutPlan(
                userId,
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
        return WorkoutPlanDto.from(saved, isDeloadRecommended(saved));
    }

    /**
     * Builds and activates a fully custom, trainer-authored plan for a client —
     * bypasses WorkoutGenerationService entirely. No requireProfile() check: nothing
     * here reads the client's FitnessProfile (goal/days/exercises all come explicitly
     * from the trainer), and requiring onboarding-completion would block a trainer
     * from planning for a brand-new client who hasn't finished their own setup yet.
     * dayNumber/orderIndex are assigned from list position, never trusted from the
     * request — workout_days/workout_exercises have unique constraints on those, and
     * GlobalExceptionHandler has no DataIntegrityViolationException handler, so a
     * client-supplied duplicate would otherwise surface as an opaque 500.
     */
    @Transactional
    public WorkoutPlanDto createCustomPlanForUser(UUID userId, CreateCustomPlanRequest request) {
        Map<UUID, Exercise> byId = exerciseRepository.findAll().stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));

        deactivateExistingActivePlan(userId);

        WorkoutPlan plan = new WorkoutPlan(userId, request.name(), request.goal(), request.days().size());
        plan.markCustom();
        plan.activate();

        int dayNumber = 1;
        for (CustomPlanDayRequest dayReq : request.days()) {
            WorkoutDay day = new WorkoutDay(plan, dayNumber++, dayReq.workoutName());
            int order = 1;
            for (CustomPlanExerciseRequest exReq : dayReq.exercises()) {
                Exercise exercise = byId.get(exReq.exerciseId());
                if (exercise == null) {
                    throw new NotFoundException("Exercise not found: " + exReq.exerciseId());
                }
                WorkoutExercise we = new WorkoutExercise(
                        day, exercise, order++,
                        exReq.sets(), exReq.repRangeMin(), exReq.repRangeMax(),
                        exReq.rirGuidance(), exReq.restSeconds()
                );
                day.addExercise(we);
            }
            plan.addDay(day);
        }

        WorkoutPlan saved = planRepository.save(plan);
        return WorkoutPlanDto.from(saved, isDeloadRecommended(saved));
    }

    private void deactivateExistingActivePlan(UUID userId) {
        planRepository.findByUserIdAndIsActiveTrue(userId)
                .ifPresent(existing -> {
                    existing.deactivate();
                    planRepository.save(existing);
                });
    }

    @Transactional(readOnly = true)
    public WorkoutPlanDto getActivePlan(CurrentUser currentUser) {
        WorkoutPlan plan = planRepository.findByUserIdAndIsActiveTrue(currentUser.id())
                .orElseThrow(() -> new NotFoundException("No active workout plan. Select a plan first."));
        return WorkoutPlanDto.from(plan, isDeloadRecommended(plan));
    }

    /** Same as getActivePlan, but for an explicit user and nullable rather than
     *  throwing — used by the trainer portal to show a client's active plan
     *  after ownership has been verified. A trainer viewing a client who hasn't
     *  picked a plan yet is a normal state there, not an error (unlike the
     *  client's own /api/plan/active, which the frontend relies on 404-ing to
     *  redirect to plan-selection — kept independent so that contract doesn't change). */
    @Transactional(readOnly = true)
    public WorkoutPlanDto getActivePlanForUser(UUID userId) {
        return planRepository.findByUserIdAndIsActiveTrue(userId)
                .map(plan -> WorkoutPlanDto.from(plan, isDeloadRecommended(plan)))
                .orElse(null);
    }

    /**
     * True once the plan has run at least as long as its trainer's recommended
     * deload frequency — TrainerPhilosophy.deloadFrequencyWeeks has been seeded
     * since Phase 9 but was never read anywhere until now.
     */
    private boolean isDeloadRecommended(WorkoutPlan plan) {
        if (plan.getTrainerPhilosophyId() == null) return false;
        return trainerRepository.findById(plan.getTrainerPhilosophyId())
                .map(trainer -> {
                    // ChronoUnit.WEEKS doesn't support Instant (a pure timestamp, not a
                    // date) — go through DAYS instead.
                    long daysElapsed = ChronoUnit.DAYS.between(plan.getCreatedAt(), Instant.now());
                    long weeksElapsed = daysElapsed / 7;
                    return weeksElapsed >= trainer.getDeloadFrequencyWeeks();
                })
                .orElse(false);
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
