package com.fitcoach.checkin;

import com.fitcoach.checkin.dto.ProgressStatsDto;
import com.fitcoach.session.WorkoutSession;
import com.fitcoach.session.WorkoutSessionRepository;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.workout.WorkoutPlan;
import com.fitcoach.workout.WorkoutPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Baseline coverage for streak/adherence math — this service had zero test
 * coverage before the trainer portal started reusing getStatsForUser() for
 * the client dashboard.
 */
@ExtendWith(MockitoExtension.class)
class WeeklyCheckInServiceTest {

    @Mock private WeeklyCheckInRepository checkInRepository;
    @Mock private WorkoutSessionRepository sessionRepository;
    @Mock private WorkoutPlanRepository planRepository;

    @InjectMocks
    private WeeklyCheckInService service;

    private static final UUID USER_ID = UUID.randomUUID();

    private static LocalDate thisWeekMonday() {
        return LocalDate.now(ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** A completed session whose startedAt falls in the given week (Monday + 1 day). */
    private WorkoutSession sessionInWeek(LocalDate weekMonday) {
        WorkoutSession s = mock(WorkoutSession.class);
        Instant startedAt = weekMonday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        when(s.getStartedAt()).thenReturn(startedAt);
        return s;
    }

    @Test
    void noSessionsOrCheckIns_returnsZeroedStats() {
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(List.of());
        when(checkInRepository.findAllByUserIdOrderByWeekStartDesc(USER_ID)).thenReturn(List.of());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());

        ProgressStatsDto stats = service.getStatsForUser(USER_ID);

        assertThat(stats.totalWorkoutsAllTime()).isZero();
        assertThat(stats.workoutsThisWeek()).isZero();
        assertThat(stats.currentStreakWeeks()).isZero();
        assertThat(stats.adherenceRate4Weeks()).isZero();
    }

    @Test
    void streak_countsConsecutiveWeeksBackFromMostRecent() {
        LocalDate week0 = thisWeekMonday();
        List<WorkoutSession> sessions = List.of(
                sessionInWeek(week0),
                sessionInWeek(week0.minusWeeks(1)),
                sessionInWeek(week0.minusWeeks(2))
        );
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(sessions);
        when(checkInRepository.findAllByUserIdOrderByWeekStartDesc(USER_ID)).thenReturn(List.of());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());

        ProgressStatsDto stats = service.getStatsForUser(USER_ID);

        assertThat(stats.currentStreakWeeks()).isEqualTo(3);
        assertThat(stats.totalWorkoutsAllTime()).isEqualTo(3);
    }

    @Test
    void streak_breaksOnGapWeek() {
        LocalDate week0 = thisWeekMonday();
        // A session this week, then a gap (no session week -1), then one 2 weeks ago.
        List<WorkoutSession> sessions = List.of(
                sessionInWeek(week0),
                sessionInWeek(week0.minusWeeks(2))
        );
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(sessions);
        when(checkInRepository.findAllByUserIdOrderByWeekStartDesc(USER_ID)).thenReturn(List.of());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());

        ProgressStatsDto stats = service.getStatsForUser(USER_ID);

        assertThat(stats.currentStreakWeeks()).isEqualTo(1);
    }

    @Test
    void adherence_computesPercentOfPlannedSessionsOverFourWeeks() {
        LocalDate week0 = thisWeekMonday();
        // 4 days/week planned x 4 weeks = 16 planned; 8 completed -> 50%.
        List<WorkoutSession> sessions = List.of(
                sessionInWeek(week0), sessionInWeek(week0),
                sessionInWeek(week0.minusWeeks(1)), sessionInWeek(week0.minusWeeks(1)),
                sessionInWeek(week0.minusWeeks(2)), sessionInWeek(week0.minusWeeks(2)),
                sessionInWeek(week0.minusWeeks(3)), sessionInWeek(week0.minusWeeks(3))
        );
        WorkoutPlan plan = mock(WorkoutPlan.class);
        when(plan.getTrainingDaysPerWeek()).thenReturn(4);
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(sessions);
        when(checkInRepository.findAllByUserIdOrderByWeekStartDesc(USER_ID)).thenReturn(List.of());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));

        ProgressStatsDto stats = service.getStatsForUser(USER_ID);

        assertThat(stats.adherenceRate4Weeks()).isEqualTo(50);
    }

    @Test
    void adherence_cappedAt100Percent() {
        LocalDate week0 = thisWeekMonday();
        // 1 day/week planned x 4 weeks = 4 planned; way more completed -> capped at 100.
        List<WorkoutSession> sessions = List.of(
                sessionInWeek(week0), sessionInWeek(week0), sessionInWeek(week0),
                sessionInWeek(week0.minusWeeks(1)), sessionInWeek(week0.minusWeeks(1))
        );
        WorkoutPlan plan = mock(WorkoutPlan.class);
        when(plan.getTrainingDaysPerWeek()).thenReturn(1);
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(USER_ID, SessionStatus.COMPLETED))
                .thenReturn(sessions);
        when(checkInRepository.findAllByUserIdOrderByWeekStartDesc(USER_ID)).thenReturn(List.of());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.of(plan));

        ProgressStatsDto stats = service.getStatsForUser(USER_ID);

        assertThat(stats.adherenceRate4Weeks()).isEqualTo(100);
    }

    @Test
    void adherence_zeroWhenNoActivePlan() {
        WorkoutSession session = sessionInWeek(thisWeekMonday());
        when(sessionRepository.findAllByUserIdAndStatusOrderByStartedAtDesc(any(), any()))
                .thenReturn(List.of(session));
        when(checkInRepository.findAllByUserIdOrderByWeekStartDesc(USER_ID)).thenReturn(List.of());
        when(planRepository.findByUserIdAndIsActiveTrue(USER_ID)).thenReturn(Optional.empty());

        ProgressStatsDto stats = service.getStatsForUser(USER_ID);

        assertThat(stats.adherenceRate4Weeks()).isZero();
    }
}
