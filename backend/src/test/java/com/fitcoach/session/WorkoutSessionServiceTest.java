package com.fitcoach.session;

import com.fitcoach.auth.Role;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.exercise.Exercise;
import com.fitcoach.exercise.domain.DifficultyLevel;
import com.fitcoach.exercise.domain.MovementPattern;
import com.fitcoach.exercise.domain.MuscleGroup;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.session.dto.CompleteSessionRequest;
import com.fitcoach.session.dto.ExerciseHistoryEntryDto;
import com.fitcoach.session.dto.LogSetRequest;
import com.fitcoach.session.dto.PreviousSetDto;
import com.fitcoach.session.dto.WorkoutSessionDto;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.workout.WorkoutDay;
import com.fitcoach.workout.WorkoutDayRepository;
import com.fitcoach.workout.WorkoutExercise;
import com.fitcoach.workout.WorkoutExerciseRepository;
import com.fitcoach.workout.WorkoutPlan;
import com.fitcoach.workout.WorkoutPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WorkoutSessionService had zero dedicated unit tests before this — only exercised
 * indirectly through WorkoutControllerTest's full mock.
 */
@ExtendWith(MockitoExtension.class)
class WorkoutSessionServiceTest {

    @Mock private WorkoutSessionRepository sessionRepository;
    @Mock private SetLogRepository setLogRepository;
    @Mock private WorkoutPlanRepository planRepository;
    @Mock private WorkoutDayRepository dayRepository;
    @Mock private WorkoutExerciseRepository exerciseRepository;

    @InjectMocks
    private WorkoutSessionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final CurrentUser CURRENT_USER = new CurrentUser(USER_ID, "test@example.com", Role.USER);

    private WorkoutPlan activePlan() {
        WorkoutPlan plan = new WorkoutPlan(USER_ID, "Test Plan", MainGoal.MUSCLE_GAIN, 4);
        plan.activate();
        return plan;
    }

    private Exercise exercise(String name) {
        return new Exercise(name, MuscleGroup.CHEST, MovementPattern.PUSH,
                DifficultyLevel.BEGINNER, "Form cue", "Common mistake");
    }

    private WorkoutExercise workoutExercise(WorkoutDay day, Exercise ex) {
        return new WorkoutExercise(day, ex, 1, 3, 8, 12, "2 RIR", 90);
    }

    // ─── startSession ──────────────────────────────────────────────────────────

    @Test
    void startSession_createsSessionForOwnedDayOnActivePlan() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");

        when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkoutSessionDto result = service.startSession(CURRENT_USER, day.getId());

        assertThat(result.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(result.workoutDay().id()).isEqualTo(day.getId());
    }

    @Test
    void startSession_abandonsAnyExistingInProgressSessionFirst() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        WorkoutSession existing = new WorkoutSession(USER_ID, plan, day);

        when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existing));
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.startSession(CURRENT_USER, day.getId());

        assertThat(existing.getStatus()).isEqualTo(SessionStatus.ABANDONED);
        verify(sessionRepository, times(2)).save(any());
    }

    @Test
    void startSession_throwsWhenDayNotFound() {
        when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        UUID missingDayId = UUID.randomUUID();
        when(dayRepository.findById(missingDayId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startSession(CURRENT_USER, missingDayId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void startSession_throwsWhenNoActivePlan() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");

        when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startSession(CURRENT_USER, day.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void startSession_throwsWhenDayBelongsToAnotherPlan() {
        WorkoutPlan ownedPlan = activePlan();
        WorkoutPlan otherPlan = new WorkoutPlan(UUID.randomUUID(), "Other User's Plan", MainGoal.FAT_LOSS, 3);
        WorkoutDay dayOnOtherPlan = new WorkoutDay(otherPlan, 1, "Full Body A");

        when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(dayRepository.findById(dayOnOtherPlan.getId())).thenReturn(Optional.of(dayOnOtherPlan));
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(ownedPlan));

        assertThatThrownBy(() -> service.startSession(CURRENT_USER, dayOnOtherPlan.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── logSet ─────────────────────────────────────────────────────────────────

    @Test
    void logSet_addsNewSetLog() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        WorkoutExercise we = workoutExercise(day, exercise("Bench Press"));
        WorkoutSession session = new WorkoutSession(USER_ID, plan, day);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(exerciseRepository.findById(we.getId())).thenReturn(Optional.of(we));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LogSetRequest request = new LogSetRequest(we.getId(), 1, new BigDecimal("60.0"), 10, 2);
        WorkoutSessionDto result = service.logSet(CURRENT_USER, session.getId(), request);

        assertThat(result.setLogs()).hasSize(1);
        assertThat(result.setLogs().get(0).repsCompleted()).isEqualTo(10);
    }

    @Test
    void logSet_replacesExistingLogForSameSetNumber() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        WorkoutExercise we = workoutExercise(day, exercise("Bench Press"));
        WorkoutSession session = new WorkoutSession(USER_ID, plan, day);
        session.addSetLog(new SetLog(session, we, 1, new BigDecimal("55.0"), 8, 3));

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(exerciseRepository.findById(we.getId())).thenReturn(Optional.of(we));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LogSetRequest request = new LogSetRequest(we.getId(), 1, new BigDecimal("60.0"), 10, 2);
        WorkoutSessionDto result = service.logSet(CURRENT_USER, session.getId(), request);

        assertThat(result.setLogs()).hasSize(1);
        assertThat(result.setLogs().get(0).weightKg()).isEqualByComparingTo("60.0");
        assertThat(result.setLogs().get(0).repsCompleted()).isEqualTo(10);
    }

    @Test
    void logSet_throwsWhenSessionNotInProgress() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        WorkoutExercise we = workoutExercise(day, exercise("Bench Press"));
        WorkoutSession session = new WorkoutSession(USER_ID, plan, day);
        session.complete(null);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        LogSetRequest request = new LogSetRequest(we.getId(), 1, new BigDecimal("60.0"), 10, 2);

        assertThatThrownBy(() -> service.logSet(CURRENT_USER, session.getId(), request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void logSet_throwsWhenSessionBelongsToAnotherUser() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        WorkoutExercise we = workoutExercise(day, exercise("Bench Press"));
        WorkoutSession session = new WorkoutSession(UUID.randomUUID(), plan, day);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        LogSetRequest request = new LogSetRequest(we.getId(), 1, new BigDecimal("60.0"), 10, 2);

        assertThatThrownBy(() -> service.logSet(CURRENT_USER, session.getId(), request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void logSet_throwsWhenExerciseNotFound() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        WorkoutSession session = new WorkoutSession(USER_ID, plan, day);
        UUID missingExerciseId = UUID.randomUUID();

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(exerciseRepository.findById(missingExerciseId)).thenReturn(Optional.empty());

        LogSetRequest request = new LogSetRequest(missingExerciseId, 1, new BigDecimal("60.0"), 10, 2);

        assertThatThrownBy(() -> service.logSet(CURRENT_USER, session.getId(), request))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── completeSession ────────────────────────────────────────────────────────

    @Test
    void completeSession_marksCompletedWithNotes() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        WorkoutSession session = new WorkoutSession(USER_ID, plan, day);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkoutSessionDto result = service.completeSession(CURRENT_USER, session.getId(),
                new CompleteSessionRequest("Felt strong today"));

        assertThat(result.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.completedAt()).isNotNull();
    }

    @Test
    void completeSession_throwsWhenAlreadyCompleted() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        WorkoutSession session = new WorkoutSession(USER_ID, plan, day);
        session.complete(null);

        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.completeSession(CURRENT_USER, session.getId(),
                new CompleteSessionRequest(null)))
                .isInstanceOf(IllegalStateException.class);
    }

    // ─── getActiveSession ───────────────────────────────────────────────────────

    @Test
    void getActiveSession_throwsWhenNoneInProgress() {
        when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActiveSession(CURRENT_USER))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── getPreviousSets ────────────────────────────────────────────────────────
    // Regression coverage for a real bug: grouping set logs by session with a plain
    // HashMap doesn't preserve the repository's DESC-by-date order, so picking the
    // "first" group could non-deterministically return an older session's sets.

    @Test
    void getPreviousSets_returnsOnlyTheMostRecentSessionsSets() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        Exercise ex = exercise("Bench Press");
        WorkoutExercise we = workoutExercise(day, ex);

        WorkoutSession olderSession = new WorkoutSession(USER_ID, plan, day);
        SetLog olderLog = new SetLog(olderSession, we, 1, new BigDecimal("50.0"), 8, 3);

        WorkoutSession recentSession = new WorkoutSession(USER_ID, plan, day);
        SetLog recentLog2 = new SetLog(recentSession, we, 2, new BigDecimal("62.5"), 9, 2);
        SetLog recentLog1 = new SetLog(recentSession, we, 1, new BigDecimal("60.0"), 10, 2);

        // Repository contract: DESC by session date — most recent session's rows come first.
        when(setLogRepository.findRecentByUserAndExercise(USER_ID, ex.getId()))
                .thenReturn(List.of(recentLog2, recentLog1, olderLog));

        List<PreviousSetDto> result = service.getPreviousSets(CURRENT_USER, ex.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).setNumber()).isEqualTo(1);
        assertThat(result.get(0).weightKg()).isEqualByComparingTo("60.0");
        assertThat(result.get(1).setNumber()).isEqualTo(2);
        assertThat(result.get(1).weightKg()).isEqualByComparingTo("62.5");
    }

    @Test
    void getPreviousSets_returnsEmptyWhenNoHistory() {
        UUID exerciseId = UUID.randomUUID();
        when(setLogRepository.findRecentByUserAndExercise(USER_ID, exerciseId)).thenReturn(List.of());

        List<PreviousSetDto> result = service.getPreviousSets(CURRENT_USER, exerciseId);

        assertThat(result).isEmpty();
    }

    // ─── getExerciseHistory ─────────────────────────────────────────────────────

    @Test
    void getExerciseHistory_computesMaxWeightAndBestRepsPerSession() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        Exercise ex = exercise("Bench Press");
        WorkoutExercise we = workoutExercise(day, ex);

        WorkoutSession session = new WorkoutSession(USER_ID, plan, day);
        SetLog set1 = new SetLog(session, we, 1, new BigDecimal("60.0"), 10, 2);
        SetLog set2 = new SetLog(session, we, 2, new BigDecimal("65.0"), 8, 1);

        when(setLogRepository.findAllByUserAndExerciseAsc(USER_ID, ex.getId()))
                .thenReturn(List.of(set1, set2));

        List<ExerciseHistoryEntryDto> result = service.getExerciseHistory(CURRENT_USER, ex.getId());

        assertThat(result).hasSize(1);
        ExerciseHistoryEntryDto entry = result.get(0);
        assertThat(entry.maxWeightKg()).isEqualTo(65.0);
        assertThat(entry.bestReps()).isEqualTo(10);
        assertThat(entry.totalSets()).isEqualTo(2);
    }

    @Test
    void getExerciseHistory_handlesBodyweightSetsWithNullWeight() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        Exercise ex = exercise("Push-up");
        WorkoutExercise we = workoutExercise(day, ex);

        WorkoutSession session = new WorkoutSession(USER_ID, plan, day);
        SetLog set1 = new SetLog(session, we, 1, null, 15, null);

        when(setLogRepository.findAllByUserAndExerciseAsc(USER_ID, ex.getId()))
                .thenReturn(List.of(set1));

        List<ExerciseHistoryEntryDto> result = service.getExerciseHistory(CURRENT_USER, ex.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).maxWeightKg()).isNull();
        assertThat(result.get(0).bestReps()).isEqualTo(15);
    }

    // ─── getHistory ─────────────────────────────────────────────────────────────

    @Test
    void getHistory_returnsOnlyCompletedSessions() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        WorkoutSession completed = new WorkoutSession(USER_ID, plan, day);
        completed.complete(null);

        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(List.of(completed));

        List<WorkoutSessionDto> result = service.getHistory(CURRENT_USER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(SessionStatus.COMPLETED);
    }

    // ─── findStrugglingExercises ───────────────────────────────────────────────

    private SetLog setLogWithReps(WorkoutSession session, WorkoutExercise we, int setNumber, int reps) {
        return new SetLog(session, we, setNumber, new BigDecimal("40.0"), reps, null);
    }

    @Test
    void findStrugglingExercises_flagsExerciseWhenBothOfLastTwoSessionsMissedRepMin() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        Exercise ex = exercise("Bench Press");
        WorkoutExercise we = workoutExercise(day, ex); // repRangeMin = 8

        WorkoutSession older = new WorkoutSession(USER_ID, plan, day);
        WorkoutSession recent = new WorkoutSession(USER_ID, plan, day);
        // Both sessions: reps below repRangeMin (8) on every set.
        List<SetLog> logs = List.of(
                setLogWithReps(recent, we, 1, 6),
                setLogWithReps(recent, we, 2, 5),
                setLogWithReps(older, we, 1, 6),
                setLogWithReps(older, we, 2, 5)
        );
        when(setLogRepository.findRecentByUserAndExercise(USER_ID, ex.getId())).thenReturn(logs);

        var result = service.findStrugglingExercises(USER_ID, List.of(ex.getId()));

        assertThat(result).containsExactly(ex.getId());
    }

    @Test
    void findStrugglingExercises_doesNotFlagWhenOnlyOneSessionHasHistory() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        Exercise ex = exercise("Bench Press");
        WorkoutExercise we = workoutExercise(day, ex);

        WorkoutSession onlySession = new WorkoutSession(USER_ID, plan, day);
        List<SetLog> logs = List.of(setLogWithReps(onlySession, we, 1, 3));
        when(setLogRepository.findRecentByUserAndExercise(USER_ID, ex.getId())).thenReturn(logs);

        var result = service.findStrugglingExercises(USER_ID, List.of(ex.getId()));

        assertThat(result).isEmpty();
    }

    @Test
    void findStrugglingExercises_doesNotFlagWhenMostSetsHitTheRange() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        Exercise ex = exercise("Bench Press");
        WorkoutExercise we = workoutExercise(day, ex); // repRangeMin = 8

        WorkoutSession older = new WorkoutSession(USER_ID, plan, day);
        WorkoutSession recent = new WorkoutSession(USER_ID, plan, day);
        List<SetLog> logs = List.of(
                setLogWithReps(recent, we, 1, 9),
                setLogWithReps(recent, we, 2, 10),
                setLogWithReps(older, we, 1, 8),
                setLogWithReps(older, we, 2, 9)
        );
        when(setLogRepository.findRecentByUserAndExercise(USER_ID, ex.getId())).thenReturn(logs);

        var result = service.findStrugglingExercises(USER_ID, List.of(ex.getId()));

        assertThat(result).isEmpty();
    }

    @Test
    void findStrugglingExercises_usesHistoricalWorkoutExerciseRepRangeNotTodaysPlan() {
        // Plan-drift scenario: an older session's set is judged against the
        // repRangeMin that was actually prescribed for it back then (8), not
        // whatever a newer WorkoutExercise row for the same Exercise prescribes now.
        WorkoutPlan plan = activePlan();
        WorkoutDay oldDay = new WorkoutDay(plan, 1, "Upper A");
        WorkoutDay newDay = new WorkoutDay(plan, 1, "Upper A");
        Exercise ex = exercise("Bench Press");
        WorkoutExercise oldWe = new WorkoutExercise(oldDay, ex, 1, 3, 8, 12, "2 RIR", 90); // repRangeMin 8
        WorkoutExercise newWe = new WorkoutExercise(newDay, ex, 1, 3, 5, 8, "2 RIR", 90);  // repRangeMin 5

        WorkoutSession olderSession = new WorkoutSession(USER_ID, plan, oldDay);
        WorkoutSession recentSession = new WorkoutSession(USER_ID, plan, newDay);
        List<SetLog> logs = List.of(
                // Recent session, against newWe's repRangeMin=5: 6 reps clears it -> not missed.
                setLogWithReps(recentSession, newWe, 1, 6),
                // Older session, against oldWe's repRangeMin=8: 6 reps misses it -> missed.
                setLogWithReps(olderSession, oldWe, 1, 6)
        );
        when(setLogRepository.findRecentByUserAndExercise(USER_ID, ex.getId())).thenReturn(logs);

        var result = service.findStrugglingExercises(USER_ID, List.of(ex.getId()));

        // Only one of the two sessions actually missed its own prescribed range,
        // so the exercise is not (yet) struggling.
        assertThat(result).isEmpty();
    }

    @Test
    void startSession_populatesStrugglingExerciseIds() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        Exercise ex = exercise("Bench Press");
        WorkoutExercise we = workoutExercise(day, ex);
        day.addExercise(we);

        WorkoutSession older = new WorkoutSession(USER_ID, plan, day);
        WorkoutSession recent = new WorkoutSession(USER_ID, plan, day);
        List<SetLog> logs = List.of(
                setLogWithReps(recent, we, 1, 5),
                setLogWithReps(older, we, 1, 5)
        );

        when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(dayRepository.findById(day.getId())).thenReturn(Optional.of(day));
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(setLogRepository.findRecentByUserAndExercise(USER_ID, ex.getId())).thenReturn(logs);

        WorkoutSessionDto result = service.startSession(CURRENT_USER, day.getId());

        assertThat(result.strugglingExerciseIds()).containsExactly(ex.getId());
    }

    @Test
    void getActiveSession_populatesStrugglingExerciseIds() {
        WorkoutPlan plan = activePlan();
        WorkoutDay day = new WorkoutDay(plan, 1, "Upper A");
        Exercise ex = exercise("Bench Press");
        WorkoutExercise we = workoutExercise(day, ex);
        day.addExercise(we);
        WorkoutSession active = new WorkoutSession(USER_ID, plan, day);

        WorkoutSession older = new WorkoutSession(USER_ID, plan, day);
        WorkoutSession recent = new WorkoutSession(USER_ID, plan, day);
        List<SetLog> logs = List.of(
                setLogWithReps(recent, we, 1, 5),
                setLogWithReps(older, we, 1, 5)
        );

        when(sessionRepository.findByUserIdAndStatus(USER_ID, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(active));
        when(setLogRepository.findRecentByUserAndExercise(USER_ID, ex.getId())).thenReturn(logs);

        WorkoutSessionDto result = service.getActiveSession(CURRENT_USER);

        assertThat(result.strugglingExerciseIds()).containsExactly(ex.getId());
    }
}
