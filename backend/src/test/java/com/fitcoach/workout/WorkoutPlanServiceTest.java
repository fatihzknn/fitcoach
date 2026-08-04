package com.fitcoach.workout;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.exercise.ExerciseRepository;
import com.fitcoach.profile.FitnessProfileRepository;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.trainer.TrainerPhilosophy;
import com.fitcoach.trainer.TrainerPhilosophyRepository;
import com.fitcoach.workout.dto.WorkoutPlanDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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

    @InjectMocks
    private WorkoutPlanService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

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
}
