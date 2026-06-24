package com.fitcoach.session;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.session.dto.CompleteSessionRequest;
import com.fitcoach.session.dto.LogSetRequest;
import com.fitcoach.session.dto.PreviousSetDto;
import com.fitcoach.session.dto.WorkoutSessionDto;
import com.fitcoach.workout.WorkoutDay;
import com.fitcoach.workout.WorkoutDayRepository;
import com.fitcoach.workout.WorkoutExercise;
import com.fitcoach.workout.WorkoutExerciseRepository;
import com.fitcoach.workout.WorkoutPlan;
import com.fitcoach.workout.WorkoutPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkoutSessionService {

    private final WorkoutSessionRepository sessionRepository;
    private final SetLogRepository setLogRepository;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutDayRepository dayRepository;
    private final WorkoutExerciseRepository exerciseRepository;

    public WorkoutSessionService(WorkoutSessionRepository sessionRepository,
                                  SetLogRepository setLogRepository,
                                  WorkoutPlanRepository planRepository,
                                  WorkoutDayRepository dayRepository,
                                  WorkoutExerciseRepository exerciseRepository) {
        this.sessionRepository = sessionRepository;
        this.setLogRepository = setLogRepository;
        this.planRepository = planRepository;
        this.dayRepository = dayRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional
    public WorkoutSessionDto startSession(CurrentUser currentUser, UUID workoutDayId) {
        // Abandon any existing in-progress session first
        sessionRepository.findByUserIdAndStatus(currentUser.id(), SessionStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    existing.abandon();
                    sessionRepository.save(existing);
                });

        WorkoutDay day = dayRepository.findById(workoutDayId)
                .orElseThrow(() -> new NotFoundException("Workout day not found."));

        WorkoutPlan plan = planRepository.findByUserIdAndIsActiveTrue(currentUser.id())
                .orElseThrow(() -> new NotFoundException("No active plan found."));

        // Verify the day belongs to this user's active plan
        if (!day.getWorkoutPlan().getId().equals(plan.getId())) {
            throw new NotFoundException("Workout day not found.");
        }

        WorkoutSession session = new WorkoutSession(currentUser.id(), plan, day);
        return WorkoutSessionDto.from(sessionRepository.save(session));
    }

    @Transactional
    public WorkoutSessionDto logSet(CurrentUser currentUser, UUID sessionId, LogSetRequest request) {
        WorkoutSession session = requireOwnedSession(currentUser.id(), sessionId);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is not in progress.");
        }

        WorkoutExercise workoutExercise = exerciseRepository.findById(request.workoutExerciseId())
                .orElseThrow(() -> new NotFoundException("Workout exercise not found."));

        // Upsert: if a log for this set already exists, replace it
        session.getSetLogs().removeIf(sl ->
                sl.getWorkoutExercise().getId().equals(request.workoutExerciseId())
                        && sl.getSetNumber() == request.setNumber());

        SetLog log = new SetLog(session, workoutExercise, request.setNumber(),
                request.weightKg(), request.repsCompleted(), request.rirActual());
        session.addSetLog(log);

        return WorkoutSessionDto.from(sessionRepository.save(session));
    }

    @Transactional
    public WorkoutSessionDto completeSession(CurrentUser currentUser, UUID sessionId,
                                              CompleteSessionRequest request) {
        WorkoutSession session = requireOwnedSession(currentUser.id(), sessionId);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is already " + session.getStatus().name().toLowerCase() + ".");
        }

        session.complete(request.notes());
        return WorkoutSessionDto.from(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public WorkoutSessionDto getActiveSession(CurrentUser currentUser) {
        WorkoutSession session = sessionRepository
                .findByUserIdAndStatus(currentUser.id(), SessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new NotFoundException("No active session."));
        return WorkoutSessionDto.from(session);
    }

    @Transactional(readOnly = true)
    public List<PreviousSetDto> getPreviousSets(CurrentUser currentUser, UUID exerciseId) {
        return setLogRepository.findRecentByUserAndExercise(currentUser.id(), exerciseId)
                .stream()
                // Only take the most recent completed session's sets (the query returns DESC by session date)
                .takeWhile(s -> true)
                .collect(java.util.stream.Collectors.groupingBy(
                        s -> s.getWorkoutSession().getId()))
                .values()
                .stream()
                .findFirst()
                .map(sets -> sets.stream()
                        .map(s -> new PreviousSetDto(s.getSetNumber(), s.getWeightKg(),
                                s.getRepsCompleted(), s.getRirActual()))
                        .sorted(java.util.Comparator.comparingInt(PreviousSetDto::setNumber))
                        .toList())
                .orElse(List.of());
    }

    private WorkoutSession requireOwnedSession(UUID userId, UUID sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found."));
        if (!session.getUserId().equals(userId)) {
            throw new NotFoundException("Session not found.");
        }
        return session;
    }
}
