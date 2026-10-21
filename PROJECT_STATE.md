# Project State — SnoozeShare

**Living document — the source of truth for this project.** Read it in full before doing
anything, then run `git log --oneline -20` to confirm it still matches reality. On `main` it
changes only through `sync-project-state` (or an owner hotfix) on `development`; on a branch, read it and write
the branch state instead.

- **Iteration:** 2 — iteration 1 (W1–W16 and the MP3-0 fixes) is closed
- **History:** [docs/project-history/](docs/project-history/README.md) — archived iteration 1; not read at session start, open it only to answer "why was X decided?"
- **Phase:** Iteration 2 is re-platforming SnoozeShare from the JavaFX desktop app to a web app (C76); MP3-0, W-INFRA and W-AUTH are done. `development` is a mono-repo: the desktop app under `desktop/`, a Spring Boot skeleton in `backend/` and a Next.js skeleton in `frontend/`, with a live dev deployment; Firebase authentication and role authorization are in place; no domain feature has been ported yet
- **Stack (code on `development` today):** `desktop/`: Java 25, JavaFX 25 (javafx.controls, javafx.fxml), Gradle (application + shadow + checkstyle plugins), SQLite (embedded, `org.xerial:sqlite-jdbc`) via plain JDBC, JUnit 5 + TestFX. `backend/`: Spring Boot 3.5.16 (web, actuator, validation, JDBC, security), Firebase Admin 9.11, Java 25, Gradle 9.7.1, Flyway, PostgreSQL 17 (Testcontainers in tests), Checkstyle. `frontend/`: Next.js 15.5 (App Router), React 19, Tailwind 4, Shadcn/ui, Firebase JS SDK 12, Vitest, Node 22, npm. Hosting: Render (backend, free web service), Vercel (frontend), Neon (database)
- **Stack (target, not yet built):** the ported domain and services, REST controllers per role, and the NUS SOC LLM for AI features, on top of the W-INFRA skeletons. Persistence approach (Spring Data JPA or ported JDBC) is decided in the W-DOMAIN spec (C74). Source: [docs/Webapp-architecture-design.md](docs/Webapp-architecture-design.md)
- **Branch:** `development` — this file describes `development` (the integration and default branch); `main` is the release branch and receives it by pull request. Branches record their changes in `docs/project-state/branch-states/` and `sync-project-state` folds them in after the merge into `development`
- **Method:** TDD-first vertical slices; spec then plan for big changes (`AGENTS.md` § 3); small commits (`AGENTS.md` § 6)
- **Last updated:** 2026-10-21 by Claude Sonnet 5.5 — folded W-AUTH
- **Last verified against repo:** 2026-10-21
- **Developer guide:** `docs/DeveloperGuide.md` documents W1–W16 and the MP3-0 fixes (last full update 2026-10-19) and, since 2026-10-20, the W-INFRA web skeletons and dev deployment (§ 3.2, § 4.17). Residual limitations stay in the guide's Known Limitations and § Known Gaps.

Sections are ordered by how often they are needed: **1–3 say where we are, 4–5 say what the
system is, 6–7 say what not to touch and what is stuck, 8–10 are the record.** Cite sections by
name (`§ Known Gaps`), not by number.

---

## 1. Orientation

SnoozeShare is a Java 25 / JavaFX desktop application for short-term property rentals (CS3227
Mini Project 3, continuing MP2). Three roles — **Guest**, **Host**, **Support Agent** — each have a
role-isolated UI over one domain model, one wallet/ledger, mocked authentication and shared
booking/ticket state machines. It runs as a single process on an embedded SQLite database; there is
no server. **That is the code on `development` today; it is being re-platformed** (C76): the domain model, services and state machines carry over, and the delivery layer becomes a Spring Boot REST API plus a Next.js frontend, with real authentication and AI features. The design is [docs/Webapp-architecture-design.md](docs/Webapp-architecture-design.md); the migration order is in § How to Resume → Upcoming.

| Path | What lives there |
|---|---|
| `docs/superpowers/specs/`, `docs/superpowers/plans/` | Design specs and plans, one pair per feature-level workstream. MP2-era (W1–W16) specs and plans are in the `mp2 archive/` subfolder of each (link it as `mp2%20archive/`); MP3 ones sit at the top level |
| `docs/project-state/done-ledger.md` | The Done ledger for this iteration — every change, newest first |
| `docs/project-state/branch-states/` | One lean state file per in-flight branch; `archive/` holds folded ones |
| `docs/project-history/` | Closed iterations (state, ledger, branch states, `REFRESH.md`). Not the source of truth |
| `docs/DeveloperGuide.md`, `docs/UserGuide.md` | Handover brief for developers; end-user walkthroughs (Jekyll site, published by `jekyll-gh-pages.yml`) |
| `docs/Webapp-architecture-design.md` | The web re-platform design: target architecture, auth, AI integration, deployment, API outline, MP3-D1..D12 and S1..S5, proposed workstream order. A design document, not a spec; each workstream still gets its own spec |
| `docs/SnoozeShare-Architecture-Proposal.md`, `docs/ProductBacklog.md` | Original design rationale and the F0–F12 backlog |
| `docs/MP2-feedback-triage.md` | MP2 grade-report triage that produced MP3-0 |
| `desktop/` | The JavaFX desktop app and its own Gradle build (C81): `src/main/java/com/snoozeshare/` (`domain`, `repository`, `service`, `infra`, `session`, `ui.{common,guest,host,admin}`, `app`, `config`), `src/test`, `db/` and the Gradle wrapper. Paths in § Architecture 4.1–4.8 are relative to it |
| `backend/` | Spring Boot API skeleton (`com.snoozeshare`): health endpoint, `dev`/`prod` profiles, Flyway `V001`–`V002`, CORS, `ProdEnvironmentGuard`, `Dockerfile`, and Firebase token verification, role authorization and registration under `auth/`; independent Gradle build and wrapper |
| `frontend/` | Next.js app: a `SnoozeShare` page with a backend-status check (`Re-check`), `src/lib/env.ts`, the login, register and forgot-password pages, `AuthProvider`, route guards and role shells, Vitest; npm, Node 22 |
| `docker-compose.yml`, `render.yaml`, `docs/runbooks/dev-environment.md` | Local Postgres 17; the Render backend Blueprint; the deployment order, variables and live-environment evidence |
| `config/checkstyle/`, `.github/workflows/` | Checkstyle rules shared by `desktop/` and `backend/`; `ci.yml` (`checkstyle` on every push; `build` on PRs and on pushes to `development` and `main`; each covers desktop, backend and frontend) and the Pages workflow (publishes `docs/` on push to `main`) |
| `desktop/db/schema.sql`, `desktop/db/seed-mock-data.sql`, `desktop/db/snoozeshare-mock.db` | Shared reference DB, committed. **Rebuild the `.db` from the two SQL files after any seed edit.** Seed timestamps are UTC ISO-8601 ending in `Z`; seed ids are hex UUIDs (prefixes: users `a`/`b`/`c`, tickets `d`, listings `1`, bookings `2`, wallets `3`, transactions `4`, availability `5`, audit `6`, categories `7`, reviews `8`). The `.db` ships migrated (schema_history through v8); `CommittedMockDbTest` guards that. Running the app with no `SNOOZESHARE_DB_URL` opens and **writes** it, so run against a copy |
| `.claude/skills/`, `.claude/agents/` | Project skills (state, docs, lite spec/plan/execute workflow) and the read-only `lite-reviewer` agent |
| `logs/` | Agent session logs `YYYY-MM-DD_HH-mm-ss_<branch>.md` (SGT) |

---

## 2. How to Resume

```bash
cd desktop;  .\gradlew build      # compile, Checkstyle, all tests (also test, run)
cd backend;  .\gradlew build      # needs Docker (Testcontainers); run checkstyle and test separately on a low-memory machine
cd frontend; npm ci; npm test; npm run lint; npm run build   # Node 22; build needs NEXT_PUBLIC_API_BASE_URL
docker compose up -d postgres     # local Postgres 17 for `cd backend; .\gradlew bootRun`
```

Desktop `run` is best on a copy of the DB: set `SNOOZESHARE_DB_URL=jdbc:sqlite:build/acceptance.db`. There is no root build (C81). The `checkstyle` and `build` status checks keep their names and each covers all three components (C83). The README has the full commands and `docs/runbooks/dev-environment.md` the deployment.

**Upcoming** — planned workstreams and the todos handed over at each sync, in priority order.
**The first row is the next action.** Remove a row when it is dropped, or when it starts and
becomes a Workstream row. Sessions and in-flight branches are not tracked here: each branch keeps
its own resume point in `docs/project-state/branch-states/`.

| Item | Kind | Context / done when | Source | Added |
|---|---|---|---|---|
| Rewrite the Developer Guide for the web app only | Todo | The owner will rewrite `docs/DeveloperGuide.md` to describe only the web app as it is built, dropping the desktop sections as `desktop/` retires. Web configuration and deployment content (§ 2, § 3.2, § 4.17) goes first. Done when the guide has no desktop-only content | Operator, 2026-10-20 | 2026-10-20 |
| W-DOMAIN | Workstream | Port the domain model and core services; settles the persistence approach (C74) and the entity-versus-record question. After W-AUTH | Webapp design § 9 | 2026-10-20 |
| W-GUEST, W-HOST, W-AGENT | Workstream | The three role feature sets and their Next.js pages. Can run in parallel after W-DOMAIN | Webapp design § 9 | 2026-10-20 |
| W-AI-GUEST, W-AI-HOST, W-AI-AGENT | Workstream | AI features (C80), parallel after their role workstream. Host and Agent features are undecided and the SOC LLM contract is unconfirmed (Q6) | Webapp design § 9 | 2026-10-20 |
| W-SECURITY, W-DEPLOY | Workstream | AI security hardening, then production deployment. After all of the above | Webapp design § 9 | 2026-10-20 |
| Revise the lite skills from the W-INFRA trial | Todo | The first web-app spec, plan and execution used them. Revise `lint.mjs` (done 2026-10-20: Modify rows on files an earlier task creates, rows of Done tasks, a `Post-merge` task status, `GATE:` warnings; still open: close-out heading must carry no `T` number; no check that the index goal matches `requirements.md` IDs) and the templates (review-lens caps done 2026-10-20: two lenses for Low and Medium, three for High, Attacker added dynamically). Findings are in the [W-INFRA archive](docs/project-state/branch-states/archive/2026-10-20-W-INFRA.md) § Lite-skills trial | handed over from W-INFRA | 2026-10-20 |

---

## 3. Workstreams

W1–W16 (auth through guest UI alignment) are `Done` and documented in `docs/DeveloperGuide.md`; their
rows are in the [iteration 1 archive](docs/project-history/iteration-1/PROJECT_STATE.md). Only open work is listed here.

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| MP3-0 | MP2 feedback fixes before new MP3 features (bugs B1–B7, design D1–D6, CI/skills/docs) | Done | [correctness-cluster design](docs/superpowers/specs/2026-10-18-mp3-correctness-cluster-design.md) | [correctness-cluster plan](docs/superpowers/plans/2026-10-18-mp3-correctness-cluster.md) | Merged (PRs #1–#3); CI and Pages verified; required checks set; operator-confirmed 2026-10-19 | Documented 2026-10-19 |
| W-INFRA | Mono-repo skeletons (`desktop/`, `backend/`, `frontend/`), local Postgres, CI for all three, Render/Vercel/Neon dev deployment | Done | [spec](docs/superpowers/specs/2026-10-20-w-infra-design.md) | [plan](docs/superpowers/plans/2026-10-20-w-infra.md) | Merged (PRs #11, #12); dev live on Render + Vercel + Neon; guide confirmed 2026-10-20 | Documented 2026-10-20 |
| W-AUTH | Firebase authentication and role authorization | Done | [spec](docs/superpowers/specs/2026-10-20-w-auth-design.md); [registration pre-validation spec](docs/superpowers/specs/2026-10-21-w-auth-registration-prevalidation-design.md) | [plan](docs/superpowers/plans/2026-10-20-w-auth.md); [registration pre-validation plan](docs/superpowers/plans/2026-10-21-w-auth-registration-prevalidation.md) | Merged (PR #14); owner approval recorded 2026-10-21, after the merge | Awaiting confirmation |

Status vocabulary, used verbatim: `Not started` · `Spec'd` · `Planned` · `Building` ·
`Blocked — needs human` · `In review` · `Done` · `Abandoned`. Guide vocabulary (owned by
`update-documentation`): `—` · `Awaiting confirmation` · `Pending` · `Documented YYYY-MM-DD` · `N/A`.
**Progress is a position, not a diary** (~15 words).

---

## 4. Architecture

> **§ 4.1–4.8 describe the desktop code as merged on `development`.** The web target is [docs/Webapp-architecture-design.md § 3](docs/Webapp-architecture-design.md): REST controllers per role, a security layer (Spring Security and Firebase), the ported service layer, domain, repositories and PostgreSQL, plus an LLM gateway. As a branch builds a web part, it rewrites that area's **Now** in its branch state. Desktop paths in 4.1–4.8 are relative to `desktop/` (C81); the web skeleton is in 4.9 and authentication in 4.10. The layering rules below and their architecture tests (`LayerDependencyTest`, `UiDependencyTest`) apply to the desktop code and are re-decided per area when the web equivalent lands.

```text
ui.guest / ui.host / ui.admin   (JavaFX/FXML, role-isolated, on ui.common)
        │
     service        (interfaces + service.impl; every mutation runs in a TransactionManager transaction)
        │
 domain ┴ infra.events          (records/enums/state machines/policies | in-process bus + PostCommitEvents)
        │
   repository (interfaces; only repository.jdbc.* touches java.sql)
        │
   infra.db (SQLite, TransactionManager, MigrationRunner)
```

Dependencies run strictly downward (`ui → service → domain`, `service → repository → infra.db`).
`LayerDependencyTest` and `UiDependencyTest` enforce the domain/UI boundaries.

Decision rows below carry only the standing decisions; the full text of each is in the
[iteration 1 archive](docs/project-history/iteration-1/PROJECT_STATE.md) § Decisions.

### 4.1 App shell & bootstrap

**Now:** `AppContext` builds the connection, one shared `TransactionManager` (with a strict `PostCommitEvents` listener publishing to the event bus), repositories, services, session and the role-aware `SceneRouter`; its `Clock` is `Clock.systemDefaultZone()` so policies, the completion sweep and the UI agree. `Main` resolves the DB (`SNOOZESHARE_DB_URL`, else the committed mock DB, else in-memory) and loads the login screen then the role shell (min window 1280×800). `Launcher` is the shaded-jar entry point.

| ID | Decision |
|---|---|
| C1 | Package-based modularity in one Gradle module, not JPMS (why: JavaFX + JPMS `opens` friction; ArchUnit-style tests give the same guarantee) |

### 4.2 UI (role-isolated)

**Now:** each role has its own FXML + controller tree under `ui.<role>`; `ui.common` holds the shared auth screen, nav shell, wallet page/dialogs (`ui.common.wallet`), messaging bubbles and `EmptyTableRow`. All shells use the Fall Light theme: `agent-theme.css` (Guest and Agent) and the Host-owned `host-theme.css`. Guest tabs: Search (titled Explore), Trips (three collapsible groups), Messages, Wallet. Host: Listings, Requests, Messages, Wallet, with breadcrumbs. Agent: Disputes, Accounts, Audit Log, Categories, with fixed table headers and `AgentModal` scrim dialogs. Controllers depend only on `service.*`. Date pickers use `GuestVisuals.dateConverter` (`d MMM yyyy`) and `blockPastDates`.

| ID | Decision |
|---|---|
| C2 | `WalletPanelController` and wallet UI live in `ui.common`, not per role |

### 4.3 Service

**Now:** interfaces in `service`, implementations in `service.impl`. Money moves only through `LedgerWriter` inside a `TransactionManager` transaction that also carries the state change and its audit rows (ESCROW hold/refund, payout, dispute settlement, account governance, top-up/withdraw). `BookingServiceImpl` validates stay dates (`StayDatePolicy`), cancels with the refund policy (pending: 100%; confirmed: 100% over 48h before check-in, else 50%) and completes only after the dispute window (`DisputeWindowPolicy`). `DisputeSettlementServiceImpl` settles the whole held escrow two-sided. `AccountGovernanceServiceImpl` suspends/reactivates with a cascade. `AvailabilityServiceImpl` rejects a past host-block start and adds synthetic BOOKING blocks for bookings without a block row. Listing, review and ticket mutations commit atomically with their audit rows.

| ID | Decision |
|---|---|
| C3 | `WalletService` (primitive) and `TransactionService` (rules) stay separate; both write through `LedgerWriter` (narrowed by C39) |
| C7 | No guest-side fee; the only fee is 3% off a host payout |
| C8 | Rejected and host-cancelled bookings refund 100% |
| C34, C36 | W11: Reactivate exists; suspension force-cancels PENDING and not-yet-started CONFIRMED bookings (100% refund) and deactivates a host's listings; reason column `users.suspensionReason` |
| C42 | Listing `baseNightlyRate` must be positive with ≤ 2 decimals (no $0 or sub-cent stays) |
| C48 | Dispute filing stays open through `endDate+7`; auto-completion from `endDate+8` |
| C53 | No date picker accepts a past date, in the UI and the services; the Audit Log filter is excepted |

### 4.4 Domain

**Now:** pure Java (no JavaFX/JDBC): `domain.model`, `domain.enums`, `domain.statemachine` (`BookingStateMachine`, `TicketStateMachine` are the one legality check every role's call goes through; `Role.AGENT` may do `CONFIRMED → COMPLETED`), `domain.settlement` (`SettlementCalculator`: split, `platformFee`/`hostNet` rounded half-up; `EscrowPolicy`) and `domain.policy` (`DisputeWindowPolicy`, `StayDatePolicy`, each built from a `Clock` only).

| ID | Decision |
|---|---|
| C4 | Records are the model passed to view models; no DTO layer |
| C17 | Escrow is held through the 7-day dispute window; an open ticket keeps it held and the agent controls settlement; auto-complete must skip bookings with an open ticket |
| C20 | Every resolution settles the full escrow: guest refund `R` (no fee), host `(escrow − R) × 0.97`; Accept = requested remedy, Reject = 0, Manual = agent figure (supersedes C9, C18) |
| C22 | No Force Cancel/Complete actions; an agent only settles by resolving a ticket |
| C23 | `Role.AGENT` allowed on `CONFIRMED → COMPLETED`; "Stay ended — escrow held" is a derived label, not a status |

### 4.5 Repository & persistence

**Now:** repository interfaces per aggregate; JDBC adapters in `repository.jdbc` with shared `RowMappers`. `JdbcLedgerRepository` reads money rows out of `audit_log` (a row with `walletAdjustment` and `balanceAfter`); there is no `wallet_transactions` table. `MigrationRunner` applies `src/main/resources/db/migration/V001–V008` (V002 has two files: audit trail and booking decision message) and adopts a pre-provisioned DB (the mock DB) by recording the baseline. `TransactionManager` takes a `TransactionListener` (`onBegin`, `afterCommit`, `afterRollback`) and offers `inTransactionUnchecked`. Tables: users (role includes `SYSTEM`), properties, availability_blocks, bookings (`hostDecisionMessage`), wallets (cached `balance`), tickets, ticket_categories, reviews, audit_log, messages/message_reads, booking_messages/booking_message_reads, schema_history.

| ID | Decision |
|---|---|
| C5 | SQLite via `org.xerial:sqlite-jdbc` and hand-rolled `PreparedStatement` DAOs; no JPA |
| C6 | `properties` columns are exactly the operator's field list (see `db/schema.sql`) |
| C40 (W14) | `wallets` table stays (no `users.balance`); the System user has role `SYSTEM` and a real wallet receiving `PLATFORM_FEE` rows |

### 4.6 Events (in-process bus)

**Now:** `InProcessEventBus` gives typed synchronous subscriptions with failure isolation. Wallet events are queued by `LedgerWriter` into `PostCommitEvents` and published by the `TransactionManager` only after commit (discarded on rollback); `PostCommitEvents` is strict in production and has an autocommit mode used only by tests/seeding. Booking, ticket, message and account-status mutations publish after commit; Host calendar changes refresh the controller directly and publish no event.

| ID | Decision |
|---|---|
| C49 | Post-commit publishing lives in `PostCommitEvents`; `TransactionManager` only exposes the listener callbacks |
| C50 | Cloud is a recorded future direction only; seams exist (`Clock`, `EventSink`, idempotent sweep) but no outbox or per-transaction queue |

### 4.6a Messaging

**Now:** `MessageService` keeps persistent per-ticket GUEST and HOST threads (agents read both); resolved tickets are read-only. `BookingConversationService` is a private host↔guest chat per booking, writable from confirmation to check-out + 7 days, never readable by agents. Both back the Host and Guest Messages tabs and publish `MessagePostedEvent` / `BookingMessagePostedEvent` after the insert.

| ID | Decision |
|---|---|
| C37 | Persistent `MessageService` replaces the in-memory one; resolved tickets read-only |
| C38 | Booking-scoped host↔guest chat; agents may not read it, even as dispute evidence |
| C40 (Host) | Hosts cannot file tickets; they only answer in agent-managed threads |

### 4.7 Session

**Now:** `SessionContext`/`MockSessionContext` hold the optional current user and role (login by email, no credential check). Service methods that need an actor take the acting user's id explicitly.

### 4.8 Audit trail

**Now:** `audit_log` is one row per change and the single money ledger. A ticket resolution writes ticket-status, booking-status, guest-wallet and host-wallet rows; a host payout writes `BOOKING_PAYOUT` plus a `PLATFORM_FEE` row to the System wallet when the fee is nonzero. Rows go through `AuditRecord.builder` and `AuditService.record` on the caller's connection, so a state change and its money rows commit or roll back together. `AuditService.search` filters by text (names resolve to ids), a set of action types and an inclusive date range, newest first. The Audit Log tab is read-only with a `TARGET` column.

| ID | Decision |
|---|---|
| C28 | One row per change; typed columns (`walletAdjustment`, `reason`, ids, name snapshots), status-only before/after text |
| C31, C39 | The ledger fold into `audit_log`, executed by W14 (V006–V008); `WalletService`/`TransactionService` kept |

### 4.9 Web platform skeleton (`backend/`, `frontend/`, hosting)

**Now:** one repo, three independent builds (no root build) with a shared Checkstyle ruleset in `config/checkstyle/`. `backend/` is Spring Boot 3.5.16 on Java 25 with Flyway `V001` (a comment-only baseline) and `V002` (auth users and wallets, § 4.10) on PostgreSQL 17. Its health endpoint is `GET /actuator/health`, which returns exactly `{"status":"UP"}` (or `DOWN` with 503); every other actuator path is 404, details and Kubernetes probe groups are off; the `/api/auth/**` endpoints are in § 4.10. Profiles: `dev` is the default (`application-dev.yml`, committed, no secrets, local Postgres, CORS for `http://localhost:3000`; secrets go in a gitignored `application-local.yml`); `prod` has no defaults, and `ProdEnvironmentGuard` fails startup naming any missing `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` or `SNOOZESHARE_CORS_ALLOWED_ORIGIN`, and requires `snoozeshare.security.enabled` and `snoozeshare.firebase.enabled` to be true (§ 4.10). CORS allows exactly one origin (`snoozeshare.cors.allowed-origin`, validated: no wildcard, no trailing slash), configured separately for application endpoints (`CorsConfig`) and the actuator. `server.port` follows `PORT`. `frontend/` is Next.js 15 (App Router): `src/lib/env.ts` is the only reader of `NEXT_PUBLIC_API_BASE_URL`, `next.config.ts` fails a production build without it, and `BackendStatus` shows `Backend: UP` or `unreachable` (5 s timeout, `Re-check` button); the auth pages and providers are in § 4.10. Tests: backend integration tests run on one JVM-wide Testcontainers Postgres 17; frontend uses Vitest. The dev environment tracks `development`: the Render Docker web service (`render.yaml`, free plan, sleeps when idle), the Vercel project (root `frontend`, Framework Preset Next.js, Production Branch `development` until W-DEPLOY) and a Neon branch `dev`. Deployment order and variables are in `docs/runbooks/dev-environment.md`.

| ID | Decision |
|---|---|
| C81 | The desktop code and root Gradle build moved under `desktop/`, with `backend/` and `frontend/` beside it; independent Gradle builds, no root build; the Checkstyle ruleset stays at root `config/checkstyle/`. Resolves Q5 |
| C82 | `application-dev.yml` is committed with non-secret local defaults; secrets go in a gitignored `application-local.yml`. Refines C77 |
| C83 | Required checks keep the names `checkstyle` and `build`, each covering all three components; backend tests use Testcontainers Postgres 17; the live dev deployment is part of W-INFRA, done by a human after merge |
| C85 | Backend hosting is Render (free web service, Docker, `render.yaml`), replacing Railway on cost; Vercel and Neon unchanged. Supersedes the Railway parts of C76 and C77 and of the W-INFRA spec |


### 4.10 Authentication and role authorization (backend `auth/`, frontend providers)

**Now:** W-AUTH initializes Firebase Admin when `FIREBASE_ENABLED=true`, requires security, proxy, and Firebase settings in the prod profile, verifies revocable Firebase ID tokens, establishes a stateless Spring Security principal, resolves the active role from the PostgreSQL `users` row keyed by Firebase UID, applies role-specific URL authorization, and provides transactional profile/wallet registration plus `/api/auth/me`. Registration abuse limits use the direct peer locally and, only when explicitly enabled for the production proxy, the `CF-Connecting-IP` address that Cloudflare overwrites on every request; `X-Forwarded-For` is never read. Email verification status is informational only and does not gate authentication or registration. The frontend validates public Firebase configuration in production, lazily initializes Firebase browser auth, attaches ID tokens to backend requests, maps stable backend errors, logs safe correlated auth failures at the backend boundary, exposes auth screens, and protects role routes with client-side guards that ignore stale profile responses.

| Path | Role |
|---|---|
| `backend/src/main/java/com/snoozeshare/auth/config/FirebaseConfig.java` | Firebase Admin application and credential initialization |
| `backend/src/main/java/com/snoozeshare/auth/security/FirebaseAuthFilter.java` | Verify bearer ID tokens and establish the authenticated principal |
| `backend/src/main/java/com/snoozeshare/auth/security/RoleAuthorizationFilter.java` | Resolve and enforce database role for URL namespaces |
| `backend/src/main/java/com/snoozeshare/auth/controller/AuthController.java` | Registration and current-user endpoints |
| `frontend/src/lib/firebase.ts` | Firebase JS SDK initialization |
| `frontend/src/providers/AuthProvider.tsx` | Client auth state and role/profile loading |
| `frontend/src` protected layouts | Client-side route protection and role redirects through `AuthProvider`; no middleware auth in W-AUTH |

| ID | Decision |
|---|---|
| C86 | Keep roles in PostgreSQL keyed by Firebase UID; do not use Firebase custom claims (why: existing web architecture requires server-owned role data and avoids claim synchronization; [spec](docs/superpowers/specs/2026-10-20-w-auth-design.md)) |
| C87 | Require server-side registration codes for HOST and AGENT; GUEST needs no code (why: preserves role isolation and prevents self-assigning privileged roles; [spec](docs/superpowers/specs/2026-10-20-w-auth-design.md)) |

---

## 5. Conventions

Highest IDs used before this iteration: **W16, C59, D38, Q3** (this iteration so far: C97, D44, Q6). Numbering continues; never reuse. `C11, C28–C31, C37, C39–C41` each have two meanings in the archive; cite the archive section when it matters.

- `ui.*` never imports `repository.*` or `infra.db.*` — only `service.*`. Only `repository.jdbc.*` imports `java.sql.*`.
- `BookingService`/`TicketService` are the only callers of `TransactionService`; UI never calls it, and never calls `WalletService.topUp/withdraw` inside a booking/ticket flow.
- All state transitions go through the shared `*StateMachine`.
- One audit row per change, written by services on the caller's connection through `AuditRecord.builder`.
- Only `LedgerWriter` changes a wallet balance; it never opens a transaction. `wallets.balance` caches the newest `balanceAfter`.
- Wallet events are queued by `LedgerWriter`; never publish `WalletTransactionRecordedEvent` by hand.
- Dispute-window and stay-date rules live only in `domain.policy`.
- Tests that match multi-line source text read it with `testsupport/SourceText.read` (CRLF working copies); tests never hard-code future dates (past dates are rejected).
- Keep commits small and each one green (`AGENTS.md` § 6).
- Specs, plans and their execution use `writing-specs-lite`, `writing-plans-lite` and `executing-plans-lite`; specs and plans are reviewed by the read-only `lite-reviewer` agent after `.claude/skills/writing-specs-lite/scripts/lint.mjs` passes. `brainstorming`, `writing-plans` and `reviewed-plan` only when the operator names them (C63, C64, C68).
- Each component builds from its own directory with its own wrapper (`desktop/`, `backend/`) or `npm` (`frontend/`); the Checkstyle block in each `build.gradle` needs both `configDirectory = file('../config/checkstyle')` and `configFile`, because the suppressions file loads through `${config_loc}`. CI step names start with `desktop:`, `backend:`, `frontend:` or `repo:`.
- `gradlew` is committed with LF endings and mode 755 (`.gitattributes`); a CRLF copy fails in Docker and Linux CI.
- Backend tests extend `PostgresIntegrationTest` (one container for the whole JVM; a per-class container restarts on a new port while Spring reuses the cached context). Tests that start an application programmatically pass overrides as command-line arguments, because `SpringApplicationBuilder.properties()` has the lowest precedence. Platform-dependent behaviour needs a test that simulates the platform (`spring.main.cloud-platform`).
- The `prod` profile has no defaults for database or CORS settings, and `ProdEnvironmentGuard` names a missing variable (Spring's binder otherwise leaves `${VAR}` in place). The health body stays exactly `{"status":"UP"}`.
- Authentication failures return a request ID that is logged with status, safe error code and path; credentials, tokens and registration codes are never logged. Production requires explicit security/Firebase enablement and all Firebase/registration/Admin credential settings. Public Firebase browser configuration is deployment metadata, not a secret.
- Neon's libpq string must be converted to a JDBC URL with separate username and password (see the runbook). `NEXT_PUBLIC_API_BASE_URL` has no trailing slash and `src/lib/env.ts` is its only reader.
- Local traps on Windows: `DOCKER_HOST` alone does not break Testcontainers (it caches `docker.client.strategy` in `~/.testcontainers.properties`; also set `TESTCONTAINERS_DOCKER_CLIENT_STRATEGY` and use `--no-daemon`); running Checkstyle and the Testcontainers tests in one Gradle invocation can exhaust memory, so run them separately; Docker image layers can corrupt after a Docker Desktop crash (`exec format error`), so pull a different tag.
- Branch states keep every `PROJECT_STATE.md` heading in order; a section with nothing to change says `No change.` (Architecture, Conventions and Known Gaps give a one-line reason), and `sync-project-state` checks those claims against the branch diff. Skills say `INT` for the integration branch (here `development`; `maintaining-project-state` § Branch Model). Plan tasks a human does after the merge are `Post-merge` and become Handover items; steps that leave the machine start with `GATE:`.
- A pull request into `development` fails its `build` check when it carries a branch state that is not `Approved` (with the owner's name and date on the `Approved` line) or that has live session rows (`.github/scripts/check-branch-states.sh`, run by the `repo:` step in `ci.yml`). It cannot tell who typed the approval, so it backs up the owner's decision and does not replace it. A pull request with no branch state is not checked.
- A push that changes only `PROJECT_STATE.md`, `AGENTS.md` or `docs/**` does not run CI (`paths-ignore` on push only; pull requests always run), and a push to a branch with an open pull request skips the duplicate push run. Render redeploys only for `backend/**` or `render.yaml` changes, and Vercel skips a deploy with no `frontend/` change.
- **IDs:** `W` workstream, `C` decision, `D` deviation, `Q` question. Allocated only by `sync-project-state`; a branch state uses provisional `~` IDs and never cites one outside itself.
- **Branches never edit this file.** Each writes `docs/project-state/branch-states/<slug>.md`; the owner approves it, merges the branch into `development`, then runs `sync-project-state` there. Do not merge a branch that is not `Approved` or has live session rows. Where a skill says `main`, read `development` (C60).
- **Iterations:** the owner runs `refresh-project-state` by hand, guide first. Agents never start one and never read `docs/project-history/` at session start.
- Agent interaction logs under `logs/` are append-only, one file per session; the logging skill is invoked manually at session end.

---

## 6. Known Gaps & Accepted Limitations

**These are decisions, not a TODO list — do not "helpfully" implement them.**

- **No multi-instance / distributed deployment** — one backend instance serving many users; `PostCommitEvents` is one shared queue by that assumption (C50, C69).
- **Ticket categories are a lookup table, not a rules engine** — revisit only if workflow branching is needed.
- **Booking overlap-check + escrow hold use a `SERIALIZABLE` transaction**, not optimistic locking or a queue.
- **Top-up/withdraw are mocked** — no payment gateway; one would slot in behind `WalletService`.
- **The dev backend sleeps when idle (Render free plan).** The first request after a pause takes tens of seconds, so the page shows `Backend: unreachable` until `Re-check`. Accepted by the owner (C85); revisit in W-DEPLOY.
- **Vercel's Production Branch is `development`** until W-DEPLOY re-points it at `main`.
- **`npm audit` reports 7 vulnerabilities** (eslint-config-next/braces, postcss) with Next 15.5.28, the latest 15.x. Not fixed; revisit in W-SECURITY or W-DEPLOY.
- **Accepted test-suite limits:** `ProdProfileTest` starts its own Postgres containers; the E7 health test blocks the shared container's login role, safe only while tests run sequentially; `gradlew build` repeats the Checkstyle run that CI's `checkstyle` job already does.
- W-DOMAIN still owns domain persistence and wallet operations beyond registration-time provisioning.
- Public code pre-validation remains a validity oracle by design; rate limits and secret strength are the compensating controls.
- Rate limiting is process-local. Forwarded-header trust is off locally and on in production, where the client address comes from `CF-Connecting-IP`; that is only sound while all traffic reaches Render through Cloudflare, so re-check it if the hosting or proxy changes (including W-DEPLOY).
- Host and Agent codes remain reusable configuration values; rotation requires replacing deployment configuration.
- Protected frontend routes use client-side guards; backend role authorization remains authoritative.
- Firebase token revocation is checked on every backend request, coupling request latency and availability to Firebase.

**Explicitly out of scope:** microservices, message brokers, horizontal scaling, JPMS module boundaries, JPA/Hibernate (until the W-DOMAIN spec decides, C74), agent Force Cancel/Complete (C22).

---

## 7. Needs a Human

| ID | What is needed | What it blocks | Raised |
|---|---|---|---|
| Q6 | The NUS SOC LLM API contract (endpoint, auth method, request/response schema), and the Host and Agent AI features (design doc: "to be decided" by Trevor and CongChen respectively) | W-AI-* specs | 2026-10-20 |

---

## 8. Decisions & Context

Standing process decisions. Architecture-shaping ones sit with their area in § Architecture.

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C54 | 2026-10-19 | Branch-state workflow: this file describes the integration branch (`main` when decided, `development` since C60) and changes via `sync-project-state`; branches write `docs/project-state/branch-states/<slug>.md` | Operator adopted the universal skills; the old shared-file workflow collided on IDs | Operator conversation |
| C55 | 2026-10-19 | `refresh-project-state` closes an iteration; run by the owner, guide first, nothing deleted, IDs continue | Operator added the skill | `.claude/skills/refresh-project-state/SKILL.md` |
| C56 | 2026-10-19 | The docs site is published by `.github/workflows/jekyll-gh-pages.yml` with `source: ./docs` (Pages source = GitHub Actions); supersedes the branch/folder deploy of C52 | Operator followed GitHub's recommendation; the template's `./` would have published `AGENTS.md` and this file | Operator conversation |
| C59 | 2026-10-19 | CI: Checkstyle on every push; full build + tests on every PR and on `main`; merging requires both once branch protection is set. Rejected: full build on every branch push; building only on `main` | Operator: Checkstyle for all pushes, build and test for all PRs at least | `.github/workflows/ci.yml` |
| C60 | 2026-10-19 | Branching and CI model: feature branch → `development` (integration, default; direct pushes and PRs allowed) → `main` (release; PR only). `checkstyle` and `build` are required status checks on both. CI runs Checkstyle on every push and the full build on PRs and on pushes to `development` and `main`. This file and the guide are written on `development`, and a branch state is folded after its merge into `development`. Amends C59 (which named `main` only) and C54 (fold after the merge into `development`, not `main`). | Operator: development comes before main; push/merge into either must run checkstyle and build; fold after merger into development | Operator conversation, 2026-10-19; repository rulesets; `.github/workflows/ci.yml` |
| C61 | 2026-10-19 | Lite skills are separate files (`writing-specs-lite`, `writing-plans-lite`) alongside, not replacing, the superpowers-derived `brainstorming` / `writing-plans` | Operator wants a lightweight alternative; replacing would break the existing workflow in AGENTS.md | Operator request |
| C62 | 2026-10-19 | A spec is an index file at the AGENTS.md § 3 path plus a sibling folder of component files (requirements, data model, API, conventions, edge cases, implementation order) | Operator: no single mega-document; keeps the AGENTS.md path valid | Operator spec |
| C63 | 2026-10-19 | The lite skills are the repo defaults for spec and plan; `brainstorming`, `writing-plans`, `reviewed-plan` only when the operator names them. Amends C61 (alongside, not replacing) | Operator: set as defaults moving forward | `AGENTS.md` § 3 |
| C64 | 2026-10-19 | Spec and plan reviews run in dedicated read-only `lite-reviewer` subagents (`.claude/agents/lite-reviewer.md`, tools Read/Grep/Glob), one per lens, reviewing logic first, then seams and integration. A separate agent configuration, not inline prompts. Rejected: self-review by the author agent | Operator | `.claude/agents/lite-reviewer.md` |
| C65 | 2026-10-19 | Plans carry code snippets for tricky logic, schemas and any context-heavy component, not only contracts | Operator | `writing-plans-lite` rule 3 |
| C66 | 2026-10-19 | Spec/plan amendments are recorded in the branch state as Decisions (no `## Amendments` heading in the spec or plan; the index keeps a one-line pointer) | Operator: amendments come from decision changes | `writing-specs-lite` change control |
| C67 | 2026-10-19 | This iteration pivots SnoozeShare from a single-user Java desktop app to a multi-user web app (design: `docs/Webapp-architecture-design.md`). Skills and the reviewer agent are therefore stack-agnostic: they read invariants and commands from `PROJECT_STATE.md`; multi-user networked work has a Medium floor and High for authN/authZ, tenancy, money or personal data | Operator, 2026-10-19 | operator conversation |
| C68 | 2026-10-19 | Added: stable edge-case IDs (`E`), spec/plan templates, `lint.mjs` (deterministic spec and plan checks), a two-round review cap, and `executing-plans-lite` (default for implementing plans) | Operator approved "proceed with all 7" | `.claude/skills/` |
| C69 | 2026-10-20 | The web app is a single backend instance serving many users; multi-instance deployment stays a Known Gap. Amends the wording of C50's gap (no longer "single-process desktop") | Operator: stick to single-instance multi-user (Q4.1) | Operator conversation |
| C70 | 2026-10-20 | Authentication is real, via Firebase Auth (MP3-D12); the "auth is fully mocked" Known Gap is retired. Role authorization is still enforced in the service layer | Operator: reverse, Firebase auth (Q4.5) | [Webapp-architecture-design § 4](docs/Webapp-architecture-design.md) |
| C71 | 2026-10-20 | Audit timestamps are stored in a dedicated timestamp column, not `Instant.toString()` text; the misordering Known Gap is retired | Operator: use a dedicated column (Q4.6) | Operator conversation |
| C72 | 2026-10-20 | Each audit row displays one primary ref: the immediate cause of the change (a reversal caused by a ticket resolution shows the ticket). The other ids stay in the typed columns and remain searchable; whether a detail view shows them is left to the audit-log spec. Retires the `REF`/`TARGET` truncation Known Gap | Operator (Q4.7); the rule was not previously written down | Operator conversation |
| C73 | 2026-10-20 | A separate DTO layer is adopted at the API boundary (MP3-D4): request and response classes, with domain records kept internal. Amends C4 and removes "a separate DTO layer" from out of scope | Operator: reverse (Q4.13); needed from the first endpoint for validation and to control what each role can see | [Webapp-architecture-design § 8](docs/Webapp-architecture-design.md) |
| C74 | 2026-10-20 | JPA/Hibernate stays out of scope for now. The persistence approach (Spring Data JPA with separate entities, ported JDBC, or a hybrid) is decided in the W-DOMAIN spec, where MP3-D2 is recorded as accepted or replaced; C5 stands until then | Operator: do not reverse (Q4.12); the pivot needs Postgres and Spring transactions, not an ORM, and the first workstreams do not touch it | Operator conversation |
| C75 | 2026-10-20 | Q4 resolved. Kept unchanged: ticket categories as a lookup table, the `SERIALIZABLE` booking and escrow transaction, mocked top-up/withdraw, and out of scope: microservices, message brokers (polling instead), horizontal scaling, JPMS, agent Force Cancel/Complete (C22; completion and payout stay automated by the sweep, settlement stays an agent ticket resolution) | Operator, 2026-10-20 | Operator conversation |
| C76 | 2026-10-20 | Re-platform the desktop app as a web app: keep the domain model, services and state machines and replace the delivery layer (MP3-D11). Spring Boot REST API in `backend/` and Next.js with Shadcn/ui and Tailwind CSS in `frontend/`, in one mono-repo (MP3-D1, D8, D9), hosted on Railway and Vercel (backend now Render, C85). Supersedes the desktop-only framing of § Orientation, C1's JavaFX rationale and C2 (the JavaFX UI is replaced) as the web equivalents land | Team design doc adopted by the operator on 2026-10-20 | [Webapp design §§ 1–3, 8](docs/Webapp-architecture-design.md) |
| C77 | 2026-10-20 | PostgreSQL replaces SQLite: local Docker in dev, Neon in prod, with Flyway-managed migrations (MP3-D3). Environments are separated by Spring profiles `dev` and `prod`, with secrets in gitignored dev config and Railway environment variables (design § 6.2). Persistence approach stays C74. Refined by C82 (a committed no-secret `application-dev.yml`) | Team design doc | [Webapp design §§ 2, 6.2](docs/Webapp-architecture-design.md) |
| C78 | 2026-10-20 | The frontend learns of changes by polling, not WebSocket (MP3-D5); events stay in-process via Spring's `ApplicationEventPublisher`. Consistent with C75 (no message brokers) | Team design doc | [Webapp design § 8](docs/Webapp-architecture-design.md) |
| C79 | 2026-10-20 | Security model (S1–S5): Firebase Auth owns credentials and tokens; the backend verifies the ID token on every request and stores the role in its own `users` table, not in Firebase claims; URL namespaces `/api/guest/**`, `/api/host/**`, `/api/agent/**` enforce the role; one account has one role; Host and Agent registration requires a registration code. Refines C70 | Team design doc | [Webapp design § 4](docs/Webapp-architecture-design.md) |
| C80 | 2026-10-20 | AI features (MP3-D6, D7, D10): the NUS SOC LLM is the only provider; outputs are suggestions the user must confirm; prompts and templates are server-side code; the API key never leaves the backend and no client calls the LLM directly; inputs are sanitized, outputs validated, requests rate-limited and capped per user; every AI interaction is audited. The Guest feature is an AI search assistant; Host and Agent features are undecided (Q6) | Team design doc; course policy for the provider | [Webapp design § 5](docs/Webapp-architecture-design.md) |
| C84 | 2026-10-20 | Spec amended after plan review: Checkstyle needs `configDirectory = file('../config/checkstyle')` as well as `configFile` (else `${config_loc}/suppressions.xml` resolves under the project and fails); E7 uses `ALTER ROLE ... NOLOGIN` plus terminating backends instead of pausing the container (a paused container hangs the health query); the `.gitignore` wrapper-jar negation becomes `!**/gradle/wrapper/gradle-wrapper.jar` | Plan-review findings; no behaviour or scope change | spec conventions.md, edge-cases.md; plan T1, T3 |
| C88 | 2026-10-20 | HOST and AGENT codes are reusable configuration secrets named `SNOOZESHARE_HOST_REGISTRATION_CODE` and `SNOOZESHARE_AGENT_REGISTRATION_CODE`; rotation replaces the configured value | Project owner selected configuration-only codes for the initial milestone; database rotation is deferred | Project owner, 2026-10-20 |
| C89 | 2026-10-21 | Firebase email verification is not required before backend registration or protected API access | Project owner reversed the earlier verification prerequisite to allow immediate local and browser testing | Project owner, 2026-10-21 |
| C90 | 2026-10-20 | Keep the Firebase session after backend registration failure and expose retry/sign-out actions | Prevents a recoverable backend outage from forcing a new account/password flow | Project owner, 2026-10-20 |
| C91 | 2026-10-20 | W-AUTH may add the minimal V002 migration for auth users and wallets; W-DOMAIN owns later domain migrations and must preserve the auth contract | Project owner approved the exception so W-AUTH can be independently implemented and tested | Project owner, 2026-10-20 |
| C92 | 2026-10-20 | W-AUTH supersedes the older architecture flow that sends `firebaseUid` from the client and relies on Next middleware auth; the W-AUTH API derives UID from the verified bearer token and uses client-side guards | The older design predates the concrete Firebase browser flow; changing it avoids trusting client identity and avoids an unspecified session-cookie system | W-AUTH spec, 2026-10-20 |
| C93 | 2026-10-21 | Implement the approved role navbar and empty landing shells directly without a standalone spec or plan | Project owner explicitly approved the inline implementation to keep this UI-only change moving; feature documents are intentionally skipped | Project owner, 2026-10-21 |
| C94 | 2026-10-21 | Validate HOST/AGENT registration codes before the website calls Firebase signup, while retaining backend final validation and rate limiting | Project owner observed that an invalid code currently creates a Firebase identity; the website flow must avoid that side effect, while direct or modified clients remain protected by the backend check | Project owner, 2026-10-21 |
| C95 | 2026-10-21 | Production must fail startup unless security, Firebase, registration-code and Admin credential settings are explicitly configured; frontend browser Firebase values are deployment metadata and CI uses non-secret repository variables or safe fallbacks | Review found that an unset security toggle would otherwise expose a permit-all production chain and that CI secrets were unnecessary for public browser configuration | Review remediation, 2026-10-21 |
| C96 | 2026-10-21 | Auth error responses carry a generated request ID that is logged with status, safe code and path; unexpected failures use `INTERNAL_ERROR` | Review found that returned request IDs had no correlation and database failures were mislabeled as validation errors | Review remediation, 2026-10-21 |
| C97 | 2026-10-21 | Use the proxy-set `CF-Connecting-IP` address only when `SNOOZESHARE_TRUST_FORWARD_HEADERS=true`; never read `X-Forwarded-For`; local development ignores proxy headers and falls back to `getRemoteAddr()`. Amends the first version of this decision, which took the first `X-Forwarded-For` entry | The operator requested per-client Render rate limiting while preserving safe local testing. Review found the leftmost `X-Forwarded-For` entry is client-forgeable (Render's proxy appends to it), which let an attacker rotate fake IPs past every limiter; Render's guidance is to use `CF-Connecting-IP` | Project owner, 2026-10-21; review remediation |

---

## 9. Deviations & Discoveries

Live traps only; resolved ones are in the archive. Each is what, the consequence, the ID.

- **MP3-0 (2026-10-18):** `PostCommitEvents` has two modes — strict (production, `enqueue` outside a transaction throws) and autocommit (tests/seeding only). `UserServiceImpl` keeps its own listener-less `TransactionManager` (it never posts to the ledger). Dispute-settlement wallet events now fire at commit, before `TicketResolvedEvent`; a payout emits two wallet events. Tests matching multi-line source text must use `SourceText.read`.
- **D3** — SQLite's Java 25 native-access warning is environmental and non-fatal.
- **D4** — SQLite `REAL` loses `BigDecimal` scale (`150.0`); assert money with `compareTo`, never `equals`.
- **D5** — manual host blocks take inclusive end dates in the UI and are stored half-open (end + 1 day).
- **D8** — `tickets.category` is label text, not an FK; category renames do not propagate.
- **D10, D13 (W8), D15** — `MigrationRunner` adopts a pre-provisioned DB; the committed mock DB predates V002 so a missing `hostDecisionMessage` maps to null; the mock DB ships migrated, so rebuild it from schema + seed after seed edits.
- **D12** — accepted: queue orders by `createdAt` text; adoption only checks for `users`; a self-booking (guest == host) loses an update; escrow comes from `booking.totalAmount()`; `DisputeDetailController.load()` calls `render()` outside its error handler.
- **D18** — the JavaFX date-picker popup cannot be restyled to the board (two month/year spinners, mixed-case weekdays); revisit only with a custom `DatePickerSkin`.
- **D20** — the suspension cascade refunds `totalAmount` without checking an `ESCROW_HOLD` row; a stay checking in today counts as started.
- **D38** — W16 traps: `.agent-root`/`.host-root` overrides outrank `wallet.css` (the shared card also changes Host Wallet); pending trips sit under Upcoming; empty tables use `EmptyTableRow`; the ticket status pill is a fixed 116×36 and the New Ticket button a fixed 36px height (platform font metrics made it 34 on Linux); Messages keep the 32px inset pinned by `HostPageLayoutTest`.
- **D39 (W-INFRA)** — Spring's binder leaves an unresolved `${VAR}` in `spring.datasource.*`, so a missing prod variable surfaced as a driver error that did not name it. `ProdEnvironmentGuard` reads the four required keys strictly and names the variable.
- **D40 (W-INFRA)** — Spring strips a trailing slash from the request `Origin` before comparing, so `http://localhost:3000/` is accepted; browsers never send one. E14 tests an `https` variant and a different port instead; the trailing-slash rule stays on the configured value.
- **D41 (W-INFRA)** — Render runs on Kubernetes, so Spring Boot enabled liveness and readiness probes and the live health body gained a `groups` field that CI could not see. Fixed with `management.endpoint.health.probes.enabled: false` and `HealthOnKubernetesTest`.
- **D42 (W-AUTH)** — Branch had pre-existing uncommitted Firebase Admin and Firebase JS dependency edits when execution began. The clean-tree precondition cannot be met without touching operator work. Action: Preserve the files, include them in their planned T2/T6 dependency commits, and verify the final diff before close-out
- **D43 (W-AUTH)** — Initial frontend baseline `npm ci` was blocked by the environment cache/registry state. The gate was temporarily unverified. Action: Resolved on 2026-10-21 with a task-local npm cache; `npm ci`, 27 tests, lint and production build pass
- **D44 (W-AUTH)** — Firebase Admin beans are gated by `FIREBASE_ENABLED` so existing datasource-free contexts remain usable. Local auth activation requires `FIREBASE_ENABLED=true`; enabled startup still fails fast for missing project, codes or ADC credentials. Action: Document the activation variable in the developer handover at the development checkpoint

---

## 10. Record

The Done ledger lives in **[`docs/project-state/done-ledger.md`](docs/project-state/done-ledger.md)** — every change, big or small, newest first.

- **Latest entry:** 2026-10-21
- **Entries:** 68 (iteration 2 started; earlier entries in the archive)
