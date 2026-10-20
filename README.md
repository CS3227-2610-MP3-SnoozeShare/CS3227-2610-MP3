# CS3227-2610-MP3 — SnoozeShare

A production-level app with multiple user roles for a formal setting as part of CS3227's month-long Mini Project 3.

## Project Layout

- [`desktop/`](desktop) — the JavaFX desktop app (`com.snoozeshare`) with its own Gradle build and SQLite mock DB
- [`backend/`](backend) — Spring Boot web API on Postgres (Flyway migrations, health endpoint), own Gradle build
- [`frontend/`](frontend) — Next.js web app (App Router, Tailwind, Shadcn/ui), npm
- [`config/checkstyle/`](config/checkstyle) — Checkstyle ruleset shared by `desktop/` and `backend/`
- [`docker-compose.yml`](docker-compose.yml) — local Postgres for the backend
- [`docs/`](docs) — [User Guide](docs/UserGuide.md), [Developer Guide](docs/DeveloperGuide.md), [Product Backlog](docs/ProductBacklog.md), [Reflections](docs/Reflections.md), [dev-environment runbook](docs/runbooks/dev-environment.md)
- [`logs/`](logs) — per-agent-session interaction logs, named with SGT timestamp and branch

The three components build independently; there is no root build.

## Pinned versions

| Tool | Version |
|---|---|
| Java | 25 (Gradle toolchain) |
| Gradle | 9.7.1 (each project's wrapper) |
| Spring Boot | 3.5.16 |
| Postgres | 17 |
| Node | 22 (`frontend/.nvmrc`; `engine-strict` rejects other majors) |
| Next.js | 15.5.28 |

## Building & Running

Commands are PowerShell; on macOS/Linux use `./gradlew`.

### Desktop

Run on a copy of the mock DB so the committed file is not written:

```shell
cd desktop
New-Item -ItemType Directory -Force build
Copy-Item db/snoozeshare-mock.db build/acceptance.db
$env:SNOOZESHARE_DB_URL='jdbc:sqlite:build/acceptance.db'
.\gradlew run
.\gradlew build
```

### Backend

Needs Docker (local Postgres, and Testcontainers for tests).

```shell
docker compose up -d postgres
cd backend
.\gradlew bootRun
.\gradlew build
```

The `dev` profile is the default and connects to the compose database. If port 5432 is taken, pick another: `$env:POSTGRES_PORT='5433'; docker compose up -d postgres`, with the same variable set when running the backend. Health check: `http://localhost:8080/actuator/health` returns `{"status":"UP"}`. Local secrets go in a gitignored `backend/src/main/resources/application-local.yml`.

On a low-memory machine run `.\gradlew checkstyleMain checkstyleTest` and `.\gradlew test` as separate commands.

### Frontend

```shell
cd frontend
npm ci
npm run dev
npm test
npm run lint
npm run build
```

Copy `.env.example` to `.env.local` to change `NEXT_PUBLIC_API_BASE_URL` (default `http://localhost:8080`). With the backend running, the home page shows `Backend: UP`. Production builds fail unless the variable is set.

## Continuous integration

`checkstyle` and `build` are the required checks; each covers all three components.

## Deployment

See the [dev-environment runbook](docs/runbooks/dev-environment.md).
