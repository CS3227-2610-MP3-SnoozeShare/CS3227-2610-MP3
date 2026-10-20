# Edge Cases and Non-Happy Paths

IDs are permanent: never reuse or renumber. Every F requirement and every API error appears in at least one row. Test layer `manual` means checked by review or a recorded walkthrough, not by an automated test.

## Migration of the desktop tree
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E1 | F1 | Tests resolve `db/snoozeshare-mock.db`, `db/schema.sql`, `db/seed-mock-data.sql` or `src/main/...` by relative path | Gradle runs tests with working directory `desktop/`, so the paths resolve; per-class test counts equal the baseline recorded before the move | A moved file silently failing or skipped hides the migrated-DB guard | integration |
| E2 | F1 | A desktop test or script reads `config/checkstyle/` or `docs/` by relative path | None does today (verified by grep); Checkstyle reads `../config/checkstyle/checkstyle.xml`; if a path is found to break it is fixed in the move commit and listed in the Changes line | A path that only worked from the root fails after the move | integration |
| E3 | F1, N5 | Stale root `build/` or `bin/` exists locally | Ignored by git; `.gitignore` covers `desktop/build/` | Stale outputs mask a failing build | manual |
| E4 | F1 | The move commit contains edits beyond those allowed in conventions | Rejected in review; `git diff -M --stat` shows renames at 100% similarity except `desktop/build.gradle` | Reviewers cannot audit a mixed move | manual |
| E5 | F1, F8 | CI artifact paths still say `build/reports/...` after the move | Fixed in T1 to `desktop/build/reports/...`; a deliberately failing test on the branch must produce the artifact | Failure reports silently vanish | manual |

## Backend
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E6 | F3 | Health called with the database up | 200, body exactly `{"status":"UP"}` | Render and the page rely on the exact body | integration |
| E7 | F3 | Database logins blocked (`ALTER ROLE ... NOLOGIN` plus terminating the other backends; a paused container would hang the health query) while the backend runs, then restored | 503 with body exactly `{"status":"DOWN"}` within 10 s of the block; 200 `{"status":"UP"}` within 10 s of the restore | A false UP routes traffic to a broken instance | integration |
| E8 | F3, N1 | `GET /actuator`, `/actuator/info`, `/actuator/env`, `/actuator/beans` | 404 | Information disclosure | integration |
| E9 | F3, N1 | Health requested with `?show-details=always`, `?details=true` and an `Authorization` header | Body is still exactly `{"status":"UP"}` | A secret-bearing URL must not leak | integration |
| E10 | F4 | Context starts with profile `dev` and Postgres not running | `SpringApplication.run` throws within 20 s with the connection error; no retry loop | A developer must see the real cause | integration |
| E11 | F4 | `V001__baseline.sql` (comment only) applied on Postgres 17, then the context restarts | First start records version 001 once; second start applies nothing; history keeps one row | Proves a comment-only migration is accepted and Flyway is idempotent | integration |
| E12 | F4 | `docker compose up -d postgres` with port 5432 in use | Compose reports the conflict; README documents `POSTGRES_PORT` | A second local Postgres is common | manual |
| E13 | F5 | Preflight to `/actuator/health` from the configured origin | 200 with `Access-Control-Allow-Origin` equal to that origin | The browser blocks the page otherwise | integration |
| E14 | F5 | Preflight to `/actuator/health` and to a test-only application endpoint from any other origin, including an `https` variant and a different port (a request origin with a trailing slash is not tested: Spring strips it before comparing, and browsers never send one) | 403, no `Access-Control-Allow-Origin` header | Exact-match origin rule on both CORS mechanisms | integration |
| E15 | F5 | Preflight to the test-only application endpoint from the configured origin | 200 with that origin allowed and `PUT` permitted | `CorsConfig` is what W-AUTH endpoints inherit | integration |
| E16 | F5 | `snoozeshare.cors.allowed-origin` blank, containing `*`, or ending in `/` | Context fails to start; message names `snoozeshare.cors.allowed-origin` | A wildcard defeats the origin rule when credentials arrive in W-AUTH | integration |
| E17 | F6 | Profile `prod` with `SNOOZESHARE_CORS_ALLOWED_ORIGIN` unset | Context fails with a message naming `SNOOZESHARE_CORS_ALLOWED_ORIGIN` | Production never runs on a dev default | integration |
| E18 | F6 | Profile `prod` with any of the three datasource variables unset | Context fails with a message naming an unresolved variable (Spring reports the first unresolved placeholder it meets; no order is promised) | Same | integration |
| E19 | F6 | `SPRING_PROFILES_ACTIVE` unset | The `dev` profile is used (`spring.profiles.default`); on Render this fails fast on the missing local database (E10) rather than serving | An unset profile must never reach production data | integration |
| E20 | F3 | `PORT` unset or set | Server listens on 8080 when unset and on `PORT` when set | Render injects `PORT` | integration |
| E21 | F2, N4 | New backend file violates the shared Checkstyle ruleset | `checkstyleMain` fails the build | Style gate parity with desktop | integration |
| E22 | F2 | `./gradlew build` in `backend/` without Docker | Tests fail with the Testcontainers error; none is skipped | A skipped DB test hides regressions | integration |
| E23 | F2 | `bootJar` and `jar` both enabled | `jar { enabled = false }`, so `build/libs/` has one jar | The Dockerfile copies one known file | integration |

## Frontend
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E24 | F7 | Fetch returns 200 `{"status":"UP"}` | Page shows `Backend: UP` | Core behaviour | unit |
| E25 | F7 | Fetch rejects (network or CORS), times out after 5 s, returns 503, returns non-JSON, or JSON `status` is not `UP` | Page shows `Backend: unreachable` | Every failure must look the same to the user | unit |
| E26 | F7 | Page unmounts while a fetch is in flight | The request is aborted; no state update after unmount; `Re-check` is disabled while loading | No stale or racing results | unit |
| E27 | F7, N2 | `validateEnv()` with `NODE_ENV=production` and the variable unset, malformed, or ending in `/` | Throws `NEXT_PUBLIC_API_BASE_URL is required` (or `... must be an http(s) URL without a trailing slash`); `next.config.ts` calls it, so `npm run build` fails | A build pointing at localhost would deploy silently | unit |
| E28 | N2 | Node other than 22 | `.npmrc` `engine-strict=true` with `engines` `>=22 <23` makes `npm ci` fail | Reproducible builds | manual |

## CI and deployment
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E29 | F8 | Pull request changes only `docs/` | Both `checkstyle` and `build` still run and report (no path filters) | A required check that never reports blocks merging forever | integration |
| E30 | F8 | Any component fails | The job fails and the failing step name starts with the component; later steps do not run | Clear culprit | manual |
| E31 | F8, N3 | `build` job exceeds 15 minutes on the first green run | Recorded in the branch state; the operator decides (raise N3 or cut tests); the job is not split | N3 is a gate on approval | manual |
| E32 | F9 | Render deploy health check fails | Render marks the deployment failed and keeps the previous one (platform behaviour); the runbook lists the variables to check | Broken deploys must not replace working ones | manual |
| E33 | F9 | Vercel builds before `NEXT_PUBLIC_API_BASE_URL` is set | Build fails (E27) | Misconfiguration surfaces at build time | manual |
| E34 | F9, F10 | Account-bound steps in T9 need a human who is unavailable | T9 stays `Blocked — needs human` in the branch state naming the step | Agents never enter credentials | manual |
| E35 | F10 | README command no longer works | The T8 walkthrough runs every README command once on a clean clone; failures are fixed before approval | Docs drift | manual |
| E36 | N1 | A secret appears in a diff | The N1 scan fails review; the secret is rotated, not just removed | History keeps it | manual |
| E37 | N5 | A file in one component references another component's path | Review rejects it; the N5 grep is run in T8 | Components stay deletable | manual |
| E38 | N2 | Unpinned dependency (`latest`, `+`) | Review rejects it; the N2 check runs in T8 | Reproducibility | manual |
