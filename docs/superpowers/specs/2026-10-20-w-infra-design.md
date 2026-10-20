# W-INFRA Design

- **Status:** Draft
- **Risk tier:** Medium - adds a backend service, a frontend app and three new dependency stacks, moves the whole desktop tree, and rewrites the CI that gates merges; no authentication, money or personal data is touched.
- **Workstream:** W-INFRA | **Plan:** not yet written
- **Constraints consulted:** C60 (branching and required checks `checkstyle`, `build`), C63/C64/C68 (lite skills), C69 (single instance), C73 (DTO layer, later), C74 (persistence approach deferred), C76-C78 (web re-platform, Postgres + Flyway, profiles), C80 (AI later), Q5 (the operator's answer is SD1; the branch state, not this spec, records it and PROJECT_STATE closes it at fold), § Known Gaps (single instance; no payment gateway), Webapp design §§ 3, 6, 9
- **Reviewed by lenses:** Implementer, Tester, Maintainer, User (round 1, findings applied; round 2 not yet run)

## 1. Problem

**Current behaviour.** The repository is one Gradle build at the root (`build.gradle`, `settings.gradle`, `gradlew`, `config/`, `db/`, `src/`) producing the JavaFX app. `.github/workflows/ci.yml` runs `./gradlew checkstyleMain checkstyleTest` (job `checkstyle`) and `xvfb-run ./gradlew build` (job `build`) from the root. There is no `backend/`, no `frontend/`, no local Postgres, and nothing deployed.

**Desired behaviour.** The mono-repo of Webapp design § 6.1 exists: the desktop app lives under `desktop/` and still builds and tests; `backend/` is a Spring Boot skeleton that connects to Postgres, runs Flyway and answers a health check; `frontend/` is a Next.js skeleton that shows the backend's health; `docker-compose.yml` starts local Postgres; CI checks all three under the unchanged required check names; and a dev environment on Railway and Vercel serves both. Every later workstream (W-AUTH onward) builds on this and none of them has to touch build, CI or hosting plumbing.

Affected: all developers (new layout and commands) and every later workstream.

## 2. Goals

Ten functional and five non-functional requirements; the full list with acceptance criteria is in [requirements.md](2026-10-20-w-infra-design/requirements.md).

**Functional:** F1 desktop moved and green; F2 backend builds; F3 backend health; F4 compose Postgres + Flyway; F5 CORS on application and actuator endpoints; F6 `prod` profile fails fast without its settings; F7 frontend builds and shows backend status; F8 CI covers all components under the existing check names; F9 live dev deployment; F10 documentation.
**Non-functional:** N1 no committed secrets, minimal actuator exposure; N2 pinned versions and lockfile; N3 CI time; N4 Checkstyle clean; N5 component isolation.

## 3. Non-goals

- Authentication, Firebase, security filters, registration - W-AUTH (C70, C79).
- Domain model, services, repositories, JPA or ported JDBC - W-DOMAIN decides persistence (C74); the skeleton adds the Postgres driver, JDBC and Flyway only.
- Any AI or LLM code (C80).
- Porting desktop features, and deleting the desktop code - the desktop stays until the web app reaches parity (SD1).
- Production environment, custom domains, HTTPS beyond what Railway and Vercel provide by default, monitoring - W-DEPLOY.
- Multi-instance deployment (C69, § Known Gaps).
- Dockerizing the frontend - Vercel builds it from source.

## 4. Components

- [Requirements](2026-10-20-w-infra-design/requirements.md) - F1-F10, N1-N5 with acceptance criteria.
- [Data model](2026-10-20-w-infra-design/data-model.md) - no domain data; Flyway baseline, desktop `db/` move.
- [API](2026-10-20-w-infra-design/api.md) - `GET /actuator/health` and CORS preflight.
- [UI](2026-10-20-w-infra-design/ui.md) - the frontend status page.
- [Conventions](2026-10-20-w-infra-design/conventions.md) - layout, versions, config and CI rules.
- [Edge cases](2026-10-20-w-infra-design/edge-cases.md) - E-numbered table.
- [Implementation order](2026-10-20-w-infra-design/implementation-order.md) - tasks T1-T9 and waves.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Q5 resolved: the desktop code and root Gradle build move under `desktop/`; `backend/` and `frontend/` sit beside it; nothing is deleted. Rejected: leave in place at the root (clutters the mono-repo root and invites accidental coupling); delete now (the desktop tests still guard the business rules W-DOMAIN ports) | Operator, in chat 2026-10-20 | Operator |
| SD2 | Independent Gradle builds: `desktop/` and `backend/` each have their own `settings.gradle` and wrapper; there is no root build. Rejected: root multi-project (couples the builds; deleting desktop later would edit the root) | Operator choice | Operator |
| SD3 | The Checkstyle ruleset stays at the repo root `config/checkstyle/checkstyle.xml` and both Gradle builds read it via `../config/checkstyle/checkstyle.xml`, so the ruleset survives the desktop's removal | Avoids a copy that drifts; refines SD1 | Agent, for operator review |
| SD4 | The required checks keep the names `checkstyle` and `build`; each covers all three components. Rejected: per-component jobs (the owner would have to edit branch protection and could merge in the gap) | Operator choice | Operator |
| SD5 | Backend tests use Testcontainers Postgres 17. Rejected: H2 (dialect drift from Neon) | Operator choice | Operator |
| SD6 | W-INFRA includes the live dev deployment (Railway backend, Vercel frontend), not just config; the account-bound steps are listed in a runbook for a human | Operator choice; Done-when in PROJECT_STATE says deployed | Operator |
| SD7 | The backend skeleton has no JPA and no entities; `spring-boot-starter-jdbc`, the Postgres driver and Flyway only | C74 defers the persistence approach | Agent from C74 |
| SD8 | Dev config: `application-dev.yml` is committed and holds only non-secret local defaults (the compose database); secrets later go in a gitignored `application-local.yml` that `application.yml` imports as `optional:`. Refines Webapp design § 6.2 ("application-dev.yml gitignored"), which assumed secrets in that file | A committed file with no secrets lets a fresh clone run | Agent, for operator review |
| SD9 | The hosted "dev" environment tracks the `development` branch: Railway environment `dev` and the Vercel project with Production Branch set to `development`, with a Neon branch `dev` as its database. W-DEPLOY re-points Vercel at `main` | The design has dev and prod; only dev exists in this workstream | Agent, for operator review |
| SD10 | Exit condition: the owner approves and merges the branch after T1-T8; T9 (live deployment) runs on `development` after the merge and is a Handover item, because both hosts track that branch. W-INFRA is `Done` only when the F9 evidence is in the runbook; the owner sets it. Rejected: deploy from the feature branch (a second set of host settings to undo) | The approval gate would otherwise be circular | Agent, for operator review |
| SD11 | The desktop move is one commit of renames plus three named edits (conventions), so it builds. Rejected: a rename-only commit followed by a fix (red at the rename, against AGENTS.md § 6) | Review finding | Agent |
| SD12 | Actuator CORS is configured through `management.endpoints.web.cors.*`, fed from the same property as `CorsConfig`; the MVC mapping alone does not cover the actuator | Review finding | Agent |
| SD13 | Review round 1 findings not applied: none rejected. Items accepted as small scope the operator may strike: F6 (prod fail-fast), the `Re-check` button, `backend/Dockerfile` and `railway.json` | They trace to Webapp design §§ 6 and 9 | Agent, for operator review |
| SD14 | The branch state, not this spec, records the refinement of C77 and the Webapp design § 6.2 by SD8, and lists as fold todos: PROJECT_STATE § How to Resume commands, § Orientation paths, removal of Q5 | Review finding | Agent |
