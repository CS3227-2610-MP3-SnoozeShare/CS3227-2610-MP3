# Dev environment runbook

The dev environment is a Render service (backend), a Neon Postgres branch `dev`, and a Vercel project (frontend). Account-bound steps are done by a human; an agent never enters credentials or tokens.

> **Vercel's Production Branch is `development` until W-DEPLOY.** Do not leave it on `main`.

## Order of steps

Render first, then Vercel, then Render again: the frontend needs the backend URL, and the backend needs the frontend's domain for CORS.

| # | Owner | Step |
|---|---|---|
| 1 | Xiao-Congchen | Render: New > Blueprint from this repo; it reads `render.yaml` (Docker web service on the free plan, branch `development`, build context `backend`, health check `/actuator/health`). Enter the `sync: false` secrets in the dashboard when prompted. |
| 2 | Xiao-Congchen | Neon: create a project and a branch `dev`. On Render set `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and a placeholder `SNOOZESHARE_CORS_ALLOWED_ORIGIN` (for example `http://localhost:3000`). Add the Firebase Admin settings below and deploy. |
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
| `SNOOZESHARE_SECURITY_ENABLED` | Render | `true` |
| `SNOOZESHARE_TRUST_FORWARD_HEADERS` | Render | `true`; the backend then reads the client address from `CF-Connecting-IP` (set by Cloudflare in front of Render) and never from `X-Forwarded-For` |
| `FIREBASE_ENABLED` | Render | `true` |
| `FIREBASE_PROJECT_ID` | Render | Firebase project ID |
| `SNOOZESHARE_HOST_REGISTRATION_CODE` | Render | Privileged registration code, never committed |
| `SNOOZESHARE_AGENT_REGISTRATION_CODE` | Render | Privileged registration code, never committed |
| `GOOGLE_APPLICATION_CREDENTIALS` | Render | Path to the provisioned Firebase Admin service-account JSON |
| `SNOOZESHARE_AI_ENABLED` | Render | `false` until the SoCLaaS values are verified (W-AI-GATEWAY T8); the `prod` profile refuses to start if it is unset |
| `SOCLAAS_BASE_URL` | Render | SoCLaaS base URL ending `/v1`, `https://` only; needed only when AI is enabled |
| `SOCLAAS_API_KEY` | Render | The environment's own SoCLaaS key (development and production keys are separate); set in the dashboard, never committed |
| `SNOOZESHARE_AI_MOCK` | local only | `true` answers AI calls from each feature's mock response; the `prod` profile rejects it |
| `NEXT_PUBLIC_API_BASE_URL` | Vercel | Render public URL |
| `NEXT_PUBLIC_FIREBASE_API_KEY` | Vercel | Firebase browser app configuration |
| `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN` | Vercel | Firebase browser app configuration |
| `NEXT_PUBLIC_FIREBASE_PROJECT_ID` | Vercel | Firebase browser app configuration |
| `NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET` | Vercel | Firebase browser app configuration |
| `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID` | Vercel | Firebase browser app configuration |
| `NEXT_PUBLIC_FIREBASE_APP_ID` | Vercel | Firebase browser app configuration |

With `prod` active, the backend refuses to start if a datasource, CORS, security, Firebase, registration-code, or Admin credential setting is missing, and the error names the property. Without `SPRING_PROFILES_ACTIVE` the `dev` profile applies and startup fails on the missing local database rather than serving.

## AI gateway

Every LLM call in the backend goes through `LlmGateway.run` (package `com.snoozeshare.ai`); role features register an `AiFeature` bean and call it. The gateway adds no endpoint and no table.

- **Variables:** `SNOOZESHARE_AI_ENABLED`, `SOCLAAS_BASE_URL`, `SOCLAAS_API_KEY`, `SNOOZESHARE_AI_MOCK` (table above). The model names and limits have defaults and can be overridden with `SNOOZESHARE_AI_MODEL_FAST`, `_STANDARD`, `_STRONG` and the other `snoozeshare.ai.*` keys.
- **Keys:** development and production use separate SoCLaaS keys. A key goes in the Render dashboard or a gitignored `application-local.yml`; a test (`SecretScanTest`) fails the build if a key-shaped string is committed.
- **Run without SoCLaaS:** locally set `SNOOZESHARE_AI_ENABLED=true` and `SNOOZESHARE_AI_MOCK=true`; each feature answers with its `mockResponse`. With AI disabled every call returns the feature's fallback with code `DISABLED`. The health endpoint is unaffected either way.
- **Rules for role authors:**
  1. Take `userId` and `role` from the authorized principal, never from a request body.
  2. Build ASSISTANT turns only from server-stored history, and put only server-built text in `trustedContext`.
  3. Call `run` outside any transaction; it throws `IllegalStateException` inside one.
  4. Act on `flagged` (the input matched an injection pattern) and on `truncated`.
  5. Render `LlmResult.value` as plain text, never as HTML or Markdown, and return it through a response DTO.
  6. Treat a degraded result as final: do not loop on it.

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

### Authentication (W-AUTH), 2026-10-21

Owner: Xiao-Congchen. The auth variables in the table above were set by hand in the Render and Vercel dashboards.

| Item | Value |
|---|---|
| Backend URL / frontend URL | https://snoozeshare-api.onrender.com / https://cs3227-snoozeshare.vercel.app (Production Branch `development`) |
| Health | HTTP 200, `{"status":"UP"}` (agent check, 2026-10-21) |
| CORS preflight from the frontend origin | Allowed (agent check, 2026-10-21) |
| `GET /api/auth/me` without a token | HTTP 401, `INVALID_TOKEN` (agent check, 2026-10-21) |
| `POST /api/auth/registration-code/validate` with a wrong code | HTTP 403, `INVALID_REGISTRATION_CODE` (agent check, 2026-10-21) |
| Frontend routes `/login`, `/register`, `/forgot-password`, `/guest`, `/host`, `/agent` | HTTP 200 each (agent check, 2026-10-21) |
| Live sign-in | All steps of the Developer Guide § 7 "Web authentication check" were run on the live site by the owner and passed (reported by the owner; the time of the run was not recorded) |

Setup problems met on the way, so they are not repeated:

- **Render:** the first deploy failed with `Error processing condition on ...FirebaseConfig`. The cause is the last `Caused by:` line, `Could not resolve placeholder 'FIREBASE_ENABLED'`: `SNOOZESHARE_SECURITY_ENABLED`, `SNOOZESHARE_TRUST_FORWARD_HEADERS` and `FIREBASE_ENABLED` are set by `render.yaml` only when Render applies the Blueprint, which it did not do for the existing service. Add them by hand. A missing variable always shows as that wrapper, with the variable named at the bottom of the chain.
- **Vercel:** Production Branch must be `development`. While it was `main`, `development` builds were Previews, which the backend's single allowed CORS origin cannot serve, and the stable URL stayed on the old build. **Redeploy** rebuilds the same commit as the deployment it is pressed on, so after changing the setting push a commit that touches `frontend/` rather than redeploying an old deployment.
