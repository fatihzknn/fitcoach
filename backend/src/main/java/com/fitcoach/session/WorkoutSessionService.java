package com.fitcoach.session;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.common.NotFoundException;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.session.dto.CompleteSessionRequest;
import com.fitcoach.session.dto.ExerciseHistoryEntryDto;
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

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkoutSessionService {

    private static final int STRUGGLE_LOOKBACK_SESSIONS = 2;

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
        WorkoutSession saved = sessionRepository.save(session);
        return WorkoutSessionDto.from(saved, strugglingExerciseIdsForDay(currentUser.id(), day));
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
        return WorkoutSessionDto.from(session, strugglingExerciseIdsForDay(currentUser.id(), session.getWorkoutDay()));
    }

    @Transactional(readOnly = true)
    public List<PreviousSetDto> getPreviousSets(CurrentUser currentUser, UUID exerciseId) {
        // Only take the most recent completed session's sets (the query returns DESC by
        // session date). Grouping must preserve that order — a plain HashMap doesn't, so
        // .findFirst() on its values() could non-deterministically return an older
        // session's sets instead. LinkedHashMap keeps the first-seen (most recent) group first.
        return setLogRepository.findRecentByUserAndExercise(currentUser.id(), exerciseId)
                .stream()
                .collect(Collectors.groupingBy(
                        s -> s.getWorkoutSession().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values()
                .stream()
                .findFirst()
                .map(sets -> sets.stream()
                        .map(s -> new PreviousSetDto(s.getSetNumber(), s.getWeightKg(),
                                s.getRepsCompleted(), s.getRirActual()))
                        .sorted(Comparator.comparingInt(PreviousSetDto::setNumber))
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<WorkoutSessionDto> getHistory(CurrentUser currentUser) {
        return sessionRepository
                .findAllByUserIdAndStatusOrderByStartedAtDesc(currentUser.id(), SessionStatus.COMPLETED)
                .stream()
                .map(WorkoutSessionDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExerciseHistoryEntryDto> getExerciseHistory(CurrentUser currentUser, UUID exerciseId) {
        List<SetLog> logs = setLogRepository.findAllByUserAndExerciseAsc(currentUser.id(), exerciseId);

        // Group by session UUID (preserving chronological order via LinkedHashMap)
        Map<UUID, List<SetLog>> bySession = logs.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getWorkoutSession().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return bySession.values().stream()
                .map(sessionLogs -> {
                    String date = sessionLogs.get(0).getWorkoutSession()
                            .getStartedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();

                    Double maxWeight = sessionLogs.stream()
                            .map(SetLog::getWeightKg)
                            .filter(w -> w != null)
                            .map(BigDecimal::doubleValue)
                            .max(Comparator.naturalOrder())
                            .orElse(null);

                    int bestReps = sessionLogs.stream()
                            .mapToInt(SetLog::getRepsCompleted)
                            .max().orElse(0);

                    return new ExerciseHistoryEntryDto(date, maxWeight, bestReps, sessionLogs.size());
                })
                .toList();
    }

    /**
     * An exercise "struggles" when, in each of the user's most recent
     * STRUGGLE_LOOKBACK_SESSIONS completed sessions logging it, a majority of sets
     * missed the bottom of what was prescribed AT THE TIME. Each SetLog is compared
     * against its own WorkoutExercise.repRangeMin (what was actually prescribed when
     * that set was logged) — never against today's active plan's prescription, since
     * a new plan creates brand-new WorkoutExercise rows and old SetLogs still point
     * at the old ones. Requires the full lookback window of history; a new exercise
     * never false-positives from too little data.
     */
    @Transactional(readOnly = true)
    public Set<UUID> findStrugglingExercises(UUID userId, Collection<UUID> exerciseIds) {
        return exerciseIds.stream()
                .filter(id -> isStruggling(userId, id))
                .collect(Collectors.toSet());
    }

    private boolean isStruggling(UUID userId, UUID exerciseId) {
        // Grouping must use LinkedHashMap, not a plain HashMap — same reasoning as
        // getPreviousSets(): the query returns DESC by session date, and only a
        // LinkedHashMap preserves that order when picking "the most recent N sessions".
        Map<UUID, List<SetLog>> bySession = setLogRepository.findRecentByUserAndExercise(userId, exerciseId)
                .stream()
                .collect(Collectors.groupingBy(
                        s -> s.getWorkoutSession().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<List<SetLog>> recentSessions = bySession.values().stream()
                .limit(STRUGGLE_LOOKBACK_SESSIONS)
                .toList();
        if (recentSessions.size() < STRUGGLE_LOOKBACK_SESSIONS) {
            return false;
        }

        return recentSessions.stream().allMatch(sets -> {
            long missed = sets.stream()
                    .filter(s -> s.getRepsCompleted() < s.getWorkoutExercise().getRepRangeMin())
                    .count();
            return missed * 2 >= sets.size();
        });
    }

    private Set<UUID> strugglingExerciseIdsForDay(UUID userId, WorkoutDay day) {
        List<UUID> exerciseIds = day.getExercises().stream()
                .map(we -> we.getExercise().getId())
                .distinct()
                .toList();
        return findStrugglingExercises(userId, exerciseIds);
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
