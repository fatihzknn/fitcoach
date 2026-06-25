package com.fitcoach.seeder;

import com.fitcoach.auth.User;
import com.fitcoach.auth.UserRepository;
import com.fitcoach.auth.jwt.CurrentUser;
import com.fitcoach.checkin.WeeklyCheckIn;
import com.fitcoach.checkin.WeeklyCheckInRepository;
import com.fitcoach.checkin.domain.CheckInPainStatus;
import com.fitcoach.profile.ProfileService;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.Sex;
import com.fitcoach.profile.domain.TrainingBackground;
import com.fitcoach.profile.dto.OnboardingRequest;
import com.fitcoach.session.SetLog;
import com.fitcoach.session.WorkoutSession;
import com.fitcoach.session.WorkoutSessionRepository;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.workout.WorkoutDay;
import com.fitcoach.workout.WorkoutExercise;
import com.fitcoach.workout.WorkoutPlan;
import com.fitcoach.workout.WorkoutPlanRepository;
import com.fitcoach.workout.WorkoutPlanService;
import com.fitcoach.workout.domain.PlanOption;
import com.fitcoach.workout.dto.SelectPlanRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

/**
 * Seeds a fully-populated demo user on startup when app.seed-demo-data=true.
 * Idempotent: skips silently if the demo email already exists.
 *
 * Credentials:
 *   Email:    alex@fitcoach.demo
 *   Password: Demo1234!
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    public static final String DEMO_EMAIL    = "alex@fitcoach.demo";
    public static final String DEMO_PASSWORD = "Demo1234!";

    private final UserRepository              userRepository;
    private final PasswordEncoder             passwordEncoder;
    private final ProfileService              profileService;
    private final WorkoutPlanService          planService;
    private final WorkoutPlanRepository       planRepository;
    private final WorkoutSessionRepository    sessionRepository;
    private final WeeklyCheckInRepository     checkInRepository;

    public DemoDataSeeder(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          ProfileService profileService,
                          WorkoutPlanService planService,
                          WorkoutPlanRepository planRepository,
                          WorkoutSessionRepository sessionRepository,
                          WeeklyCheckInRepository checkInRepository) {
        this.userRepository    = userRepository;
        this.passwordEncoder   = passwordEncoder;
        this.profileService    = profileService;
        this.planService       = planService;
        this.planRepository    = planRepository;
        this.sessionRepository = sessionRepository;
        this.checkInRepository = checkInRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            log.info("Demo user already exists — skipping seed.");
            return;
        }

        // ── 1. User ──────────────────────────────────────────────────────────
        User user = new User(DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "Alex Martinez");
        userRepository.save(user);
        CurrentUser cu = new CurrentUser(user.getId(), DEMO_EMAIL);

        // ── 2. Onboarding ────────────────────────────────────────────────────
        OnboardingRequest onboarding = new OnboardingRequest(
                MainGoal.MUSCLE_GAIN,
                TrainingBackground.REGULAR,
                4,          // 4 days/week → Upper/Lower split
                60,         // 60-min sessions
                28,         // age
                180,        // cm
                80.0,       // kg
                Sex.MALE,
                Set.of(PainArea.NONE)
        );
        profileService.completeOnboarding(cu, onboarding);

        // ── 3. Plan selection ────────────────────────────────────────────────
        planService.selectPlan(cu, new SelectPlanRequest(PlanOption.RECOMMENDED));

        WorkoutPlan plan = planRepository.findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new IllegalStateException("Plan not saved after selection."));
        List<WorkoutDay> days = plan.getDays(); // ordered by dayNumber

        // ── 4. Historical workout sessions ───────────────────────────────────
        // Build ~12 sessions spread over the past 4 weeks
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate thisWeekMon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Pattern: Mon/Tue/Thu/Fri across 4 weeks
        int[][] weekOffsets = {
            {-22, -21, -19, -18},  // 3 weeks ago: Mon/Tue/Thu/Fri
            {-15, -14, -12, -11},  // 2 weeks ago
            { -8,  -7,  -5},       // 1 week ago (3 of 4)
            { -2,  -1},            // this week so far (Mon/Tue)
        };

        int dayIdx = 0;
        for (int[] weekPattern : weekOffsets) {
            for (int dayOffset : weekPattern) {
                WorkoutDay wDay = days.get(dayIdx % days.size());
                dayIdx++;

                WorkoutSession session = new WorkoutSession(user.getId(), plan, wDay);
                // Add set logs for each exercise in this day
                addSetLogs(session, wDay.getExercises(), dayIdx);
                session.complete(null);
                WorkoutSession saved = sessionRepository.save(session);

                // Back-date timestamps
                Instant started = today.plusDays(dayOffset).atTime(9, 0).toInstant(ZoneOffset.UTC);
                Instant completed = started.plusSeconds(50 * 60); // ~50 min session
                sessionRepository.updateTimestamps(saved.getId(), started, completed);
            }
        }

        // ── 5. Weekly check-ins (last 4 weeks) ───────────────────────────────
        seedCheckIn(user.getId(), thisWeekMon.minusWeeks(3), 76.2, 3, 3, 4, CheckInPainStatus.NO_PAIN, "Felt a bit tired but got through it.");
        seedCheckIn(user.getId(), thisWeekMon.minusWeeks(2), 75.8, 4, 4, 3, CheckInPainStatus.NO_PAIN, "Great week! PRs on bench and rows.");
        seedCheckIn(user.getId(), thisWeekMon.minusWeeks(1), 75.5, 4, 5, 2, CheckInPainStatus.NO_PAIN, "Best week so far. Energy levels high.");
        seedCheckIn(user.getId(), thisWeekMon,               75.1, 4, 4, 2, CheckInPainStatus.NO_PAIN, null);

        log.info("");
        log.info("╔══════════════════════════════════════════╗");
        log.info("║           DEMO USER CREATED              ║");
        log.info("╠══════════════════════════════════════════╣");
        log.info("║  Name    : Alex Martinez                 ║");
        log.info("║  Email   : alex@fitcoach.demo            ║");
        log.info("║  Password: Demo1234!                     ║");
        log.info("║  Goal    : Muscle Gain (REGULAR, 4 days) ║");
        log.info("║  Plan    : Upper/Lower split             ║");
        log.info("║  Sessions: 13 completed (4 weeks)        ║");
        log.info("║  Check-ins: 4 weeks of data              ║");
        log.info("╚══════════════════════════════════════════╝");
        log.info("");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void addSetLogs(WorkoutSession session, List<WorkoutExercise> exercises, int sessionIdx) {
        // Base weights per session index (increases slightly to simulate progression)
        double progressFactor = 1.0 + (sessionIdx * 0.01); // 1% progression per session

        for (WorkoutExercise we : exercises) {
            double baseWeight = estimateBaseWeight(we.getExercise().getName());
            int sets = we.getSets();
            int targetReps = (we.getRepRangeMin() + we.getRepRangeMax()) / 2;

            for (int setNum = 1; setNum <= sets; setNum++) {
                // Last set slightly heavier; fatigue shown through slightly fewer reps
                double weight = Math.round(baseWeight * progressFactor * (setNum == sets ? 1.05 : 1.0) / 2.5) * 2.5;
                int reps = setNum == sets ? we.getRepRangeMin() : targetReps;
                int rir = setNum == sets ? 1 : 2;

                SetLog log = new SetLog(session, we, setNum,
                        BigDecimal.valueOf(weight), reps, rir);
                session.addSetLog(log);
            }
        }
    }

    private double estimateBaseWeight(String exerciseName) {
        return switch (exerciseName) {
            case "Barbell Bench Press"    -> 70.0;
            case "Dumbbell Bench Press"   -> 26.0;
            case "Chest Press Machine"    -> 60.0;
            case "Overhead Press"         -> 50.0;
            case "Lat Pulldown"           -> 60.0;
            case "Cable Row"              -> 55.0;
            case "Dumbbell Row"           -> 30.0;
            case "Assisted Pull-up"       -> 40.0;
            case "Romanian Deadlift"      -> 80.0;
            case "Leg Press"              -> 120.0;
            case "Goblet Squat"           -> 24.0;
            case "Leg Curl"               -> 45.0;
            case "Leg Extension"          -> 50.0;
            case "Hip Thrust"             -> 80.0;
            case "Lateral Raise"          -> 10.0;
            case "Biceps Curl"            -> 14.0;
            case "Triceps Pushdown"       -> 25.0;
            default                       -> 30.0;
        };
    }

    private void seedCheckIn(java.util.UUID userId, LocalDate weekStart,
                             double weightKg, int sleep, int energy, int stress,
                             CheckInPainStatus pain, String notes) {
        WeeklyCheckIn ci = new WeeklyCheckIn(userId, weekStart);
        ci.update(BigDecimal.valueOf(weightKg), sleep, energy, stress, pain, notes);
        checkInRepository.save(ci);
    }
}
