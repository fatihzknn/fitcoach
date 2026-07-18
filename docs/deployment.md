# FitCoach — Deployment & iOS (PWA) Guide

FitCoach ships to iPhones as a **PWA**: you share a URL, friends open it in Safari and
tap **Share → Add to Home Screen**. The app installs with its own icon and runs
full-screen like a native app. No App Store, no Apple Developer account.

For that to work, both halves must be reachable from the internet:

| Piece | Where | Cost |
|-------|-------|------|
| PostgreSQL | [Neon](https://neon.tech) (or Railway Postgres) | free tier |
| Backend (Spring Boot) | [Railway](https://railway.app) or [Render](https://render.com) | free/hobby tier |
| Frontend (Next.js) | [Vercel](https://vercel.com) | free tier |

---

## 1. Database — Neon

1. Create a project at neon.tech → copy the **connection string**
   (`postgresql://user:pass@host/dbname?sslmode=require`).
2. Nothing else — Flyway creates the whole schema on first backend boot.

## 2. Backend — Railway

1. railway.app → **New Project → Deploy from GitHub repo** → pick `fitcoach`.
2. Settings → **Root Directory**: `backend` (it auto-detects the Dockerfile).
3. Variables — add:

   | Variable | Value |
   |----------|-------|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<neon-host>/<db>?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | from Neon |
   | `SPRING_DATASOURCE_PASSWORD` | from Neon |
   | `APP_JWT_SECRET` | random 32+ chars — `openssl rand -base64 48` |
   | `APP_CORS_ALLOWED_ORIGINS` | `https://<your-app>.vercel.app` (add after step 3) |
   | `APP_SEED_DEMO_DATA` | `true` (demo user) or `false` |

   > Note the `jdbc:` prefix — Neon gives you `postgresql://…`, Spring needs
   > `jdbc:postgresql://…` and the user/password moved to their own variables.

4. Settings → Networking → **Generate Domain** → note the URL, e.g.
   `https://fitcoach-backend-production.up.railway.app`.
5. Check `https://<backend-url>/api/health` returns `{"status":"UP",…}`.

## 3. Frontend — Vercel

1. vercel.com → **Add New → Project** → import the `fitcoach` repo.
2. **Root Directory**: `frontend` (framework auto-detected: Next.js).
3. Environment variable:

   | Variable | Value |
   |----------|-------|
   | `NEXT_PUBLIC_API_BASE_URL` | `https://<backend-url>` (no trailing slash) |

4. Deploy → you get `https://<your-app>.vercel.app`.
5. Go **back to Railway** and set `APP_CORS_ALLOWED_ORIGINS` to that exact URL.

## 4. Install on iPhone

1. Send friends the Vercel URL (WhatsApp/iMessage — link, not a file).
2. They open it in **Safari** (must be Safari, not the in-app browser).
3. **Share button → Add to Home Screen → Add.**
4. FitCoach appears on the home screen with the volt-lime dumbbell icon and opens
   full-screen, standalone.

## Local dev is unchanged

`docker compose up -d db` + `mvn spring-boot:run` + `npm run dev` keep working —
production config comes entirely from env vars, local defaults still apply.

## Gotchas

- **CORS**: if the phone shows "Could not load", the Vercel URL is missing from
  `APP_CORS_ALLOWED_ORIGINS` (exact match, `https://`, no trailing slash).
- **Free-tier cold starts**: Railway/Render hobby dynos sleep; first request can take
  ~30 s. The frontend's "Backend online" indicator will flip once it wakes.
- **Custom domain later**: add it in Vercel, then append it (comma-separated) to
  `APP_CORS_ALLOWED_ORIGINS`.
- **JWT secret**: never reuse the local default in production.
