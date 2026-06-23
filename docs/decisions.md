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
