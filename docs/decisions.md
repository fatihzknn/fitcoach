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
