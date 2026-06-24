# FitCoach — Project Context & Handoff

> Place this file at the **repository root** (`fitcoach/CLAUDE.md`). Claude Code reads
> `CLAUDE.md` automatically and will use it as standing context for every session.

This document is a complete handoff for an in-progress project. **Phases 1 and 2 are
built and verified running.** Phase 3 is next. Read the "How to continue" and "Working
rules" sections before writing any code.

---

## 1. What this product is

FitCoach is a **personalized AI gym coach**. The whole product is organized around one
question the user sees first when they open the app:

> **"What am I doing today?"**

It understands the user's goals, level, and progress without long forms, then gives them
the workout, lets them log it, swaps exercises they can't do, and coaches them on staying
consistent.

### Non-negotiable product principles
- Assume users train in a **standard commercial gym**. Do **not** ask them to enter a
  list of available equipment.
- If a user can't do an exercise, a machine is occupied, or something hurts → offer
  **alternatives**.
- **Workout generation must be deterministic** — a controlled rule engine over workout
  templates and an exercise library. **Not** random LLM output.
- Use **AI only** for explanations, personalization, coaching chat, and
  alternative-exercise suggestions.
- May synthesize methodologies from multiple real coaches, but must **not** imitate any
  individual coach's identity, voice, or personality.
- **Out of scope for the MVP:** nutrition tracking, social feeds, camera form analysis,
  wearable integrations, coach marketplace.

### Target user
Beginner-to-intermediate; new to the gym or returning after a break. Goals: fat loss,
muscle gain, strength, or general fitness. Advanced periodization / expert programming is
out of scope.

---

## 2. Tech stack

| Layer    | Tech |
|----------|------|
| Frontend | Next.js 14 (App Router), TypeScript **strict**, Tailwind CSS, shadcn-style UI, PWA-ready, Vitest |
| Backend  | Java 21, Spring Boot 3.3.x, Spring Security (JWT), Spring Data JPA, Flyway, REST, OpenAPI/Swagger, JUnit 5 + Mockito |
| Database | PostgreSQL 16 via Docker Compose |

**Architecture:** modular monolith. One Spring Boot deployable; feature packages live
side by side under `com.fitcoach`. One PostgreSQL database. Frontend and backend are
separate directories in one repo.

---

## 3. Repository structure

```text
fitcoach/
├── CLAUDE.md                 # this file
├── README.md
├── docker-compose.yml        # local PostgreSQL (+ optional Adminer under profile "tools")
├── .env.example              # copy to .env
├── docs/
│   └── decisions.md          # running log of decisions/assumptions — KEEP UPDATING THIS
├── backend/
│   ├── pom.xml
│   ├── Dockerfile            # optional; not wired into compose yet
│   └── src/main/java/com/fitcoach/
│       ├── FitcoachApplication.java
│       ├── common/           # BaseEntity, ApiError, GlobalExceptionHandler, Not/ConflictException
│       ├── config/           # SecurityConfig, CorsConfig, OpenApiConfig, JpaConfig
│       ├── health/           # HealthController (+ dto)
│       ├── auth/             # User, UserRepository, AuthService, AuthController,
│       │   │                 #   CustomUserDetailsService, Role, dto/, jwt/
│       │   └── jwt/          # JwtService, JwtAuthenticationFilter, CurrentUser
│       └── profile/          # FitnessProfile, repo, ProfileService, ProfileController,
│                             #   domain/ (enums), dto/
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/     # V1__init.sql, V2__auth_and_profile.sql
└── frontend/
    └── src/
        ├── app/              # login/, register/, onboarding/, today/, layout, page, globals.css
        ├── components/       # app-shell, wordmark, backend-status, ui/ (button,card,input,label,option-card)
        ├── lib/              # api.ts, session.ts, onboarding.ts, utils.ts
        └── middleware.ts     # route guards for /today
```

---

## 4. Current state — what's implemented

### Phase 1 — Foundation & skeleton ✅
Monorepo, runnable Spring Boot backend with `GET /api/health`, Flyway baseline, CORS,
OpenAPI/Swagger, DTO-only responses + global exception handler. Mobile-first Next.js
shell with the design system, all page shells, PWA manifest, and the route guards. A live
"Backend online/offline" indicator on the auth screens is the frontend↔backend
connectivity check.

### Phase 2 — Authentication & onboarding ✅
- **JWT auth** (HS256, JJWT): `POST /api/auth/register`, `POST /api/auth/login`,
  `GET /api/auth/me`. BCrypt hashing, `AuthenticationManager` + `CustomUserDetailsService`
  for login, `JwtAuthenticationFilter` sets a `CurrentUser` principal. Public routes:
  auth, health, swagger; everything else requires a bearer token (JSON 401 otherwise).
- **User profile + onboarding:** `User` and `FitnessProfile` entities (one profile per
  user, audit fields via JPA auditing). `POST /api/onboarding`, `GET /api/profile`.
  Onboarding is idempotent and stamps `onboarding_completed_at`.
- **6-step onboarding flow** (frontend): goal → background → days → session length →
  basics → pain/injury. Single-selects auto-advance; basics & pain use explicit actions.
  `NONE` pain area is exclusive. Validation on both client and server (+ DB CHECKs).
- **Client auth:** JWT stored in the `fc_auth` cookie (so middleware can guard routes);
  `fc_onboarded` is a routing convenience. `/today` greets the authenticated user by name.

### Verification status
- Frontend: `tsc --noEmit` clean, `vitest` 8/8 passing, `next lint` clean.
- Backend: compiles and **boots successfully** (confirmed on the developer machine; it was
  not compilable in the original authoring sandbox due to no Maven Central access).
  Tests exist (`JwtServiceTest`, `AuthServiceTest` via Mockito, health slice test) and run
  without a database — **run `mvn test` and confirm they pass.**

---

## 5. How to run locally

```bash
cd fitcoach
cp .env.example .env
docker compose up -d db            # PostgreSQL on localhost:5432

# Backend → http://localhost:8080  (Swagger: /swagger-ui.html, health: /api/health)
cd backend && mvn spring-boot:run

# Frontend → http://localhost:3000 (second terminal)
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

### Known environment gotcha (already hit once)
If the backend fails on startup with **`FATAL: role "fitcoach" does not exist`**, a second
PostgreSQL (e.g. a local/Homebrew/Postgres.app instance) is also bound to port 5432 and
answers before the Docker container. Fix by either stopping the local Postgres
(`brew services stop postgresql@16` or quit Postgres.app) **or** moving the app DB to
another port: set `POSTGRES_PORT=5433` in `.env`, `docker compose down -v && up -d db`,
and run the backend with `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/fitcoach`.
Use `docker compose down -v` to recreate the volume cleanly (Postgres only creates the
`fitcoach` role the first time its data dir is empty).

---

## 6. Conventions & important implementation rules

- **Never return JPA entities from controllers** — always DTOs (`record`s). Map with
  static `from(...)` factories.
- **User-friendly error messages** via `ApiError` + `GlobalExceptionHandler`. No stack
  traces leak. `NotFoundException` → 404, `ConflictException` → 409, auth → 401, bean
  validation → 400 with field details.
- **All schema changes go through Flyway** (`V3__*.sql`, `V4__*.sql`, …). `ddl-auto` is
  `validate` — Hibernate never mutates the schema; it only validates against it.
- **Entities** extend `BaseEntity` (audit `created_at`/`updated_at`). IDs are `UUID`
  assigned in the constructor.
- **Validate on both ends.** Backend uses Jakarta Bean Validation; frontend guards inputs
  before calling the API.
- **No hard-coded secrets.** Config comes from env with local-dev defaults
  (`APP_JWT_SECRET`, datasource creds). Document any new env var in `.env.example`.
- **REST naming:** `/api/...`, lowercase, consistent.
- **Frontend:** TypeScript strict (`noUncheckedIndexedAccess` is on — handle possibly-
  undefined indexed access). Local state for UI only; all critical data via the backend.
  Use the existing `api` client (`lib/api.ts`) and `session` helper (`lib/session.ts`).
- **Design system** (dark-first): graphite canvas, a single energetic "volt-lime" accent
  reserved for the **primary action**; Archivo display font + Inter body; large touch
  targets (≥48px); visible focus rings; reduced-motion support. Tokens are HSL CSS vars in
  `app/globals.css`, mapped in `tailwind.config.ts`. Keep the main screen uncluttered.
- **Keep `docs/decisions.md` updated** at the end of every phase with assumptions made.

---

## 7. Working rules for continuing (follow exactly)

This project is built **one phase at a time**. For the current phase:

1. Implement only that phase. Do **not** jump ahead.
2. At the end of the phase: (a) briefly list what was completed, (b) list main
   created/changed files, (c) state which tests were run, (d) note known limitations /
   tech debt, (e) state the next phase, then **stop**.
3. The app must remain **runnable** at the end of every phase.
4. If a detail is missing, make a **reasonable assumption** and record it in
   `docs/decisions.md` rather than blocking.
5. Add seed data and tests. Build a testable service layer.

---

## 8. Phase roadmap

- **Phase 1 — Foundation & skeleton** ✅
- **Phase 2 — Authentication & onboarding** ✅
- **Phase 3 — Exercise library & workout generation engine** ⬅️ **NEXT**
- Phase 4 — Today screen & workout logging
- Phase 5 — Progress & weekly check-in
- Phase 6 — AI coach & coach principles
- Phase 7 — Quality, testing & release prep

### Data models still to be created (Phases 3–6)
`Exercise`, `ExerciseAlternative`, `WorkoutPlan`, `WorkoutDay`, `WorkoutExercise`,
`WorkoutSession`, `SetLog`, `BodyMeasurement`, `WeeklyCheckIn`, `CoachPrinciple`,
`ChatConversation`, `ChatMessage`, `PlanChangeReason`. (Audit fields, migrations, DTOs,
and validation for each.)

---

## 9. Phase 3 spec (what to build next)

**Goal:** deterministic workout generation from the user's `FitnessProfile`.

Tasks:
- Create the `Exercise` entity + **seed data**, and `ExerciseAlternative` relationships.
- Create workout templates and a **deterministic** generator (rule engine — no LLM).
- Generate a plan from the profile; show a **recommended main plan plus one alternative**,
  but clearly recommend one (don't present both with equal weight).
- Save the selected plan as the **active plan**.

**Exercise metadata (each exercise must support):** name, primary muscle group, secondary
muscle groups, movement pattern, difficulty level, video URL field, form cue, common
mistake, alternative exercises.

**Initial exercise library (at least):** Barbell Bench Press, Dumbbell Bench Press, Chest
Press Machine, Lat Pulldown, Cable Row, Leg Press, Romanian Deadlift, Leg Curl, Leg
Extension, Lateral Raise, Biceps Curl, Triceps Pushdown.

**Alternative system** considers **both muscle group and movement pattern**, e.g.:
Bench Press → Dumbbell Bench Press / Chest Press Machine / Push-up; Squat → Leg Press /
Goblet Squat / Split Squat; Lat Pulldown → Assisted Pull-up / Machine Row / Cable Row.

**Each generated plan must include:** plan name, goal, weekly training days, a workout
name per day, exercises, sets, rep range, RIR/RPE guidance, rest time, a short form cue,
and exercise alternatives.

**Deterministic template rules (examples):**
- Beginner + 3 days → Full Body A / Full Body B, alternating; limited compounds; prioritize
  form & consistency.
- Beginner + 4 days → Upper / Lower split.
- Regular + 4 days → Upper / Lower or a higher-intensity variation by goal.
- 5 days → Upper / Lower + an extra focus day, and show a **sustainability warning**.

### Phase 4+ (for context, do not build yet)
- **Phase 4:** wire `/today` to the active plan; daily workout selection; start session;
  log sets/weight/reps; show previous performance; exercise-alternative flow with a reason
  prompt (machine occupied / don't know it / causes pain / no access / other); **safe pain
  guidance** (never push through pain; recommend professional evaluation if severe; never
  diagnose); completion flow.
- **Phase 5:** weekly check-in (weight, optional measurements, sleep/energy/stress 1–5,
  % planned workouts done, pain status, notes); trend charts using **weekly averages**, not
  daily noise; adherence rate.
- **Phase 6:** seeded `CoachPrinciple` records (Beginner Training, Progressive Overload,
  Exercise Technique, Training Volume, Recovery, Deload, Injury Safety, Motivation,
  Consistency); chat with a **`CoachAiProvider`** abstraction (`MockCoachAiProvider` now,
  `OpenAiCoachAiProvider` skeleton for later). AI gets full user context. **AI must never**
  diagnose, give definitive treatment, recommend aggressive weight loss or excessive
  volume, or tell users to push through pain.

---

## 10. First action for Claude Code

Start **Phase 3**. Before coding, skim `docs/decisions.md` and the existing `auth`/
`profile` packages to match the established patterns (entity + repository + service +
DTO + controller, Flyway migration, DTO-only responses, validation, tests). Then
implement Phase 3 per Section 9, keep the app runnable, update `docs/decisions.md`, and
stop with the end-of-phase summary described in Section 7.
