package com.fitcoach.workout;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.exercise.Exercise;
import com.fitcoach.exercise.ExerciseRepository;
import com.fitcoach.exercise.domain.DifficultyLevel;
import com.fitcoach.exercise.domain.MovementPattern;
import com.fitcoach.exercise.domain.MuscleGroup;
import com.fitcoach.profile.FitnessProfileRepository;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.session.WorkoutSessionService;
import com.fitcoach.trainer.TrainerPhilosophy;
import com.fitcoach.trainer.TrainerPhilosophyRepository;
import com.fitcoach.workout.dto.CreateCustomPlanRequest;
import com.fitcoach.workout.dto.CustomPlanDayRequest;
import com.fitcoach.workout.dto.CustomPlanExerciseRequest;
import com.fitcoach.workout.dto.WorkoutPlanDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused on isDeloadRecommended(), reached via the public getActivePlan() —
 * WorkoutPlanService had no dedicated unit test before this (only exercised
 * indirectly through WorkoutControllerTest's full mock).
 */
@ExtendWith(MockitoExtension.class)
class WorkoutPlanServiceTest {

    @Mock private WorkoutPlanRepository planRepository;
    @Mock private FitnessProfileRepository profileRepository;
    @Mock private ExerciseRepository exerciseRepository;
    @Mock private WorkoutGenerationService generationService;
    @Mock private TrainerPhilosophyRepository trainerRepository;
    @Mock private WorkoutSessionService sessionService;

    @InjectMocks
    private WorkoutPlanService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

    @BeforeEach
    void defaultNoStrugglingExercises() {
        // Struggle-based deload is a second, independent trigger alongside the
        // time-based one — default every test to "nothing struggling" so existing
        // time-based assertions aren't affected; tests that care override this.
        lenient().when(sessionService.findStrugglingExercises(any(), any())).thenReturn(Set.of());
    }

    private WorkoutPlan mockPlan(UUID trainerId, Instant createdAt) {
        WorkoutPlan plan = mock(WorkoutPlan.class);
        when(plan.getId()).thenReturn(UUID.randomUUID());
        when(plan.getName()).thenReturn("Test Plan");
        when(plan.getGoal()).thenReturn(MainGoal.MUSCLE_GAIN);
        when(plan.getTrainingDaysPerWeek()).thenReturn(4);
        when(plan.isActive()).thenReturn(true);
        when(plan.getDays()).thenReturn(List.of());
        when(plan.getTrainerPhilosophyId()).thenReturn(trainerId);
        when(plan.getTrainerPhilosophyName()).thenReturn("Evidence-Based");
        // lenient: unread when isDeloadRecommended() short-circuits before checking dates
        // (no trainer linked, or trainer not found)
        lenient().when(plan.getCreatedAt()).thenReturn(createdAt);
        return plan;
    }

    private TrainerPhilosophy mockTrainer(int deloadFrequencyWeeks) {
        TrainerPhilosophy trainer = mock(TrainerPhilosophy.class);
        when(trainer.getDeloadFrequencyWeeks()).thenReturn(deloadFrequencyWeeks);
        return trainer;
    }

    @Test
    void deloadRecommendedWhenWeeksElapsedExceedsFrequency() {
        UUID trainerId = UUID.randomUUID();
        Instant eightWeeksAgo = Instant.now().minus(8 * 7, ChronoUnit.DAYS);
        WorkoutPlan plan = mockPlan(trainerId, eightWeeksAgo);
        TrainerPhilosophy trainer = mockTrainer(6);
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));
        when(trainerRepository.findById(trainerId)).thenReturn(Optional.of(trainer));

        WorkoutPlanDto result = service.getActivePlan(CURRENT_USER);

        assertThat(result.deloadRecommended()).isTrue();
    }

    @Test
    void deloadNotRecommendedWhenUnderFrequency() {
        UUID trainerId = UUID.randomUUID();
        Instant twoWeeksAgo = Instant.now().minus(2 * 7, ChronoUnit.DAYS);
        WorkoutPlan plan = mockPlan(trainerId, twoWeeksAgo);
        TrainerPhilosophy trainer = mockTrainer(6);
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));
        when(trainerRepository.findById(trainerId)).thenReturn(Optional.of(trainer));

        WorkoutPlanDto result = service.getActivePlan(CURRENT_USER);

        assertThat(result.deloadRecommended()).isFalse();
    }

    @Test
    void deloadNotRecommendedWhenNoTrainerLinked() {
        WorkoutPlan plan = mockPlan(null, Instant.now().minus(20 * 7, ChronoUnit.DAYS));
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));

        WorkoutPlanDto result = service.getActivePlan(CURRENT_USER);

        assertThat(result.deloadRecommended()).isFalse();
    }

    @Test
    void deloadNotRecommendedWhenTrainerNotFound() {
        UUID trainerId = UUID.randomUUID();
        WorkoutPlan plan = mockPlan(trainerId, Instant.now().minus(20 * 7, ChronoUnit.DAYS));
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));
        when(trainerRepository.findById(trainerId)).thenReturn(Optional.empty());

        WorkoutPlanDto result = service.getActivePlan(CURRENT_USER);

        assertThat(result.deloadRecommended()).isFalse();
    }

    @Test
    void deloadRecommendedWhenStruggleDetectedEvenIfTimeNotElapsed() {
        // Fresh plan (createdAt = now) — the time-based check alone would say no —
        // but the user is struggling with an exercise in the plan.
        UUID trainerId = UUID.randomUUID();
        WorkoutPlan plan = mockPlan(trainerId, Instant.now());
        TrainerPhilosophy trainer = mockTrainer(6);
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));
        when(trainerRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(sessionService.findStrugglingExercises(any(), any())).thenReturn(Set.of(UUID.randomUUID()));

        WorkoutPlanDto result = service.getActivePlan(CURRENT_USER);

        assertThat(result.deloadRecommended()).isTrue();
    }

    @Test
    void notDeloadRecommendedWhenNeitherTimeNorStruggleTrigger() {
        UUID trainerId = UUID.randomUUID();
        WorkoutPlan plan = mockPlan(trainerId, Instant.now());
        TrainerPhilosophy trainer = mockTrainer(6);
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));
        when(trainerRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        // sessionService.findStrugglingExercises defaults to Set.of() via @BeforeEach

        WorkoutPlanDto result = service.getActivePlan(CURRENT_USER);

        assertThat(result.deloadRecommended()).isFalse();
    }

    // ─── createCustomPlanForUser ────────────────────────────────────────────────

    private Exercise exercise(String name) {
        return new Exercise(name, MuscleGroup.CHEST, MovementPattern.PUSH,
                DifficultyLevel.BEGINNER, "form cue", "common mistake");
    }

    private CustomPlanExerciseRequest exerciseRequest(UUID exerciseId) {
        return new CustomPlanExerciseRequest(exerciseId, 3, 8, 12, "2 RIR", 90);
    }

    @Test
    void createCustomPlanForUser_persistsFullGraphWithSequentialDayAndOrderNumbers() {
        Exercise benchPress = exercise("Barbell Bench Press");
        Exercise row = exercise("Cable Row");
        when(exerciseRepository.findAll()).thenReturn(List.of(benchPress, row));
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateCustomPlanRequest request = new CreateCustomPlanRequest(
                "Custom Plan", MainGoal.MUSCLE_GAIN,
                List.of(
                        new CustomPlanDayRequest("Day A", List.of(
                                exerciseRequest(benchPress.getId()), exerciseRequest(row.getId()))),
                        new CustomPlanDayRequest("Day B", List.of(exerciseRequest(benchPress.getId())))
                ));

        WorkoutPlanDto result = service.createCustomPlanForUser(USER_ID, request);

        assertThat(result.name()).isEqualTo("Custom Plan");
        assertThat(result.isCustom()).isTrue();
        assertThat(result.days()).hasSize(2);
        assertThat(result.days().get(0).dayNumber()).isEqualTo(1);
        assertThat(result.days().get(1).dayNumber()).isEqualTo(2);
        assertThat(result.days().get(0).exercises()).hasSize(2);
        assertThat(result.days().get(0).exercises().get(0).orderIndex()).isEqualTo(1);
        assertThat(result.days().get(0).exercises().get(1).orderIndex()).isEqualTo(2);
    }

    @Test
    void createCustomPlanForUser_deactivatesExistingActivePlan() {
        Exercise benchPress = exercise("Barbell Bench Press");
        when(exerciseRepository.findAll()).thenReturn(List.of(benchPress));
        WorkoutPlan existing = mock(WorkoutPlan.class);
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(existing));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateCustomPlanRequest request = new CreateCustomPlanRequest(
                "Custom Plan", MainGoal.MUSCLE_GAIN,
                List.of(new CustomPlanDayRequest("Day A", List.of(exerciseRequest(benchPress.getId())))));

        service.createCustomPlanForUser(USER_ID, request);

        verify(existing).deactivate();
        ArgumentCaptor<WorkoutPlan> captor = ArgumentCaptor.forClass(WorkoutPlan.class);
        verify(planRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0)).isSameAs(existing);
    }

    @Test
    void createCustomPlanForUser_unknownExerciseId_throwsNotFound() {
        when(exerciseRepository.findAll()).thenReturn(List.of());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());

        CreateCustomPlanRequest request = new CreateCustomPlanRequest(
                "Custom Plan", MainGoal.MUSCLE_GAIN,
                List.of(new CustomPlanDayRequest("Day A", List.of(exerciseRequest(UUID.randomUUID())))));

        assertThatThrownBy(() -> service.createCustomPlanForUser(USER_ID, request))
                .isInstanceOf(NotFoundException.class);
        verify(planRepository, never()).save(any());
    }
}
