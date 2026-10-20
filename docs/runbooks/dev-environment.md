# Dev environment runbook

The dev environment is a Render service (backend), a Neon Postgres branch `dev`, and a Vercel project (frontend). Account-bound steps are done by a human; an agent never enters credentials or tokens.

> **Vercel's Production Branch is `development` until W-DEPLOY.** Do not leave it on `main`.

## Order of steps

Render first, then Vercel, then Render again: the frontend needs the backend URL, and the backend needs the frontend's domain for CORS.

| # | Owner | Step |
|---|---|---|
| 1 | _team member, fill in_ | Render: New > Blueprint from this repo; it reads `render.yaml` (Docker web service on the free plan, branch `development`, build context `backend`, health check `/actuator/health`). Enter the `sync: false` secrets in the dashboard when prompted. |
| 2 | _team member, fill in_ | Neon: create a project and a branch `dev`. On Render set `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and a placeholder `SNOOZESHARE_CORS_ALLOWED_ORIGIN` (for example `http://localhost:3000`). Deploy. |
| 3 | _team member, fill in_ | Vercel: create a project with root directory `frontend`, Production Branch `development`, and `NEXT_PUBLIC_API_BASE_URL` set to the Render public URL (no trailing slash). A first deploy without the variable must fail the build. |
| 4 | _team member, fill in_ | Render: set `SNOOZESHARE_CORS_ALLOWED_ORIGIN` to Vercel's stable production domain (scheme and host, no trailing slash) and redeploy. |
| 5 | _team member, fill in_ | Verify (below) and fill in the evidence block. |

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

Filled in after the first live deployment (T9).

| Item | Value |
|---|---|
| Backend URL | |
| Frontend URL | |
| UTC timestamp | |
| `curl` output | |
