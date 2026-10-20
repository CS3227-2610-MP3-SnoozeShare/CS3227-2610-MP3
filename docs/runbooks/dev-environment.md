# Dev environment runbook

The dev environment is a Render service (backend), a Neon Postgres branch `dev`, and a Vercel project (frontend). Account-bound steps are done by a human; an agent never enters credentials or tokens.

> **Vercel's Production Branch is `development` until W-DEPLOY.** Do not leave it on `main`.

## Order of steps

Render first, then Vercel, then Render again: the frontend needs the backend URL, and the backend needs the frontend's domain for CORS.

| # | Owner | Step |
|---|---|---|
| 1 | Xiao-Congchen | Render: New > Blueprint from this repo; it reads `render.yaml` (Docker web service on the free plan, branch `development`, build context `backend`, health check `/actuator/health`). Enter the `sync: false` secrets in the dashboard when prompted. |
| 2 | Xiao-Congchen | Neon: create a project and a branch `dev`. On Render set `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and a placeholder `SNOOZESHARE_CORS_ALLOWED_ORIGIN` (for example `http://localhost:3000`). Deploy. |
| 3 | Xiao-Congchen | Vercel: create a project with root directory `frontend`, Production Branch `development`, and `NEXT_PUBLIC_API_BASE_URL` set to the Render public URL (no trailing slash). A first deploy without the variable must fail the build. Set the Framework Preset to **Next.js**: if the preset is left on "Other", the build fails with `No Output Directory named "public" found` (`frontend/vercel.json` also pins it). |
| 4 | Xiao-Congchen | Render: set `SNOOZESHARE_CORS_ALLOWED_ORIGIN` to Vercel's stable production domain (scheme and host, no trailing slash) and redeploy. |
| 5 | Xiao-Congchen | Verify (below) and fill in the evidence block. |

## Variables

| Variable | Where | Value |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Render | `prod` |
| `SPRING_DATASOURCE_URL` | Render | Neon JDBC URL ending in `?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Render | Neon role |
| `SPRING_DATASOURCE_PASSWORD` | Render | Neon password (set in Render, never committed) |
| `SNOOZESHARE_CORS_ALLOWED_ORIGIN` | Render | Vercel production domain, one origin |
| `NEXT_PUBLIC_API_BASE_URL` | Vercel | Render public URL |

With `prod` active, the backend refuses to start if any of the first five is missing, and the error names the variable. Without `SPRING_PROFILES_ACTIVE` the `dev` profile applies and startup fails on the missing local database rather than serving.

## Neon connection string

Neon shows a libpq string (`postgresql://USER:PASSWORD@HOST/DB?sslmode=require&channel_binding=require`). The backend needs a JDBC URL and separate credentials, so convert it:

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST/DB?sslmode=require` (no user or password, no `channel_binding`) |
| `SPRING_DATASOURCE_USERNAME` | `USER` |
| `SPRING_DATASOURCE_PASSWORD` | `PASSWORD` |

Use the direct host, not the `-pooler` one: Flyway's migration lock can misbehave through the pooler. A URL that does not start with `jdbc` fails startup with `'url' must start with "jdbc"`.

## Free-tier behaviour

The free Render web service sleeps after about 15 minutes without traffic and takes tens of seconds to wake. The frontend's 5-second check then shows `Backend: unreachable`; click `Re-check` after a minute. It also has limited memory, so `render.yaml` caps the JVM heap with `JAVA_TOOL_OPTIONS`. Check Render's current limits before relying on this.

## Verify

```shell
curl.exe https://<railway-url>/actuator/health
```

Expect `{"status":"UP"}`. The Vercel URL must show `Backend: UP`; its `Re-check` button repeats the check.

## Evidence

First live deployment (T9), 2026-10-20. Owner: Xiao-Congchen (owns the Vercel, Render and Neon accounts).

| Item | Value |
|---|---|
| Backend URL | https://snoozeshare-api.onrender.com |
| Frontend URL | https://cs3227-snoozeshare.vercel.app (Production Branch `development`) |
| UTC timestamp | 2026-10-20 10:34 (agent re-check of the health endpoint; the owner's own check was earlier the same day) |
| `curl` output | HTTP 200, `{"status":"UP","groups":["liveness","readiness"]}` |
| Frontend page | `Backend: UP`, reported by the owner |

The body above does not match F3's exact `{"status":"UP"}`: Render runs on Kubernetes, where Spring Boot adds liveness and readiness probes. `management.endpoint.health.probes.enabled=false` (with `HealthOnKubernetesTest`) fixes this; after the next Render deploy the body is exactly `{"status":"UP"}`.
