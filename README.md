# CS3227-2610-MP3 — SnoozeShare

A production-level app with multiple user roles for a formal setting as part of CS3227's month-long Mini Project 3.

## Try the hosted app

Open the current web deployment at [SnoozeShare](https://cs3227-snoozeshare.vercel.app). The
[User Guide](docs/UserGuide.md) explains the Guest, Host, and Support Agent workflows.

## Project Layout

- [`desktop/`](desktop) — the JavaFX desktop app (`com.snoozeshare`) with its own Gradle build and SQLite mock DB
- [`backend/`](backend) — Spring Boot web API on Postgres (Flyway migrations, health endpoint), own Gradle build
- [`frontend/`](frontend) — Next.js web app (App Router, Tailwind, Shadcn/ui), npm
- [`config/checkstyle/`](config/checkstyle) — Checkstyle ruleset shared by `desktop/` and `backend/`
- [`docker-compose.yml`](docker-compose.yml) — local Postgres for the backend
- [`docs/`](docs) — [User Guide](docs/UserGuide.md), [Developer Guide](docs/DeveloperGuide.md), [Product Backlog](docs/ProductBacklog.md), [Reflections](docs/Reflections.md), [dev-environment runbook](docs/runbooks/dev-environment.md)
- [`workflow/`](workflow/README.md) — the agentic-SE workflow records: specs, plans, project state and history, agent session logs, the prompt-tuning kit (see the Developer Guide § 0)

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

## Local development

The hosted site is the primary way to use SnoozeShare. Contributors can run the web app locally
with Docker, the backend, and the frontend:

```shell
docker compose up -d postgres
cd backend
./gradlew bootRun
```

```shell
cd frontend
npm ci
npm run dev
```

The frontend uses `http://localhost:8080` by default; copy `frontend/.env.example` to
`frontend/.env.local` only when changing that API URL. See the
[development environment runbook](docs/runbooks/dev-environment.md) for environment variables,
deployment, and testing details.

The legacy JavaFX component remains under [`desktop/`](desktop) with its own Gradle build; it is
not the primary hosted application.

## Continuous integration

`checkstyle` and `build` are the required checks; each covers all three components.

## Deployment

See the [dev-environment runbook](docs/runbooks/dev-environment.md).
