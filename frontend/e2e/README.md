# E2E tests (Playwright)

Real browser tests — clicking, typing, navigating — against the actual running
app (frontend + backend + Postgres), not mocks. This is the first UI-level
test coverage in the project; everything under `src/lib/__tests__` and
`backend/src/test` is unit/slice-level and already covers business-rule edge
cases (BF% formulas, plan-generation rules, adherence math), so these specs
focus on multi-step flows, navigation, and cross-role behavior instead of
re-deriving that.

## Layout

- `e2e/support/api.ts` — thin client for hitting the backend directly (register,
  onboard, select a plan, etc.) so tests can jump straight to the screen under
  test instead of re-driving the UI for setup every time.
- `e2e/support/session.ts` — seeds the `fc_*` cookies the app's own
  `lib/session.ts` reads, to skip UI login.
- `e2e/support/fixtures.ts` — `readyClient` (registered + onboarded + plan
  selected) and `trainerAccount` fixtures used by most specs.
- One spec file per feature area (`auth`, `onboarding`, `plan-selection`,
  `today`, `workout-session`, `check-in`, `measurements`, `progress`, `coach`,
  `link-trainer`, `trainer-roster`, `trainer-client-management`, `messaging`,
  `route-guards`).

## Running

Needs the full stack up: Postgres (Docker), the Spring Boot backend, and the
Next.js dev server (Playwright starts the frontend dev server itself if it
isn't already running; the backend is not auto-started — start it yourself).

```bash
docker compose up -d db          # from the repo root
cd backend && mvn spring-boot:run &   # separate terminal, or & to background it

cd frontend
npm run test:e2e                 # headless run, once
npm run test:e2e:ui              # interactive UI mode
npm run test:e2e:report          # open the last HTML report
```

## Continuous local testing while you develop

`npm run test:e2e:watch` re-runs the whole suite whenever frontend source,
backend Java source, or the specs themselves change. It's a plain file
watcher (`chokidar-cli`) calling `npx playwright test` — **it does not involve
Claude at all**, so leaving it running in a terminal while you work costs
nothing beyond your machine's CPU. Claude's role is to write/update specs as
you add features and to help read failures when you ask, not to be the thing
polling in a loop.

```bash
npm run test:e2e:watch
```

Tests create fresh accounts per run (`uniqueEmail()` in `support/api.ts`), so
re-runs don't collide with each other or with your own manual-testing account
in the same database.
