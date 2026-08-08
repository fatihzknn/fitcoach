package com.fitcoach.checkin;

import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.checkin.dto.ProgressStatsDto;
import com.fitcoach.checkin.dto.SubmitCheckInRequest;
import com.fitcoach.checkin.dto.WeeklyCheckInDto;
import com.fitcoach.session.WorkoutSession;
import com.fitcoach.session.WorkoutSessionRepository;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.workout.WorkoutPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WeeklyCheckInService {

    private final WeeklyCheckInRepository checkInRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutPlanRepository planRepository;

    public WeeklyCheckInService(WeeklyCheckInRepository checkInRepository,
                                 WorkoutSessionRepository sessionRepository,
                                 WorkoutPlanRepository planRepository) {
        this.checkInRepository = checkInRepository;
        this.sessionRepository = sessionRepository;
        this.planRepository = planRepository;
    }

    @Transactional
    public WeeklyCheckInDto submitCheckIn(CurrentUser currentUser, SubmitCheckInRequest request) {
        LocalDate weekStart = currentWeekStart();
        WeeklyCheckIn checkIn = checkInRepository
                .findByUserIdAndWeekStart(currentUser.id(), weekStart)
                .orElseGet(() -> new WeeklyCheckIn(currentUser.id(), weekStart));

        checkIn.update(
                request.weightKg(),
                request.sleepQualityRating(),
                request.energyRating(),
                request.stressRating(),
                request.painStatus(),
                request.notes()
        );
        return WeeklyCheckInDto.from(checkInRepository.save(checkIn));
    }

    @Transactional(readOnly = true)
    public List<WeeklyCheckInDto> getHistory(CurrentUser currentUser) {
        return checkInRepository.findAllByUserIdOrderByWeekStartDesc(currentUser.id())
                .stream()
                .map(WeeklyCheckInDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressStatsDto getStats(CurrentUser currentUser) {
        return getStatsForUser(currentUser.id());
    }

    /** Same as getStats, but for an explicit user — used by the trainer portal
     *  dashboard to summarize a client's adherence/streak, after ownership has
     *  already been verified. */
    @Transactional(readOnly = true)
    public ProgressStatsDto getStatsForUser(UUID userId) {
        List<WorkoutSession> allSessions = sessionRepository
                .findAllByUserIdAndStatusOrderByStartedAtDesc(userId, SessionStatus.COMPLETED);

        int totalAllTime = allSessions.size();

        LocalDate thisWeekStart = currentWeekStart();
        int workoutsThisWeek = (int) allSessions.stream()
                .filter(s -> !toLocalDate(s).isBefore(thisWeekStart))
                .count();

        int streak = computeStreak(allSessions);
        int adherence = computeAdherence4Weeks(userId, allSessions);

        List<WeeklyCheckInDto> history = checkInRepository
                .findAllByUserIdOrderByWeekStartDesc(userId)
                .stream()
                .map(WeeklyCheckInDto::from)
                .toList();

        return new ProgressStatsDto(totalAllTime, workoutsThisWeek, streak, adherence, history);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static LocalDate currentWeekStart() {
        return LocalDate.now(ZoneOffset.UTC)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static LocalDate toLocalDate(WorkoutSession session) {
        return session.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private int computeStreak(List<WorkoutSession> sessions) {
        if (sessions.isEmpty()) return 0;

        // Group sessions by the Monday of their week
        NavigableMap<LocalDate, Long> countByWeek = sessions.stream().collect(
                Collectors.groupingBy(
                        s -> toLocalDate(s).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        TreeMap::new,
                        Collectors.counting()));

        // Walk backwards from the most recent week that had a session
        LocalDate cursor = countByWeek.lastKey();
        int streak = 0;
        while (countByWeek.getOrDefault(cursor, 0L) > 0) {
            streak++;
            cursor = cursor.minusWeeks(1);
        }
        return streak;
    }

    private int computeAdherence4Weeks(UUID userId, List<WorkoutSession> sessions) {
        int plannedPerWeek = planRepository.findByUserIdAndIsActiveTrue(userId)
                .map(p -> p.getTrainingDaysPerWeek())
                .orElse(0);
        if (plannedPerWeek == 0) return 0;

        LocalDate fourWeeksAgo = currentWeekStart().minusWeeks(4);
        long completed = sessions.stream()
                .filter(s -> !toLocalDate(s).isBefore(fourWeeksAgo))
                .count();

        int planned = plannedPerWeek * 4;
        return (int) Math.min(100, Math.round((completed * 100.0) / planned));
    }
}
