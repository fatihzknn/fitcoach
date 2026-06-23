# FitCoach

A personalized AI gym coach. Open the app and it answers one question first —
**“What am I doing today?”** — then gives you the workout, lets you log it, swaps
exercises you can’t do, and coaches you through staying consistent.

This repository is built in phases. **Phase 1 (this state)** is the foundation: the
monorepo, a runnable Spring Boot backend with a health endpoint and Flyway, a
mobile-first Next.js shell with the design system and all page shells, and the route
guards that keep `/today` behind onboarding. Real auth, workout generation, logging,
progress, and AI chat arrive in later phases.

## Stack

| Layer    | Tech |
|----------|------|
| Frontend | Next.js 14 (App Router), TypeScript (strict), Tailwind CSS, shadcn-style UI, PWA-ready |
| Backend  | Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Flyway, REST, OpenAPI/Swagger |
| Database | PostgreSQL 16 (via Docker Compose) |

## Repository layout

```text
/frontend           Next.js app (mobile-first PWA)
/backend            Spring Boot modular monolith
/docs               decisions.md and design notes
/docker-compose.yml local PostgreSQL (+ optional Adminer)
/.env.example       copy to .env
```

## Prerequisites

- **Java 21** and **Maven 3.9+** (or an IDE that bundles Maven)
- **Node 20+** and **npm**
- **Docker** + **Docker Compose** (for PostgreSQL)

## Quick start

```bash
# 1. Environment
cp .env.example .env

# 2. Database
docker compose up -d db          # PostgreSQL on localhost:5432
# optional DB UI on http://localhost:8081
# docker compose --profile tools up -d adminer

# 3. Backend  → http://localhost:8080
cd backend
mvn spring-boot:run
#   Health:   http://localhost:8080/api/health
#   Swagger:  http://localhost:8080/swagger-ui.html

# 4. Frontend → http://localhost:3000
cd ../frontend
cp .env.example .env.local        # sets NEXT_PUBLIC_API_BASE_URL
npm install
npm run dev
```

Open http://localhost:3000. You’ll land on **/login** (no session yet). The header on
the auth screens shows a live **Backend online / offline** indicator — that’s the
frontend-to-backend connectivity check.

### Walking the flow

1. **/register** → create an account → you receive a JWT and land on **/onboarding**.
2. **/onboarding** → six quick steps (goal, background, days, session length, basics,
   pain/injuries) → your `FitnessProfile` is saved and you’re sent to **/today**.
3. **/today** → personalized greeting from your account, plus the bottom navigation
   (Today / Progress / Coach). The plan itself is generated in Phase 3.
4. The header sign-out button clears your token and returns you to **/login**.

Visiting `/today` without a token redirects to `/login`; with a token but no completed
onboarding it redirects to `/onboarding` — the route guard working as specified.

## Tests

```bash
# Backend (no database needed — slice + unit tests for health, JWT, and auth)
cd backend && mvn test

# Frontend (unit tests + typecheck)
cd frontend
npm run test
npm run typecheck
```

## API documentation

With the backend running:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Notes & decisions

See [`docs/decisions.md`](docs/decisions.md) for architecture choices and the
assumptions made where the brief left a detail open.

## Roadmap

- **Phase 1 — Foundation & skeleton** ✅
- **Phase 2 — Authentication & onboarding** ✅ (this state)
- Phase 3 — Exercise library & workout generation engine
- Phase 4 — Today screen & workout logging
- Phase 5 — Progress & weekly check-in
- Phase 6 — AI coach & coach principles
- Phase 7 — Quality, testing & release prep
