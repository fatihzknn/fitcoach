# Decisions & Assumptions

A running log of decisions made while building FitCoach, especially where the brief
left a detail open and a reasonable assumption was chosen instead of asking.

## Phase 1 — Foundation

### Architecture
- **Modular monolith.** One Spring Boot deployable; feature packages (`health`, and
  later `auth`, `onboarding`, `workout`, `progress`, `coach`) sit side by side under
  `com.fitcoach`. One PostgreSQL database. No microservices for the MVP.
- **Separate frontend/backend directories** under one repo (`/frontend`, `/backend`),
  with `docker-compose.yml`, `/docs`, and `README.md` at the root, matching the brief.

### Backend
- **Build tool: Maven.** The brief didn't specify; Maven chosen for ubiquity with
  Spring Boot. No Maven wrapper jar is committed in Phase 1 (avoids committing a binary);
  generate one later with `mvn -N wrapper:wrapper`. Requires Maven 3.9+ or an IDE.
- **Spring Boot 3.3.x on Java 21.** Spring Security included now but configured to
  permit all requests — real JWT auth is Phase 2. This keeps the skeleton runnable and
  the health endpoint reachable from the browser.
- **JPA + PostgreSQL wired now, no entities yet.** `ddl-auto: validate` so Hibernate
  never mutates the schema; **Flyway owns the schema.** `V1__init.sql` enables
  `pgcrypto` (for `gen_random_uuid()`) and adds a tiny `app_metadata` marker row so the
  baseline is observable.
- **Two health surfaces:** public `GET /api/health` (consumed by the web client) and
  Spring Actuator's `/actuator/health` (operations/k8s probes). Kept separate on purpose.
- **DTO-only responses + global exception handler.** No entity is ever returned directly;
  errors come back as a uniform `ApiError` with user-friendly messages.

### Frontend
- **Next.js 14 (App Router) + TypeScript strict + Tailwind.** `noUncheckedIndexedAccess`
  and `noImplicitOverride` on for extra safety.
- **shadcn/ui conventions, hand-rolled primitives.** `components.json` is present so
  `npx shadcn@latest add ...` works later, but Phase 1 ships a few primitives by hand
  (Button, Card, Input, Label) to avoid a network/interactive CLI step during scaffolding.
- **Design system:** dark-first graphite canvas with a single energetic "volt-lime"
  accent reserved for the primary action ("Start workout"). Display type is Archivo
  (athletic), body is Inter. Tokens live as HSL CSS variables in `globals.css` (shadcn
  compatible) and are mapped in `tailwind.config.ts`. Large touch targets (≥48px),
  visible focus rings, and reduced-motion support are baked in.
- **PWA-ready, not full PWA.** `manifest.json` + viewport/theme metadata are wired.
  A service worker (e.g. Serwist/next-pwa) is intentionally deferred so the skeleton
  builds cleanly; icons are placeholders.

### Auth & routing in Phase 1 (important)
- Real auth is Phase 2. To make the **redirect infrastructure real and testable now**,
  Phase 1 uses two cookies as a stand-in: `fc_auth` (signed in) and `fc_onboarded`
  (profile complete). `middleware.ts` enforces: `/today` requires both; missing auth →
  `/login`, missing onboarding → `/onboarding`; "/" routes by state.
- The login/register/onboarding screens include clearly-labelled **demo** buttons that
  set these cookies, so a reviewer can walk the full Login → Onboarding → Today flow
  without a backend session. The cookie **names** and the `lib/session.ts` helper
  signatures are the stable contract Phase 2 keeps when swapping in real JWT.

### Conventions
- REST paths are plural, kebab/lowercase, under `/api/*`.
- Local DB credentials default to `fitcoach/fitcoach` purely for a zero-config local run;
  these are **not** secrets and must be overridden via env in any shared/production
  environment. No secret is hard-coded in source.

## Open questions deferred to later phases
- Auth token strategy details (access/refresh lifetimes, storage) — Phase 2.
- Exercise video hosting/source — Phase 3 (field exists on the model).
- Real LLM provider + key handling — Phase 6 (abstraction defined, mock default).

## Phase 2 — Authentication & Onboarding

### Auth
- **Stateless JWT (HS256, JJWT 0.12).** Token carries the user id (subject) + email.
  Lifetime is configurable (`app.jwt.expiration-minutes`, default 24h). The signing
  secret comes from `APP_JWT_SECRET` (min 32 chars) — a clearly-marked local-dev default
  exists in `application.yml`; production must override it. No refresh-token rotation in
  the MVP (single access token); this is the main known auth debt for a later phase.
- **BCrypt** password hashing via the `PasswordEncoder` bean. Login goes through Spring's
  `AuthenticationManager` + a `CustomUserDetailsService`; request-time auth is handled by
  a `JwtAuthenticationFilter` that sets a `CurrentUser` principal.
- **Security posture:** `/api/auth/register`, `/api/auth/login`, `/api/health`, actuator
  health, and Swagger are public; everything else requires a bearer token. A JSON 401 is
  returned for unauthenticated access (no HTML login redirect — this is an API).

### Token storage on the client (decision)
- The JWT is stored in a **cookie** (`fc_auth`), not `localStorage`, specifically so
  Next.js **middleware can read it** for the route guard. Trade-off: a JS-readable cookie
  is not as XSS-hardened as an httpOnly cookie. For the MVP this is acceptable; a
  follow-up can move to an httpOnly cookie set by the backend once SSR auth is introduced.
  `fc_onboarded` remains a routing convenience; the backend `FitnessProfile` is the truth.

### Onboarding
- **One `FitnessProfile` per user** (unique `user_id`). Submitting onboarding is
  idempotent — re-submitting updates the same row. `onboarding_completed_at` marks
  completion and drives the `/today` guard.
- **Validation on both ends.** Backend: Jakarta Bean Validation on `OnboardingRequest`
  (3–5 days; 45/60/75 min via `@AssertTrue`; bounded age/height/weight) plus DB
  `CHECK` constraints. Frontend: per-step guards before advancing.
- **Pain areas:** `NONE` is exclusive — selecting a specific area drops `NONE` and vice
  versa, normalized on both client and server. Stored in a child table
  (`fitness_profile_pain_areas`).
- **Sex** captured as `MALE/FEMALE/OTHER` for baseline estimates (assumption: enough for
  the MVP; not surfaced anywhere sensitive).

### Onboarding UX
- Six steps; single-select steps (goal, background, days, duration) **auto-advance** on
  tap to minimize friction, with a Back control and progress bar. Basics (inputs) and
  pain (multi-select) use an explicit Continue/Finish action.

### Endpoints added
- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
- `POST /api/onboarding`, `GET /api/profile`
- Logout is client-side (clear the token cookie); no server token blacklist in the MVP.

## Phase 3 — Exercise Library & Workout Generation Engine

### Exercise library
- **18 seeded exercises** in `V3__exercise_and_workout.sql`. The required 12 from the spec
  plus 6 for meaningful alternatives (Push-up, Assisted Pull-up, Goblet Squat, Overhead
  Press, Dumbbell Row, Hip Thrust). `videoUrl` is nullable; left null for all seed data —
  will be populated in a later phase once hosting is decided.
- **Alternatives are bidirectional** in the seed data (each exercise also has a back-link
  to its alternatives), consistent with the `@ManyToMany` join table that is not directional
  at the DB level. Applications that display alternatives should deduplicate if needed.
- **Self-referential `@ManyToMany`** used on `Exercise.alternatives` rather than a separate
  `ExerciseAlternative` entity, because no per-link metadata is needed (the link itself is
  the data). A separate entity would be added if "alternative reason" or "ordering" became
  requirements.

### Workout plan generation
- **Pure generation then select-to-save.** `GET /api/plan/options` returns two computed
  plans without writing to the DB; `POST /api/plan/select` re-computes and saves the chosen
  one. The double-computation is intentional — it avoids orphaned draft records if users
  abandon the flow.
- **Only one active plan per user at a time.** Selecting a new plan deactivates the
  previous one (soft-deactivate, not delete, to preserve history for Phase 5).
- **5-day plans show a sustainability warning** in both the DTO and the UI. The warning
  text is: "5 sessions per week is demanding. Ensure 7–8 hours of sleep and listen to
  your body." This is a guardrail, not a block — users can still select the 5-day plan.
- **Goal-tuned rep ranges for Regular + 4-day:** STRENGTH 5–8 / 180s rest; FAT_LOSS
  12–15 / 60s rest; MUSCLE_GAIN and GENERAL_FITNESS 8–12 / 90s rest.
- **Workout day storage:** For alternating plans (e.g. Full Body A/B/A), Day 3 is an
  explicit copy of Day 1 in the DB. This makes the session-by-session plan concrete and
  avoids ambiguity in Phase 4 when logging by day number.
- **RIR guidance** ("2-3 RIR", "3 RIR", "4 RIR") rather than RPE chosen because RIR
  is more immediately actionable for beginners: "leave 2-3 reps in the tank".

### Frontend routing
- **`fc_plan` cookie** added alongside `fc_auth` and `fc_onboarded`. It is a routing
  convenience — set client-side after `POST /api/plan/select` succeeds. The source of
  truth remains the backend `workout_plans` table. If a user clears cookies, they'll be
  redirected to `/plan-selection` and the page will call `GET /api/plan/active` (via the
  API) to render their existing plan; selecting again deactivates the old plan first.
- **Onboarding now redirects to `/plan-selection`** instead of `/today`. `/today` guard
  extended: requires `fc_auth` + `fc_onboarded` + `fc_plan`.

### Tests — Java 26 + Mockito compatibility
- Mockito 5's inline mock maker requires JVM agent access which is restricted in Java 26.
  Fixed by adding `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
  containing `mock-maker-subclass`. This switches to subclass-based mocking (ByteBuddy
  subclassing instead of instrumentation). Limitation: cannot mock final classes or static
  methods — not needed in this codebase.
- `@WebMvcTest` slice tests load `JwtAuthenticationFilter` via component scan even when
  security auto-configuration is excluded (because the filter is a `@Component`). Fixed by
  adding `@MockBean JwtService` to the slice test classes so the filter's dependency is
  satisfied. The filter is not actually invoked because `addFilters = false`.

### Endpoints added
- `GET /api/plan/options` — returns `PlanOptionsResponse { recommended, alternative }`
- `POST /api/plan/select` — body `{ option: "RECOMMENDED" | "ALTERNATIVE" }`, saves plan, returns `WorkoutPlanDto`
- `GET /api/plan/active` — returns active plan or 404

## Phase 6 — AI Coach

- **MockCoachAiProvider is `@Primary` by default.** `OpenAiCoachAiProvider` is conditionally
  activated via `@ConditionalOnProperty(name = "app.openai.api-key")`. This means the mock
  is always used unless the real key is set in environment — safe for development and demos.
- **Single conversation per user.** Each user has at most one `ChatConversation`; history
  accumulates in one thread. Chosen for simplicity over multi-thread; can be extended later.
- **AI context includes:** user name, goal, training background, days/week, pain areas,
  active plan name, last 5 session names, all coach principles, last 10 chat messages.
- **Safety disclaimer** is visible below the chat input and in pain-related responses.
  The AI is explicitly coded to redirect pain/injury questions to healthcare professionals
  and never diagnose or recommend training through pain.
- **OpenAiCoachAiProvider skeleton** throws `UnsupportedOperationException` to prevent
  accidental activation. Completing it requires wiring the OpenAI Java SDK or an HTTP client.

## Phase 7 — Quality & Demo Data

- **DemoDataSeeder** creates user `alex@fitcoach.demo` / `Demo1234!` on startup when
  `APP_SEED_DEMO_DATA=true` (default in local dev). Idempotent — skips if user exists.
  Seeds 13 sessions across 4 weeks and 4 weekly check-ins with realistic weight/wellness data.
  Session timestamps are backdated via a `@Modifying` JPQL update in `WorkoutSessionRepository`
  to simulate historical data for streak/adherence calculations.
- **Check-in nudge timezone:** The Today page computes the current week start in local browser
  time; the backend uses UTC. On systems with UTC offsets that cross midnight on Monday,
  the nudge may appear one day early or late. Acceptable for MVP; fix by storing the user's
  timezone preference or using `Intl.DateTimeFormat` with explicit timezone.
- **V6 migration (hotfix):** V5 created rating columns as `SMALLINT` (int2) but Hibernate
  maps Java `Integer` to `INTEGER` (int4), causing schema-validation failure. V6 alters the
  columns — do not modify V5 (would break Flyway checksums on existing databases).

## Phase 10 — Body Measurements & Sex-Specific BF%

- **Two body-fat formulas, chosen per sex by accuracy vs DEXA:** women use the
  **Body Adiposity Index** (BAI = hip / height^1.5 − 18, r≈0.85), men use the
  **US Navy circumference method** (r≈0.84). Navy was worse for women (r≈0.73);
  BAI is also simpler for women (only hip needed). `Sex.OTHER` falls back to Navy.
- `body_fat_method` (`NAVY`/`BAI`) is stored per measurement (V10) so historical
  values remain honest if formulas change later. V10 back-fills existing rows as NAVY.
- One measurement per user per day (`UNIQUE(user_id, measured_at)`), upsert on save.
- Height comes from the fitness profile — not re-asked on every measurement.

## Distribution — iOS via PWA

- **iOS delivery is a PWA, not an IPA.** Sideloading arbitrary files is not possible
  on iOS; TestFlight would require a paid Apple Developer account. The frontend is
  already PWA-ready (manifest, standalone display, apple-touch-icon); users
  install via Safari → Add to Home Screen.
- Generated brand icons (volt-lime dumbbell on graphite): `icon-192.png`,
  `icon-512.png`, `apple-touch-icon.png` in `frontend/public/`.
- `server.port` now honors the cloud-standard `PORT` env var (Railway/Render),
  falling back to `SERVER_PORT`, then 8080.
- Deployment runbook: `docs/deployment.md` (Neon + Railway + Vercel, all free tier).
  Local dev flow is unchanged.

## Phase 6 (partial) — AI Coach grounded in real trainer content

- User supplied a `data/` pipeline (YouTube transcript scraping → Claude-extracted
  evidence cards) covering 6 sources: Ağır Sağlam, Güray Aydın, Jeff Nippard,
  Renaissance Periodization, Dr. Stacy Sims, and a PubMed research corpus.
  The pipeline's *final* aggregation step (`trainer_philosophies.json`, meant to
  replace the 4 seeded `TrainerPhilosophy` rows) was found broken on inspection:
  2 of the richest sources (Stacy Sims — 370 cards, Renaissance Periodization —
  398 cards) were missing entirely, 3 of the 4 entries that did make it were
  missing required fields, and no numeric programming parameters (rep ranges,
  RIR, rest) were extracted anywhere — so it cannot replace the trainer
  philosophy cards used by `WorkoutGenerationService`.
- The intermediate `evidence_cards` layer, however, is well-structured (4486
  claims, each with a domain, a real quote, and a cited source) — decided to use
  this to ground the **AI coach chat** instead. New `evidence_claims` table
  (V11) is populated at startup by `EvidenceDataLoader`, reading bundled JSONL
  (`backend/src/main/resources/coach-data/`, ~2.1MB, copied from the source
  `data/derived/evidence_cards/` — the 199MB `data/` folder itself stays
  untracked and outside the repo).
- **Safety**: `injury_safety` and `safety` domain claims (203+2 rows) are
  excluded at load time, not just at query time — so they can never surface
  through the coach even if retrieval logic changes later. The hardcoded pain
  response in `MockCoachAiProvider` is untouched by this change; a regression
  test (`MockCoachAiProviderTest.painResponse_neverConsultsEvidenceService`)
  asserts the evidence service is never even called for pain-flagged messages.
- `EvidenceCitationService.findOne()` picks randomly from a small pool (top 8)
  rather than always the same row, so repeated questions on the same topic
  don't feel robotic.
- `OpenAiCoachAiProvider` (still an unimplemented skeleton — no API key wired)
  was updated to include the same evidence claims in its system prompt, so the
  real LLM path is grounded too whenever it's implemented.
- Verified end-to-end against local Docker Postgres before considering this
  done: loader logged "4276 evidence claims loaded, 210 skipped" on first boot,
  and a live coach chat request returned a real Dr. Stacy Sims quote on a
  recovery question while a pain question stayed exactly as before.

## Phase 10-continued — Trainer choice now affects exercise selection, not just numbers

- Discussed directly with the user (RAG explainer, "kadınlara özel" section, and
  "is the plan still default regardless of trainer?" — all three questions were
  connected): trainer philosophy previously only changed numeric prescription
  (sets/reps/rest/RIR) via `TemplateParams`; the exercises and day structure were
  byte-identical across all trainers. Fixed via `TrainerExercisePreferences`
  (`com.fitcoach.workout`) — a plain static `Map<slug, Map<canonicalName,
  preferredName>>`, deliberately **not** a Spring bean (`WorkoutGenerationService`
  is instantiated directly in its test, no Spring context).
- **New 5th trainer**: `womens-physiology-focused` ("Women's Physiology Focused")
  — numeric params grounded in real extracted claims from `evidence_claims`
  (Dr. Stacy Sims source: ~80% 1RM sits at 3-5 reps, muscular-endurance work at
  8-10 reps, 2-3x/week frequency) — heavy compound emphasis, deliberately **not**
  a "light weights, toning" caricature. Per CLAUDE.md's non-negotiable rule
  (never imitate an individual real coach's identity/name), the display name is
  synthesized/generic, matching the pattern of the 4 existing trainers — the
  product never says "Dr. Stacy Sims" anywhere user-facing.
- **Real bug caught by a Plan-agent validation pass before implementation**: a
  naive per-slot substitution is unsafe because several templates deliberately
  place multiple members of the same exercise family in one day (e.g. `buildPPL`'s
  "Push" day contains both Barbell Bench Press and Chest Press Machine
  simultaneously). A context-free substitution would silently duplicate an
  exercise within a day. Fixed with `daySlots(...)`, which resolves a whole day's
  slots together against two guards: the day's own literal canonical names, and
  names already resolved-into by earlier slots in the same day. Regression-tested
  by `noDuplicateExerciseWithinAnyDayAcrossAllTrainersAndTemplates` (iterates all
  5 real trainer slugs × every background × 3-5 days) — this test caught a second,
  more subtle version of the same bug class during implementation (the literal
  canonical-name fallback path wasn't being checked against `usedNames`) before
  it ever reached the database.
- Substitution pairs are curated to stay within the same `MovementPattern` bucket
  (compound vs. isolation) — crossing it would silently apply the wrong numeric
  prescription to a substituted exercise. `evidence-based` has no exercise
  overrides at all — its differentiator is frequency/volume, not implement choice.
- Verified end-to-end against local Docker Postgres: `GET /api/trainers` returns
  5 rows; `GET /api/plan/options` for evidence-based vs. womens-physiology-focused
  on the same profile shows real, expected differences (Cable Row → Dumbbell Row,
  Leg Press → Goblet Squat, Dumbbell Bench Press → Barbell Bench Press) with zero
  duplicate exercises in any day, including a day where the substitution correctly
  no-ops because the target already appears elsewhere that day (Lower B: Goblet
  Squat already present, so Leg Press → Goblet Squat is correctly skipped there).
- Frontend required **zero changes** — `plan-selection/page.tsx` already renders
  `trainers.map(...)` generically; the 5th trainer appears automatically.
- Deferred to a later phase (per explicit user agreement): letting users select
  *multiple* trainers at once for a "combined" program (intersection of exercise
  preferences), and linking `EvidenceCitationService` retrieval to the active
  trainer's slug so the AI coach leans on that trainer's real source more heavily
  when relevant.

## Phase 10-continued (2) — Trainer sex-gating + plan-selection personalization UX

Direct user feedback after testing: "kadın seçmediğim halde kadın programını
seçebiliyor olmam doğru değil" (being able to select the women's-physiology
trainer without having a female profile isn't right) — the 5th trainer added
this session was visible/selectable by everyone.

- **`target_sex` column (V13)** on `trainer_philosophies`, nullable — null means
  visible to everyone (the 4 general trainers), otherwise must exactly match
  `FitnessProfile.sex.name()`. Only `womens-physiology-focused` is gated
  (`FEMALE`).
- **Two enforcement layers**, both using a new plain static
  `TrainerVisibility.isVisible(targetSex, userSex)` utility (not an instance
  method on the entity — Mockito mocks don't execute real method bodies by
  default, so entity behavior methods are awkward to unit test with this
  codebase's existing mock-heavy style; a static pure function stays trivially
  testable and matches the `TrainerExercisePreferences` precedent):
  1. `GET /api/trainers` filters the list server-side by the caller's profile
     sex — the UI never shows an inappropriate trainer in the first place.
  2. `WorkoutPlanService.resolveTrainer()` independently re-validates on
     `getPlanOptions`/`selectPlan` and rejects a mismatched `trainerId` with
     404 — defense in depth against direct API calls that bypass the filtered
     list (verified live: a male demo user's direct request for the women's
     trainer's ID returns 404; a female test user's request for the same ID
     succeeds).
  3. If no `FitnessProfile` is found at all (shouldn't happen post-onboarding),
     the trainer list falls back to sex-neutral trainers only, not an error.
- **Plan-selection loading UX**: user also asked for the plan-generation step
  to feel like real personalization is happening rather than an instant swap.
  Added `PersonalizingOverlay` (frontend, `plan-selection/page.tsx`) — a
  full-screen overlay with rotating status text ("Analyzing your profile…",
  "Applying your training philosophy…", "Building your personalized
  program…", "Finalizing your plan…") shown while `selectPlan` is in flight,
  with an enforced ~2.4s minimum display time (`Promise.all` with a timer) so
  it doesn't just flash even when the API responds instantly. Wording was
  kept honest — it describes what genuinely happens (profile + trainer
  selection do drive the deterministic rule engine), not a fabricated "AI is
  analyzing you" claim, consistent with CLAUDE.md's deterministic-generation
  principle.

## Phase 10-continued (3) — Pain-area-aware exercise selection

User asked for "richer onboarding" as the first personalization step, offering
equipment access as an example. **Redirected**: that specific example conflicts
with CLAUDE.md's non-negotiable "do not ask users to enter a list of available
equipment" rule — flagged this to the user rather than building it or silently
substituting something else.

Instead, checking what onboarding data *could* be wired up found a real gap:
`FitnessProfile.painAreas` (Step 6 of onboarding) was collected and stored but
used **nowhere in `WorkoutGenerationService`** — only `CoachService` read it (for
AI coach context). A user reporting knee pain still got Leg Extension with zero
avoidance. Fixed with zero new onboarding questions, zero migration, zero
frontend changes — purely making the generator finally read data it already has.

- **`PainAvoidancePreferences`** (`com.fitcoach.workout`, plain static utility,
  same reasoning as `TrainerExercisePreferences`): a small, deliberately
  conservative table — `KNEE`: Leg Extension → Leg Curl; `LOWER_BACK`: Romanian
  Deadlift → Hip Thrust; `SHOULDER`: Overhead Press → Chest Press Machine.
  `OTHER`/`NONE` have no automated overrides (too unstructured to safely
  automate — same as before, handled by the existing in-session swap flow).
  Only well-established, broadly-cited accommodations were included, not
  clinical judgment calls; each pair stays within the same `MovementPattern`
  bucket so the numeric prescription is never silently wrong.
- **Priority order in `slot()`**: pain avoidance is checked *before* trainer
  preference (safety before style), both reusing the exact same per-day
  collision guard (`canonicalNamesInDay`/`usedNames`) built for trainer-driven
  substitution earlier this session — extracted into a shared `safeCandidate()`
  helper so both sources use identical collision logic.
- Threaded `Set<PainArea> painAreas` through `daySlots()`/`slot()` from
  `profile.getPainAreas()`, already available at every `build*()` call site
  (mechanical addition to the ~43 existing `daySlots(...)` calls, same pattern
  as the earlier `trainer` threading).
- The regression-test class this needed (no duplicate exercise within a day) was
  already built for the trainer-substitution work — extended
  `noDuplicateExerciseWithinAnyDayAcrossAllTrainersAndTemplates` to also iterate
  pain-area combinations. 75/75 backend tests pass.
- Verified end-to-end against local Docker Postgres with three fresh test
  profiles (KNEE/STARTING+3, LOWER_BACK/REGULAR+3, SHOULDER/STARTING+4): each
  showed the correct substitution in the day where it wasn't blocked by the
  collision guard, and correctly *kept* the original exercise in a day where the
  substitute was already present elsewhere that day (e.g. "Upper B" in the
  shoulder-pain plan legitimately keeps Overhead Press because Chest Press
  Machine already appears there) — same intentional no-op behavior documented
  for the trainer-substitution feature.

**Also captured in project memory** (`product_roadmap.md`): the "living/adaptive
plan" direction (progressive-overload nudges, deload detection using
`SetLog`/`WeeklyCheckIn` history) and the trainer/coach-portal idea remain
undesigned, separate future phases.

## Phase — Living plan (Phase 1 of 2): progressive overload + deload nudge

First of the two personalization directions from `product_roadmap` memory — the
trainer/coach portal remains a distinct, later, undesigned phase.

- **Progressive overload prefill** (`workout/[sessionId]/page.tsx`, `SetRow`):
  the weight input already carried over last session's number; now if last
  session's set hit the top of the prescribed rep range, it prefills
  `previousWeight + 2.5kg` instead, with a small badge explaining why. Flat
  +2.5kg, no per-exercise tuning — kept deliberately simple/predictable.
  Frontend-only, no new data (`previous`/`prescribed` were already passed into
  `SetRow`).
- **Deload nudge**: `TrainerPhilosophy.deloadFrequencyWeeks` has been seeded on
  every trainer since Phase 9 but was read nowhere until now.
  `WorkoutPlanService.isDeloadRecommended()` compares weeks elapsed since the
  active plan's `createdAt` against the trainer's frequency; exposed as
  `WorkoutPlanDto.deloadRecommended`, surfaced on Today as a card linking to
  `/coach` (already grounded with real recovery/deload evidence via
  `EvidenceCitationService`).
- **Real bug caught by the new `WorkoutPlanServiceTest`** (this service had zero
  dedicated unit tests before — only exercised indirectly through
  `WorkoutControllerTest`'s full mock): `ChronoUnit.WEEKS.between(Instant,
  Instant)` throws `UnsupportedTemporalType` — `Instant` is a pure timestamp,
  not a date, so calendar units like WEEKS aren't supported on it directly.
  Fixed by going through `ChronoUnit.DAYS` and dividing by 7. Would have thrown
  in production on the very first `GET /api/plan/active` call for any plan with
  a linked trainer.
- Verified end-to-end against local Docker Postgres: backdated a fresh test
  plan's `created_at` by 8 weeks via direct SQL (Evidence-Based's
  `deload_frequency_weeks` = 6) and confirmed `deloadRecommended` flips
  `false → true` exactly at the threshold. 79/79 backend tests pass.

## Phase — Trainer portal, Phase A: role threading through auth

First step of the trainer/coach portal (product_roadmap memory) — shipped
separately from the roster/dashboard work (Phase B) because
`JwtAuthenticationFilter`/`JwtService.parse()` sits in front of *every*
authenticated request in the app; a bug here has the largest possible blast
radius of anything in this feature. Verify this alone before building on it.

- `Role` enum gains `TRAINER` (was `USER`-only). `User`/`RegisterRequest` gain
  a way to create a trainer account (`isTrainer: boolean` on registration —
  a narrow signup toggle, not the `Role` enum directly, since `Role` may grow
  further values later that registration shouldn't expose 1:1).
- `CurrentUser` (the JWT principal used by every `@AuthenticationPrincipal`)
  gained a `role` field — a breaking constructor change with **5 call sites**
  found and fixed (`JwtService`, `DemoDataSeeder`, `WorkoutControllerTest`,
  `WorkoutPlanServiceTest`, `TrainerPhilosophyControllerTest`). A Plan-agent
  validation pass caught this enumeration before implementation — easy to
  miss one and get a confusing compile error instead of a clean list.
- **Backward compatibility**: tokens minted before this deploy have no `role`
  claim. `JwtService.parse()` defaults a missing claim to `Role.USER` — every
  pre-deploy account genuinely was `USER` (TRAINER didn't exist), so this
  exactly reproduces old behavior. Verified via a unit test that hand-builds
  a raw JWT omitting the claim (`JwtServiceTest.tokenMintedBeforeRoleClaimExisted_defaultsToUser`),
  not just asserted in code.
- **Frontend bug caught by the validation pass before it shipped**:
  `app/login/page.tsx` called `api.getActivePlan()` unconditionally after
  login to restore the plan cookie — for a TRAINER account (which never has a
  `FitnessProfile`) this would 404 and silently misroute them through
  `/plan-selection`. Fixed by branching on `res.user.role === "TRAINER"`
  *before* that call.
- `session.ts` gained a parallel `fc_role` cookie (same graceful "missing →
  USER" default as the JWT) so `middleware.ts` can route trainer accounts
  correctly in Phase B without another round-trip to the backend.
- Verified live against local Docker Postgres: fresh USER registration,
  fresh TRAINER registration (`isTrainer:true`), role persists across
  login and `/api/auth/me`, and the pre-existing demo user (created before
  this feature existed) still logs in correctly as `USER`. 82/82 backend
  tests + 11/11 frontend tests pass.

**Next**: Phase B — the `roster` package (invite codes, trainer↔client
links, dashboard, plan assignment). Deliberately not `com.fitcoach.trainer`
(already means *TrainerPhilosophy*, workout methodology templates) or
`com.fitcoach.coach` (already means the AI chat feature) — both would be a
real naming collision, not just a style nitpick.

## Phase — Trainer portal, Phase B: roster domain + trainer frontend

Built on top of Phase A once it was verified stable. New `com.fitcoach.roster`
package (see naming-collision note above).

- **Invite codes, not email.** The project has zero email infrastructure;
  adding one was explicitly declined for this pass. One standing 8-char code
  per trainer (`trainer_invites`, `UNIQUE(trainer_id)`) from an unambiguous
  alphabet (`ABCDEFGHJKMNPQRSTUVWXYZ23456789` — no `0/O`, `1/I/L`), generated
  via `SecureRandom` with retry-on-collision. 30-day expiry; fetching a code
  past expiry silently regenerates it (a trainer should never be shown a dead
  code); a separate explicit "regenerate" action covers "this leaked."
- **Idempotent redemption, multi-trainer clients allowed.** Redeeming an
  already-redeemed code returns the existing link instead of erroring — a
  double-tap shouldn't scare anyone. No uniqueness constraint on `client_id`
  alone in `trainer_clients`, so a client can link to multiple trainers; if
  two trainers both assign plans, last assignment wins (same behavior a
  client already gets from re-selecting their own plan) — not worth a guard
  with no product ask behind it.
- **Two-tier authorization: 403 vs. 404.** A role mismatch (a `USER` hitting
  a trainer-only endpoint, or a `TRAINER` trying to redeem a code — "panel
  only" per the product decision) → new `ForbiddenException` → 403; the
  endpoint's existence isn't sensitive. An ownership mismatch (trainer A
  requesting trainer B's client by guessed UUID) → existing
  `NotFoundException` → 404, matching `WorkoutSessionService
  .requireOwnedSession`'s established precedent exactly rather than
  introducing an inconsistent new pattern.
- **Reused, not duplicated, plan-generation logic.** `WorkoutPlanService` and
  `WeeklyCheckInService` already resolved everything off a plain `UUID`
  internally; extracted `getPlanOptionsForUser`/`selectPlanForUser`/
  `getActivePlanForUser`/`getStatsForUser` (`UUID`-based) alongside the
  original `CurrentUser`-based methods, which now delegate to them. The
  trainer portal calls the `UUID` variants after its own ownership check.
  Sex-based trainer visibility (`TrainerVisibility.isVisible`) is inherited
  for free since it's keyed off the *client's* profile.
- **Near-miss bug caught before shipping**: an early draft of
  `getActivePlanForUser(UUID)` had `getActivePlan(CurrentUser)` delegate to
  it and return `null` instead of throwing. That would have silently changed
  `/api/plan/active`'s contract from a 404 (which `login.tsx`'s `try/catch`
  depends on to redirect to `/plan-selection`) to a 200-with-null-body. Fixed
  by keeping the two methods fully independent instead of one delegating to
  the other.
- **`WeeklyCheckInService` had zero test coverage before this phase** —
  added a baseline `WeeklyCheckInServiceTest` (stats/streak/adherence)
  alongside the extraction, since nothing else would have caught a mistake
  in logic the trainer dashboard now also depends on.
- **Frontend account-type toggle at registration** (`/register`): "Train
  myself" vs. "I'm a trainer" (`isTrainer` on the register request). A
  TRAINER account is **panel-only** — it never sees onboarding, plan
  selection, or workout logging; `middleware.ts` redirects any client route
  to `/trainer` for a TRAINER session and redirects `/trainer/**` back to
  the right home for a client session (`fc_role` cookie, same graceful
  "missing → USER" default as the JWT).
- **`components/plan-picker.tsx`** extracted the "pick a philosophy, see
  recommended vs. alternative plan" UI (`TrainerCard`, `DayRow`, `PlanCard`,
  `PersonalizingOverlay`) out of `plan-selection/page.tsx` into shared,
  prop-driven components, so the trainer's client-detail page
  (`/trainer/clients/[clientId]`) reuses the exact same picker when
  assigning a plan instead of rebuilding it. `plan-selection/page.tsx`'s own
  behavior is unchanged (import-only edit, verified via `tsc`).
- **`/link-trainer`** is the client-side redemption entry point, reachable
  from a small icon in `AppShell`'s header next to sign-out; guarded by
  auth-only (independent of onboarding/plan progress, since a client could
  reasonably want to link a trainer before finishing their own setup).
- Verified end-to-end against local Docker Postgres: registered a trainer
  and a separate client, fetched the trainer's invite code, redeemed it
  (`204`, confirmed idempotent on a second redemption), confirmed the client
  appeared in `GET /api/trainer/clients`, fetched plan options and assigned
  the recommended plan from the trainer side, confirmed it became the
  client's own active plan via `GET /api/plan/active`, confirmed a client
  gets `403` on `/api/trainer/**`, a trainer gets `404` on a guessed/unowned
  client UUID, a `TRAINER` gets `403` redeeming a code, and that
  regenerating an invite code issues a new one. 109/109 backend tests,
  11/11 frontend tests, `tsc`/`next lint` clean.

Both phases of the trainer/coach portal are now complete — the second of the
two personalization directions from the `product_roadmap` memory (the first
being the living-plan work above).
