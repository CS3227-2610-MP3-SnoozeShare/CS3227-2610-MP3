# Dev environment runbook

The dev environment is a Railway service (backend), a Neon Postgres branch `dev`, and a Vercel project (frontend). Account-bound steps are done by a human; an agent never enters credentials or tokens.

> **Vercel's Production Branch is `development` until W-DEPLOY.** Do not leave it on `main`.

## Order of steps

Railway first, then Vercel, then Railway again: the frontend needs the backend URL, and the backend needs the frontend's domain for CORS.

| # | Owner | Step |
|---|---|---|
| 1 | _team member, fill in_ | Railway: create a project and a service from the repo with root directory `backend`, environment `dev`, deploying branch `development`. Railway reads `backend/railway.json` (Dockerfile build, health check `/actuator/health`). |
| 2 | _team member, fill in_ | Neon: create a project and a branch `dev`. On Railway set `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and a placeholder `SNOOZESHARE_CORS_ALLOWED_ORIGIN` (for example `http://localhost:3000`). Deploy. |
| 3 | _team member, fill in_ | Vercel: create a project with root directory `frontend`, Production Branch `development`, and `NEXT_PUBLIC_API_BASE_URL` set to the Railway public URL (no trailing slash). A first deploy without the variable must fail the build. |
| 4 | _team member, fill in_ | Railway: set `SNOOZESHARE_CORS_ALLOWED_ORIGIN` to Vercel's stable production domain (scheme and host, no trailing slash) and redeploy. |
| 5 | _team member, fill in_ | Verify (below) and fill in the evidence block. |

## Variables

| Variable | Where | Value |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Railway | `prod` |
| `SPRING_DATASOURCE_URL` | Railway | Neon JDBC URL ending in `?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Railway | Neon role |
| `SPRING_DATASOURCE_PASSWORD` | Railway | Neon password (set in Railway, never committed) |
| `SNOOZESHARE_CORS_ALLOWED_ORIGIN` | Railway | Vercel production domain, one origin |
| `NEXT_PUBLIC_API_BASE_URL` | Vercel | Railway public URL |

With `prod` active, the backend refuses to start if any of the first five is missing, and the error names the variable. Without `SPRING_PROFILES_ACTIVE` the `dev` profile applies and startup fails on the missing local database rather than serving.

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
