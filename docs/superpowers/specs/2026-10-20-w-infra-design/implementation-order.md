# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Move desktop under `desktop/` | Baseline test counts recorded first; one commit of renames plus the allowed edits (Checkstyle path, `ci.yml` working directory and artifact paths, `.gitignore` for all components); the tree builds | F1, N5 |
| T2 | Local Postgres | `docker-compose.yml` per conventions | F4 |
| T3 | Backend skeleton | `backend/` Gradle project and wrapper, Spring Boot app, Flyway `V001`, profiles and runtime keys from api.md, `CorsProperties` validation, Testcontainers base class, health and fail-fast tests, Boot/Java 25 support check | F2, F3, F4, F6, N1, N2, N4 |
| T4 | CORS | `CorsConfig`, actuator CORS keys, tests on both mechanisms | F5 |
| T5 | Frontend skeleton | `frontend/` Next.js + Tailwind + Shadcn `Button`, status page, `env.ts`, `api.ts`, Vitest tests, `.nvmrc`, `.npmrc`, `.env.example`, lockfile | F7, N2 |
| T6 | Backend container | `backend/Dockerfile`, `backend/railway.json` | F9 |
| T7 | CI for all components | `ci.yml` covering desktop, backend (Testcontainers), frontend under `checkstyle` and `build`, step names per conventions | F8, N3 |
| T8 | Documentation and checks | `README.md` rewrite of the run sections with pinned versions, `docs/runbooks/dev-environment.md`, N1 scan, N2 and N5 checks, README walkthrough | F10, N1, N2, N5 |
| T9 | Live dev deployment (human steps, after merge) | Railway `dev` and Vercel projects tracking `development`, Neon branch `dev`, variables set in the stated order, evidence in the runbook | F9 |

T1-T8 are done on the `W-INFRA` branch and are what the owner approves (SD10). T9 runs on `development` after the merge, because both hosts track that branch; it is a Handover item.

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Move desktop | - | T2, T5 | 1 |
| T2 Local Postgres | - | T1, T5 | 1 |
| T3 Backend skeleton | T1, T2 | T5 | 2 |
| T4 CORS | T3 | T5, T6, T7 | 3 |
| T5 Frontend skeleton | - | T1, T2, T3, T4, T6 | 1 |
| T6 Backend container | T3 | T4, T5, T7 | 3 |
| T7 CI for all components | T1, T3, T5 | T4, T6 | 3 |
| T8 Documentation and checks | T4, T6, T7 | - | 4 |
| T9 Live dev deployment | T8 | - | 5 |

Shared-file hazards: T1 owns every `.gitignore` edit and the first `ci.yml` edit; T7 rewrites `ci.yml` after T1 (serialized by the dependency). T3 and T4 both live under `backend/` but touch different files (T4 adds `CorsConfig` and `application.yml` keys; serialized as listed). T7 and T8 edit different files (`ci.yml` vs `README.md`, runbook).
