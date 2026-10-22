# Project State — SnoozeShare

**Living document — the source of truth for this project.** Read it in full before doing
anything, then run `git log --oneline -20` to confirm it still matches reality. On `main` it
changes only through `sync-project-state` (or an owner hotfix) on `development`; on a branch, read it and write
the branch state instead.

- **Iteration:** 2 — iteration 1 (W1–W16 and the MP3-0 fixes) is closed
- **History:** [docs/project-history/](docs/project-history/README.md) — archived iteration 1; not read at session start, open it only to answer "why was X decided?"
- **Phase:** Iteration 2 is re-platforming SnoozeShare from the JavaFX desktop app to a web app (C76); MP3-0, W-INFRA, W-AUTH, W-DOMAIN, W-AI-GATEWAY, W-HOST and W-HOST-API are done. `development` is a mono-repo: the desktop app under `desktop/`, a Spring Boot backend in `backend/` and a Next.js frontend in `frontend/`, with a live dev deployment; Firebase authentication and role authorization are in place; the domain model, repositories and core services are ported to Spring Data JPA on PostgreSQL; the shared LLM gateway for the AI features (`com.snoozeshare.ai`, § 4.11) is in place and off by default; Host pages use authenticated backend APIs (§ 4.12–4.13)
- **Stack (code on `development` today):** `desktop/`: Java 25, JavaFX 25 (javafx.controls, javafx.fxml), Gradle (application + shadow + checkstyle plugins), SQLite (embedded, `org.xerial:sqlite-jdbc`) via plain JDBC, JUnit 5 + TestFX. `backend/`: Spring Boot 3.5.16 (web, actuator, validation, data-jpa, security), Firebase Admin 9.11, Java 25, Gradle 9.7.1, Flyway, PostgreSQL 17 (Testcontainers in tests), Checkstyle. `frontend/`: Next.js 15.5 (App Router), React 19, Tailwind 4, Shadcn/ui, Firebase JS SDK 12, Vitest, Node 22, npm. Hosting: Render (backend, free web service), Vercel (frontend), Neon (database)
- **Stack (target, not yet built):** The remaining Guest and Agent REST controllers and the role AI features, on top of the ported domain and services and the LLM gateway (§ 4.11). Source: [docs/Webapp-architecture-design.md](docs/Webapp-architecture-design.md)
- **Branch:** `development` — this file describes `development` (the integration and default branch); `main` is the release branch and receives it by pull request. Branches record their changes in `docs/project-state/branch-states/` and `sync-project-state` folds them in after the merge into `development`
- **Method:** TDD-first vertical slices; spec then plan for big changes (`AGENTS.md` § 3); small commits (`AGENTS.md` § 6)
- **Last updated:** 2026-10-22 by GPT-5 — resolved Q6 and documented the Host AI feature
- **Last verified against repo:** 2026-10-22
- **Developer guide:** `docs/DeveloperGuide.md` documents W1–W16 and MP3-0 (last full update 2026-10-19), W-INFRA (2026-10-20: § 3.2, § 4.17), W-AUTH (2026-10-21: § 4.18 and related setup/architecture/requirements/testing), W-DOMAIN (2026-10-22: § 4.19 and related overview/architecture/requirements/testing), W-AI-GATEWAY (2026-10-22: § 4.20 and related overview/architecture), and W-HOST/W-HOST-API (2026-10-22: § 3.2, § 4.18–4.22, § 5 and § 7). Residual limitations stay in the guide's Known Limitations and § Known Gaps.

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
| `backend/` | Spring Boot API (`com.snoozeshare`): health endpoint, `dev`/`prod` profiles, Flyway `V001`–`V005`, CORS, `ProdEnvironmentGuard`, `Dockerfile`, Firebase auth under `auth/`, shared LLM gateway under `ai/` (§ 4.11), Host controllers and safe errors under `api/controller/` and `api/error/` (§ 4.12–4.13), ported domain model (`domain/`), Spring Data JPA repositories (`repository/`), service layer (`service/`, `service/impl/`), DTOs (`api/request/`, `api/response/`, `api/mapper/`); independent Gradle build and wrapper |
| `frontend/` | Next.js app: `SnoozeShare` page with a backend-status check (`Re-check`), `src/lib/env.ts`, login/register/forgot-password pages, `AuthProvider`, route guards and role shells; authenticated Host pages and API provider (`src/app/host/`, `src/components/host/`, `src/lib/hostApi.ts`); Vitest; npm, Node 22 |
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
| Rewrite the Developer Guide for the web app only | Todo | A rolling rewrite, updated as each web change is built, not a one-off: each web workstream adds its own content at its guide checkpoint, and the desktop sections are dropped as `desktop/` retires. Web configuration and deployment content (§ 2, § 3.2, § 4.17) goes first. Done so far: W-INFRA (2026-10-20), W-AUTH (2026-10-21), W-DOMAIN and W-AI-GATEWAY (2026-10-22), and W-HOST/W-HOST-API (2026-10-22: § 3.2, § 4.18–4.22, § 5 and § 7). Next: each later web workstream adds its content when confirmed. Done when the guide has no desktop-only content | Operator, 2026-10-20 | 2026-10-20 |
| W-GUEST, W-AGENT | Workstream | The remaining role feature sets and their Next.js pages. Can run in parallel after W-DOMAIN | Webapp design § 9 | 2026-10-20 |
| W-AI-GUEST, W-AI-HOST, W-AI-AGENT | Workstream | AI features (C80), parallel after their role workstream and on top of the W-AI-GATEWAY gateway (§ 4.11, done). The SoCLaaS contract is confirmed (runbook Evidence). The Host feature is the Listing Optimisation Assistant (§ 5.2); the Agent feature is decided (W-AI-AGENT) | Webapp design §§ 5.2, 9 | 2026-10-20 |
| W-AI-AGENT scoping decisions | Todo | The report goes in a separate AI draft field; the assistant posts as the SYSTEM user with a new `AI` author role in the GUEST channel only; Guest-raised tickets only; it asks at submission, then on each guest reply up to a fixed round cap; it depends on the AI gateway. They belong in that branch's state and spec | handed over from W-AI-GATEWAY | 2026-10-22 |
| Production SoCLaaS key limit | Todo | Before a production key is configured, read its requests-a-minute limit from the portal budget endpoint and set `SNOOZESHARE_AI_MAX_RPM` to a value below it (the development key allows 30; the default is now 25) | handed over from ai-gateway-t8-defaults | 2026-10-22 |
| Role AI specs and reasoning | Todo | Role AI specs (W-AI-GUEST, W-AI-HOST, W-AI-AGENT) can start now that the Render reachability check is recorded (2026-10-22). They must set `maxTokens` with room to spare if a feature ever turns reasoning back on; with the default `none` the models answer in a few tokens. | handed over from ai-gateway-t8-defaults | 2026-10-22 |
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
| W-AUTH | Firebase authentication and role authorization | Done | [spec](docs/superpowers/specs/2026-10-20-w-auth-design.md); [registration pre-validation spec](docs/superpowers/specs/2026-10-21-w-auth-registration-prevalidation-design.md) | [plan](docs/superpowers/plans/2026-10-20-w-auth.md); [registration pre-validation plan](docs/superpowers/plans/2026-10-21-w-auth-registration-prevalidation.md) | Merged (PR #14); owner approval recorded 2026-10-21, after the merge; confirmed 2026-10-21 | Documented 2026-10-21 |
| W-DOMAIN | Port domain model, repositories and core services to Spring Boot with Spring Data JPA | Done | — | [plan](docs/superpowers/plans/2026-10-21-w-domain.md) | Merged (PR #15); approved 2026-10-21 (re-approved after PR bugfixes); guide confirmed 2026-10-22 | Documented 2026-10-22 |
| W-AI-GATEWAY | Shared SoCLaaS LLM gateway (`com.snoozeshare.ai`) for the role AI features | Done | [spec](docs/superpowers/specs/2026-10-22-w-ai-gateway-design.md) | [plan](docs/superpowers/plans/2026-10-22-w-ai-gateway.md) | Merged (PRs #17, #18); approved 2026-10-22 (re-approved after C109; PR #18 approved after its merge); T8 recorded; Render reachable (owner); guide confirmed 2026-10-22 | Documented 2026-10-22 |
| W-HOST | Host features — listings, calendar, requests, wallet, messages, Next.js pages | Done | [experience corrections spec](docs/superpowers/specs/2026-10-22-w-host-experience-corrections-design.md); [UI spec](docs/superpowers/specs/2026-10-21-w-host-ui-design.md) | [UI plan](docs/superpowers/plans/2026-10-21-w-host-ui.md); [experience corrections plan](docs/superpowers/plans/2026-10-22-w-host-experience-corrections.md) | Merged PR #16; owner-approved; UI and calendar refinements complete; guide documented 2026-10-22 | Documented 2026-10-22 |
| W-HOST-API | Replace Host fixtures with authenticated backend integration | Done | [spec](docs/superpowers/specs/2026-10-22-w-host-backend-integration-design.md) | [plan](docs/superpowers/plans/2026-10-22-w-host-backend-integration.md) | Merged PR #16; authenticated backend integration complete; D90 accepted overlap-response test gap; guide documented 2026-10-22 | Documented 2026-10-22 |

Status vocabulary, used verbatim: `Not started` · `Spec'd` · `Planned` · `Building` ·
`Blocked — needs human` · `In review` · `Done` · `Abandoned`. Guide vocabulary (owned by
`update-documentation`): `—` · `Awaiting confirmation` · `Pending` · `Documented YYYY-MM-DD` · `N/A`.
**Progress is a position, not a diary** (~15 words).

---

## 4. Architecture

> **§ 4.1–4.8 describe the desktop code as merged on `development`.** The web target is [docs/Webapp-architecture-design.md § 3](docs/Webapp-architecture-design.md): REST controllers per role, a security layer (Spring Security and Firebase), the ported service layer, domain, repositories and PostgreSQL, plus an LLM gateway. As a branch builds a web part, it rewrites that area's **Now** in its branch state. Desktop paths in 4.1–4.8 are relative to `desktop/` (C81); the web skeleton is in 4.9, authentication in 4.10, the ported persistence/domain/services in 4.5, the LLM gateway in 4.11, Host API/frontend integration in 4.12, and Host availability in 4.13. The layering rules below and their architecture tests (`LayerDependencyTest`, `UiDependencyTest`) apply to the desktop code and are re-decided per area when the web equivalent lands.

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

> **Desktop (§ 4.5 desktop):** repository interfaces per aggregate; JDBC adapters in `repository.jdbc` with shared `RowMappers`. `JdbcLedgerRepository` reads money rows out of `audit_log`; `MigrationRunner` applies `src/main/resources/db/migration/V001–V008`. `TransactionManager` with `TransactionListener` and `inTransactionUnchecked`. Tables: users, properties, availability_blocks, bookings, wallets, tickets, ticket_categories, reviews, audit_log, messages/message_reads, booking_messages/booking_message_reads, schema_history.

**Now (web):** Spring Data JPA with Hibernate on PostgreSQL 17. Domain model uses `@Entity` classes (mutable, no-arg constructors) in `domain.model`, mapped to the schema created by Flyway V002 (auth tables from W-AUTH) and V003 (domain ALTERs and remaining 11 tables). All entities use `id` as the PK column name, matching W-AUTH's convention. `WalletTransaction` remains a record (not an entity) — it is a projection from `audit_log` rows read by a future `JdbcTemplate`-based `LedgerRepository`. V004 seeds the SYSTEM user and platform-fee wallet. V005 tightens the nightly-rate CHECK to reject zero and enforce two-decimal precision. JPA is configured with `ddl-auto: validate` and `open-in-view: false`. Database-dependent service implementations and JDBC adapters use the shared `@ConditionalOnDatabase` condition.

| Path | Role |
|---|---|
| `backend/src/main/java/com/snoozeshare/domain/model/` | 12 JPA entity classes + `AmenitySetConverter` + `WalletTransaction` record |
| `backend/src/main/java/com/snoozeshare/domain/enums/` | 13 enums (copied from desktop) |
| `backend/src/main/java/com/snoozeshare/domain/statemachine/` | `BookingStateMachine`, `TicketStateMachine` (copied from desktop) |
| `backend/src/main/java/com/snoozeshare/domain/policy/` | `StayDatePolicy`, `DisputeWindowPolicy` (updated for entity getters) |
| `backend/src/main/java/com/snoozeshare/domain/settlement/` | `SettlementCalculator`, `SettlementBreakdown`, `EscrowPolicy` (copied) |
| `backend/src/main/java/com/snoozeshare/domain/validation/` | `DomainValidation` (copied) |
| `backend/src/main/java/com/snoozeshare/service/` | 13 service interfaces, 14 value-object records, 2 request types |
| `backend/src/main/java/com/snoozeshare/service/impl/` | 15 service implementations + `AuthorizationService` utility |
| `backend/src/main/java/com/snoozeshare/api/request/` | 16 request DTOs (records with `@Valid` + `@JsonIgnoreProperties`) |
| `backend/src/main/java/com/snoozeshare/api/response/` | 21 response DTOs (records with `static from()` factory methods) |
| `backend/src/main/java/com/snoozeshare/api/mapper/` | 3 mapper utilities (`ListingMapper`, `TicketMapper`, `SearchMapper`) |
| `backend/src/main/resources/db/migration/V003__domain_tables.sql` | ALTERs W-AUTH's users/wallets + creates 11 domain tables |
| `backend/src/main/resources/db/migration/V004__seed_system_user.sql` | SYSTEM user + platform wallet seed |
| `backend/src/main/resources/db/migration/V005__tighten_nightly_rate_constraint.sql` | Rejects zero nightly rate and enforces two-decimal precision |

| ID | Decision |
|---|---|
| C5 | *(desktop)* SQLite via `org.xerial:sqlite-jdbc` and hand-rolled `PreparedStatement` DAOs; no JPA |
| C6 | `properties` columns are exactly the operator's field list (see `desktop/db/schema.sql`) |
| C40 (W14) | `wallets` table stays (no `users.balance`); the System user has role `SYSTEM` and a real wallet receiving `PLATFORM_FEE` rows |
| C99 | Use Spring Data JPA for persistence (reverses C74/C5 for web). Entities use `id` as PK column on all tables to match W-AUTH's convention |
| C100 | Wallet balance uses `NUMERIC(19,2)` (from W-AUTH's V002), not `NUMERIC(19,4)` from the desktop schema. Other money columns (rates, amounts, adjustments) keep `NUMERIC(19,4)` |

| C129 | 2026-10-22 | *(superseded by C132)* Amend W-HOST-API to include deterministic activation of existing W-DOMAIN services and JDBC adapters through `snoozeshare.persistence.enabled` (`true` by default; `false` in DB-free contexts), without changing service/domain behavior | Project owner approved amending and continuing after T2 exposed that `@ConditionalOnBean(PropertyRepository.class)` runs before JPA repository registration. Deferring the prerequisite to separate work was rejected because all Host API endpoints depend on those services | Operator conversation |
| C132 | 2026-10-22 | Replace W-HOST-API's explicit `snoozeshare.persistence.enabled` bean gate (C129) with `@ConditionalOnDatabase`, which matches unless datasource auto-configuration is excluded | Project owner approved adopting `development`'s convention after learning the former gate would be replaced by the shared backend condition; this aligns W-HOST-API with the integrated service/repository activation model | Operator conversation |

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

**Now:** one repo, three independent builds (no root build) with a shared Checkstyle ruleset in `config/checkstyle/`. `backend/` is Spring Boot 3.5.16 on Java 25 with Flyway `V001` (a comment-only baseline), `V002` (auth users and wallets, § 4.10), `V003`–`V005` (domain tables, system-user seed and nightly-rate constraint, § 4.5) on PostgreSQL 17. Its health endpoint is `GET /actuator/health`, which returns exactly `{"status":"UP"}` (or `DOWN` with 503); every other actuator path is 404, details and Kubernetes probe groups are off; the `/api/auth/**` endpoints are in § 4.10. Profiles: `dev` is the default (`application-dev.yml`, committed, no secrets, local Postgres, CORS for `http://localhost:3000`; secrets go in a gitignored `application-local.yml`); `prod` has no defaults, and `ProdEnvironmentGuard` fails startup naming any missing `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` or `SNOOZESHARE_CORS_ALLOWED_ORIGIN`, and requires `snoozeshare.security.enabled` and `snoozeshare.firebase.enabled` to be true (§ 4.10). CORS allows exactly one origin (`snoozeshare.cors.allowed-origin`, validated: no wildcard, no trailing slash), configured separately for application endpoints (`CorsConfig`) and the actuator. `server.port` follows `PORT`. `frontend/` is Next.js 15 (App Router): `src/lib/env.ts` is the only reader of `NEXT_PUBLIC_API_BASE_URL`, `next.config.ts` fails a production build without it, and `BackendStatus` shows `Backend: UP` or `unreachable` (5 s timeout, `Re-check` button); the auth pages and providers are in § 4.10. Tests: backend integration tests run on one JVM-wide Testcontainers Postgres 17; frontend uses Vitest. The dev environment tracks `development`: the Render Docker web service (`render.yaml`, free plan, sleeps when idle), the Vercel project (root `frontend`, Framework Preset Next.js, Production Branch `development` until W-DEPLOY) and a Neon branch `dev`. Deployment order and variables are in `docs/runbooks/dev-environment.md`.

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

### 4.11 AI gateway (backend `ai/`)

**Now:** the backend has one LLM entry point, `LlmGateway.run(AiFeature, LlmCall)` in `com.snoozeshare.ai`. It runs a fixed pipeline: shape and transaction checks, DISABLED, role gate, per-user rolling-minute and in-flight limits, sanitizing (NFKC, control and format characters, PII, delimiter tags) and prompt assembly with untrusted text wrapped in `<user_input>` and `<untrusted_data>`, per-user and global daily caps counted from `audit_log` plus in-flight reservations, a budget block and a circuit breaker, then up to `max-retries + 1` attempts through one global scheduler (`max-rpm`, `max-concurrent`, priority queue) against SoCLaaS `POST /chat/completions` over a shared `HttpClient` (https, no redirects), then output validation (stop reason, refusal marker, key and link checks, schema or length) and one audit row (`AI_CALL` or `AI_CALL_REJECTED`, metadata only). Every failure returns the feature's fallback with a code; `run` blocks the caller thread for at most `deadline-ms` and refuses to run inside a transaction. Role workstreams register `AiFeature` beans and own their prompts, output types and endpoints. Limiter, breaker and budget state are process-local (C69). With `snoozeshare.ai.enabled` false, or with the upstream unreachable, the backend and its health endpoint are unaffected.

| Path | Role |
|---|---|
| `backend/src/main/java/com/snoozeshare/ai/` | `LlmGateway`, `AiFeature`, `OutputSpec`, `LlmCall`, `LlmResult`, `LlmErrorCode` |
| `backend/src/main/java/com/snoozeshare/ai/{config,guard,limit,client,audit}/` | properties and startup check; sanitizing and validation; scheduler, throttle, breaker, budget block; the SoCLaaS client; the audit writer |
| `backend/src/main/java/com/snoozeshare/domain/enums/AuditAction.java` | gains `AI_CALL` and `AI_CALL_REJECTED` |
| `application*.yml`, `ProdEnvironmentGuard`, `render.yaml` | the `snoozeshare.ai.*` keys and the production guard rules |

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C103 | 2026-10-22 | Generic gateway; each role feature registers its own prompt, output type and caps | Operator chose it over a gateway that owns every feature's prompt, which would block on Host's undecided feature | spec SD1 |
| C104 | 2026-10-22 | Audit stores metadata only, not prompt or response text. Departs from `Webapp-architecture-design § 5.3` | Operator chose it over truncated sanitized text, to avoid copying personal data into `audit_log` | spec SD2 |
| C105 | 2026-10-22 | Three departures from `Webapp-architecture-design §§ 3, 5.3` (with C104): a custom scheduler and breaker replace Bucket4j, and the module is `com.snoozeshare.ai` instead of `service/ai` | The shared-key budget needs a global queue with priorities, which a per-user rate limiter does not give; no new dependency | spec SD4, SD11, SD12 |
| C106 | 2026-10-22 | Daily caps count `AI_CALL` audit rows plus in-flight reservations; fail-closed audit for ok results; no gateway call inside a transaction | Caps survive restarts and bursts; unaudited output must not reach a user; a 45 s call must not hold a connection | spec SD5, SD6, SD7, SD11 |
| C107 | 2026-10-22 | Output with links, HTML or Markdown images (also Unicode-escaped, after NFKC) is rejected by default; priority 0 keeps a reserved share of the global daily cap; base URL must be https and redirects are never followed; a budget 429 blocks for 15 minutes then re-probes | Reviewer threats: exfiltration through rendered links, key sent to another host, a forged "quota" phrase causing a day-long outage | spec SD13, SD15, SD16 |

---

### 4.12 Host API and frontend integration

**Now:** Protected Host controllers register only when `snoozeshare.security.enabled=true`, matching `HostIdentityResolver`; listing, calendar, booking decision, conversation, and wallet routes resolve host identity server-side and call services rather than repositories. The authenticated frontend adapter routes requests through `authenticatedFetch`; all Host listing, calendar, request, message, and wallet screens use API-backed state with fixture mode retained for presentation tests. Collection and history GETs return every matching owner-scoped row in deterministic order, with no page/size contract; the listings collection carries booking count and average review rating from two grouped queries filtered by the resolved Host ID. Listing cards preserve those metrics when their status response patches only the matching row. The calendar route loads listing detail independently from month data, retains the last successful month while the requested month loads in-grid, and starts its block range empty; calendar data remains month-bounded with at most 42 grid cells. Block-create conflicts return distinct 409 codes for booking overlap and prior host blocks, preferring the booking code if both sources overlap (C139). This accepts unbounded collection payloads and server/client memory use (C131). Wallet mutations retain the existing ledger transaction and wallet row lock, with events after commit; ambiguous failures refresh balance and statement without retrying.

| ID | Decision |
|---|---|
| C127 | *(historical baseline; collection pagination superseded by C131)* W-HOST-API uses the operator-approved composite responses, inclusive month calendar, distinct message routes, stable errors, Firebase/domain identity check, booking row locking, transactional availability/message behavior, and bounded database pagination; it does not add wallet request idempotency after C128 |
| C131 | Reverse bounded Host collection/history pagination: listing, request, conversation-summary, message-history, and wallet-statement GETs return every matching row in one response; remove `page`, `size`, `items`, `hasMore`, and pagination controls. Preserve owner scoping, authorization, deterministic ordering, DTOs, and the calendar's one-month bound |

### 4.13 Host availability

**Now:** Host month reads query only overlapping blocks and bookings; mutations use fresh PostgreSQL `SERIALIZABLE` transactions, audit in the transaction, retry SQLSTATE `40001` once, then publish the availability event after commit. API ranges are inclusive; persisted ranges remain half-open.

| ID | Decision |
|---|---|
| C127 | Host calendar reads are month projections; block mutations are atomic and auditable. |
| C139 | Calendar block conflicts use distinct source codes; booking conflict takes precedence. |

## 5. Conventions

Highest IDs used before this iteration: **W16, C59, D38, Q3** (this iteration so far: W16, C141, D90, Q7). Numbering continues; never reuse. `C11, C28–C31, C37, C39–C41` each have two meanings in the archive; cite the archive section when it matters.

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
- JPA entities use `id` as the PK column name (not entity-specific names like `user_id`, `booking_id`); getter is `getId()`. Domain enums in `domain.enums` are shared between the desktop and backend; copy verbatim, change only the package declaration.
- Checkstyle import order for the backend: `java.*` then `org.*` then `com.*` then `jakarta.*`, with blank lines between groups.
- Role code calls `LlmGateway.run` outside any transaction, supplies `userId` and `role` from its authorized principal, builds ASSISTANT turns only from server-stored history, puts only server-built text in `trustedContext`, acts on `flagged`, and renders AI output as plain text through a response DTO.
- Nothing outside `com.snoozeshare.ai` imports `ai.config`, `ai.guard`, `ai.limit`, `ai.client` or `ai.audit`, and nothing outside `ai/client` calls SoCLaaS; a JUnit source-scan test enforces it.
- Daily boundaries for AI caps use UTC; waiting and randomness in `ai.limit` go through `Sleeper` and `Jitter` so tests need no real sleep.
- Logs for AI failures carry feature id, user id, code, status and latency only, and exceptions are logged by class name; the JDK HTTP client debug logger is never enabled.
- `application-prod.yml` has no default for `snoozeshare.ai.enabled`; `render.yaml` sets `SNOOZESHARE_AI_ENABLED` to `"false"` until AI is switched on.
- Beans that need the database carry `@ConditionalOnDatabase` (`config/ConditionalOnDatabase`), which matches unless `spring.autoconfigure.exclude` lists `DataSourceAutoConfiguration`, so datasource-free contexts start without them. Do not use `@ConditionalOnBean(PropertyRepository.class)` on scanned classes: it is evaluated before Spring Data registers repositories and never matches (see § Deviations, D51). Service impls use entity-style getters (`.getField()`) instead of record accessors (`.field()`); mutation uses setters + `repository.save()` instead of creating new records.
- Neon's libpq string must be converted to a JDBC URL with separate username and password (see the runbook). `NEXT_PUBLIC_API_BASE_URL` has no trailing slash and `src/lib/env.ts` is its only reader.
- Local traps on Windows: `DOCKER_HOST` alone does not break Testcontainers (it caches `docker.client.strategy` in `~/.testcontainers.properties`; also set `TESTCONTAINERS_DOCKER_CLIENT_STRATEGY` and use `--no-daemon`); running Checkstyle and the Testcontainers tests in one Gradle invocation can exhaust memory, so run them separately; Docker image layers can corrupt after a Docker Desktop crash (`exec format error`), so pull a different tag.
- Branch states keep every `PROJECT_STATE.md` heading in order; a section with nothing to change says `No change.` (Architecture, Conventions and Known Gaps give a one-line reason), and `sync-project-state` checks those claims against the branch diff. Skills say `INT` for the integration branch (here `development`; `maintaining-project-state` § Branch Model). Plan tasks a human does after the merge are `Post-merge` and become Handover items; steps that leave the machine start with `GATE:`.
- A pull request into `development` fails its `build` check when it carries a branch state that is not `Approved` (with the owner's name and date on the `Approved` line) or that has live session rows (`.github/scripts/check-branch-states.sh`, run by the `repo:` step in `ci.yml`). It cannot tell who typed the approval, so it backs up the owner's decision and does not replace it. A pull request into `development` with no branch state also fails, unless its head branch is `sync/*` (the fold PR, which archives the state; C98).
- A push that changes only `PROJECT_STATE.md`, `AGENTS.md` or `docs/**` does not run CI (`paths-ignore` on push only; pull requests always run), and a push to a branch with an open pull request skips the duplicate push run. Render redeploys only for `backend/**` or `render.yaml` changes, and Vercel skips a deploy only when nothing under `frontend/` changed since its last successful build (`ignoreCommand` in `frontend/vercel.json`, using `VERCEL_GIT_PREVIOUS_SHA`; it must exit exactly 0 or 1, because Vercel treats any other code as a failed build), so a docs-only newest commit cannot hide an earlier frontend change and a failed build is retried by the next push.
- Host pages use a fixed navbar and viewport-contained scrolling; listing route/load failures return to listings, while validation/mutation errors stay inline. Host APIs derive identity server-side and gate controllers on `snoozeshare.security.enabled=true`.
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
- Host listing, request, conversation, message-history, and wallet-statement GETs return complete owner-scoped collections without pagination or response-size caps; this accepts unbounded response and memory use (C131).
- W-DOMAIN still owns domain persistence and wallet operations beyond registration-time provisioning.
- Public code pre-validation remains a validity oracle by design; rate limits and secret strength are the compensating controls.
- Rate limiting is process-local. Forwarded-header trust is off locally and on in production, where the client address comes from `CF-Connecting-IP`; that is only sound while all traffic reaches Render through Cloudflare, so re-check it if the hosting or proxy changes (including W-DEPLOY).
- Host and Agent codes remain reusable configuration values; rotation requires replacing deployment configuration.
- Protected frontend routes use client-side guards; backend role authorization remains authoritative.
- Firebase token revocation is checked on every backend request, coupling request latency and availability to Firebase.
- **AI limiter, breaker and budget-block state is process-local and lost on restart** (C69); a Render redeploy briefly runs two instances on one shared key. "Hard ceiling" means per process.
- **A coordinated group of accounts can still open the AI circuit breaker** (it needs failures from 3 distinct users); GUEST registration needs no code (C89). Bounded by the per-user and global daily caps.
- **Steered prose that matches no leak or link pattern passes output validation**; role features keep AI output a suggestion the user confirms (C80).
- **A daily cap counts calls, not upstream requests**: worst case is `global-daily-cap x (max-retries + 1)` requests a day.
- **Many accounts can still exhaust the non-reserved 95 percent of the global AI cap and the queue** (guest registration needs no code, C87); priority-0 features keep a reserved share and displace lower-priority jobs.
- **While audit writes fail, sent calls are counted in memory only until 00:00 UTC**, so a restart loses that floor.
- **Bare-domain links (no scheme) in model output are not detected**; role UIs render AI output as plain text.
- **Reasoning cannot be chosen per feature or tier.** `reasoning-effort` is one global setting (default `none`). `llama3.1:8b` rejects `low` with HTTP 400, and `low` does not stop `qwen3.5:9b` or `gemma4:26b` from reasoning, so a feature that needs reasoning would need a gateway amendment (a per-feature option and `maxTokens` headroom).
- **429 behaviour is unobserved.** 100 requests in a burst returned no 429 although the development key's policy says 30 requests a minute; whether a 429 carries `Retry-After` and what a budget 429 says are still unknown, so those paths rely on the spec's assumptions.

**Explicitly out of scope:** microservices, message brokers, horizontal scaling, JPMS module boundaries, agent Force Cancel/Complete (C22).

---

## 7. Needs a Human

| ID | What is needed | What it blocks | Raised |
|---|---|---|---|
| — | No open items. The former Q6 is resolved by C142; the Host AI feature is defined in webapp-architecture-design § 5.2. | — | — |

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
| C80 | 2026-10-20 | AI features (MP3-D6, D7, D10): the NUS SOC LLM is the only provider; outputs are suggestions the user must confirm; prompts and templates are server-side code; the API key never leaves the backend and no client calls the LLM directly; inputs are sanitized, outputs validated, requests rate-limited and capped per user; every AI interaction is audited. The Guest feature is an AI search assistant; the Host feature is the AI Listing Optimisation Assistant defined in § 5.2; the Agent feature is decided in the W-AI-AGENT handoff | Team design doc; course policy for the provider; C142 resolves the stale Host status | [Webapp design § 5.2] |
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
| C98 | 2026-10-22 | Workflow gates (`AGENTS.md` § 8): the agent stops and asks the operator for a yes at six points — spec approved before the plan, plan approved before any code, branch review before any push or PR, sync after the merge, fold confirmation, then the guide proposal — and writes the `Approved (<name>, <date>)` marker itself on that yes. "By hand" in the sync and guide skills now means the operator's yes in chat, so the operator no longer has to know which skill comes next. Rejected: leaving the skills operator-invoked; relying on the `check-branch-states.sh` CI check alone (it skipped a PR that had no branch state; `ci.yml` now fails such a PR unless its head is `sync/*`) | Operator: W-AUTH was merged before its approval was recorded ("after the merge" in its Workstreams row) and the plan carried no approval marker, so nothing prompted the operator at the stage boundaries | `AGENTS.md` § 8; owner hotfix on `development` |
| C99 | 2026-10-21 | Use Spring Data JPA for persistence (reverses C74/C5 for web). Entities use `id` as PK column on all tables to match W-AUTH's convention | JPA provides cleaner Spring integration (validation, transactions, repositories) than ported JDBC; column naming follows W-AUTH's established pattern | Plan T1 |
| C100 | 2026-10-21 | Wallet balance uses `NUMERIC(19,2)` (from W-AUTH's V002), not `NUMERIC(19,4)` from the desktop schema. Other money columns (rates, amounts, adjustments) keep `NUMERIC(19,4)` | W-AUTH created the wallets table first; the domain migration builds on it | V002/V003 reconciliation |
| C101 | 2026-10-22 | The LLM gateway is its own workstream, W-AI-GATEWAY, merged before W-AI-GUEST, W-AI-HOST and W-AI-AGENT | Operator: lets the three role AI features run concurrently on a shared base. Rejected: the first role feature builds the gateway; every role builds its own client | Operator conversation |
| C102 | 2026-10-22 | Development and production use separate SoCLaaS keys, each supplied by the deployment environment and never committed. The owner already holds the development key | Operator | Operator conversation |
| C103 to C107 | 2026-10-22 | See § 4 (4.11 AI gateway) | Operator and review rounds | spec |
| C108 | 2026-10-22 | Spec risk tier is High with Implementer, Tester, Maintainer and Attacker lenses | Operator chose High over Medium | spec SD3 |
| C109 | 2026-10-22 | Default `priority-zero-reserve-percent` lowered from 20 to 5 (spec SD21, api.md, F7, E55 amended; still configurable 0 to 50) | Operator: the SoCLaaS key is not used by any other system, so only our own roles compete for the cap and a large staff reserve is not needed. Rejected: removing the reserve (a flood of guest accounts could still starve Agent and Host features) | Operator conversation |
| C110 | 2026-10-22 | `snoozeshare.ai.max-rpm` defaults to 25 (was 55) | Operator: the development key allows 30 requests a minute (portal budget endpoint). Rejected: leaving 55 and setting an environment variable on Render, because a forgotten variable means 429s | spec SD9 |
| C111 | 2026-10-22 | `snoozeshare.ai.reasoning-effort` defaults to `none` (was empty, field omitted) | Operator: `qwen3.5:9b` and `gemma4:26b` spend `max_tokens` on reasoning and returned empty content at 20 and 300 tokens; all three models accept `none`. Rejected: switching to other chat models (most larger ones also reason and cost 4 to 20 times as much per output token), and keeping reasoning on with large `maxTokens` | spec SD10 |
| C112 | 2026-10-22 | The startup model check logs one INFO line when it succeeds | Operator: silence on success is indistinguishable from "AI is off" in Render logs | `ModelCheck` |

---

| C113 | 2026-10-21 | W-HOST UI must match the supplied Claude artifact exactly, including its Fall Light tokens, shell layout, page composition and interaction presentation | User explicitly set the artifact as the visual source of truth; generic existing Tailwind styling is insufficient | [Claude artifact](https://claude.ai/artifact/PWBCxbfv9e9FGVvY6RKUwd) |
| C114 | 2026-10-21 | Host content pages use the full available width; edit values use normal weight and selected status uses title case; host messages do not show a New Ticket action | Project owner, after authenticated comparison, directed these UI corrections and explicitly rejected adding New Ticket to Host messaging | Operator conversation |
| C115 | 2026-10-21 | Listings page places its create action beside the title; cards omit the status pill and use five centered, width-stable right-side groups, with outlined calendar/edit actions | Project owner requested the final Listings refinements after reviewing the pushed W-HOST implementation | Operator conversation |
| C116 | 2026-10-21 | Within the Availability group, the switch sits to the right of its ACTIVE/INACTIVE label; reduce horizontal gaps among all five card groups while retaining centered content and stable widths | Project owner requested a follow-up after reviewing the first T9 layout | Operator conversation |
| C117 | 2026-10-21 | New/Edit listing forms retain full-width page use, with their four form cards arranged to fill the available width rather than constraining the form to the artifact's narrower artboard | Project owner clarified that the forms should add up to use the full width after the visual comparison surfaced the apparent width difference | Operator conversation |
| C118 | 2026-10-21 | Booking calendar shows as many complete week rows as its month requires; blocking uses one artifact-style date-range picker and a required reason; current blocked dates list bold reasons above ranges with aligned remove actions, using Maintenance as the fixture reason | Project owner specified calendar coverage and the date-blocking presentation; separate start/end fields, optional reasons, and unlabeled override ranges are rejected | Operator conversation |
| C119 | 2026-10-21 | Booking-calendar month navigation uses fixed arrow columns with the month/year centered in a fixed-width track, so varying title length does not move the arrows | Project owner requested stable arrow placement and a centered month/year label when navigating across month names of different lengths | Operator conversation |
| C120 | 2026-10-21 | Reopened date picker must calculate its Sunday-first grid from the first of the selected month, not the selected date itself | Project owner reported selected dates shifted to incorrect weekdays after closing and reopening the picker | Operator conversation |
| C121 | 2026-10-21 | The booking calendar always renders six week rows; the date-range picker stages edits until Apply, Clear resets the draft without closing, outside click closes without applying, and picker/current-blocked ranges include the end year (both years across year boundaries) | Project owner specified the calendar layout, artifact-style picker controls, dismissal behavior, and range-year presentation | Operator conversation |
| C122 | 2026-10-21 | The current page name in every Host breadcrumb is bolded | Project owner requested consistent visual emphasis for the current page | Operator conversation |
| C123 | 2026-10-21 | Host wallet presentation follows the artifact’s inset balance panel, stacked controls, currency label, and bordered transaction table; fixture rows represent relevant booking and wallet events | Project owner approved matching the Wallet page to the artifact and requested sample data based on booking or other relevant examples | Operator conversation |
| C124 | 2026-10-22 | Vertically center the stacked Top up and Withdraw actions against the available-balance panel without changing their width or stack order | Project owner requested and approved the Wallet action alignment adjustment | Operator conversation |
| C125 | 2026-10-22 | Keep Host listings, booking requests, and wallet within the viewport below the navbar; scroll listing results, cap pending requests at four rows, and let past requests and wallet statement use the remaining height | Project owner requested the three page-specific scrolling behaviors to avoid long content pushing pages beyond the screen | Operator conversation |
| C126 | 2026-10-22 | Populate enough sample data to exercise each scroll region (12 listings, 8 pending requests, 10 past requests, 31 wallet transactions) and keep table headers visible while scrolling | Project owner requested overflow-ready sample data and sticky headers after reviewing the initial scroll behavior | Operator conversation |
| C127 | 2026-10-22 | *(historical baseline; collection pagination superseded by C131)* W-HOST-API uses the operator-approved composite responses, inclusive month calendar, distinct message routes, stable errors, Firebase/domain identity check, booking row locking, transactional availability/message behavior, and bounded database pagination; it does not add wallet request idempotency after C128 | Project owner approved the recommended defaults to proceed; second-round review required exact bounded query semantics. In-memory slicing was rejected. Wallet idempotency was later explicitly reversed | Operator conversation and second-round spec review |
| C128 | 2026-10-22 | Remove wallet request idempotency from W-HOST-API: no key header, durable key table, hash, replay response, or request deduplication; each accepted mocked wallet mutation is a separate operation, with no automatic client retry and wallet refresh after ambiguous failure | Project owner said it was unnecessary, then explicitly directed removal after the existing decision C127 / spec SD7 and SD10 was surfaced. Existing wallet transaction, row lock, ledger, and audit behavior remain | Operator conversation |
| C129 | 2026-10-22 | *(superseded by C132)* Amend W-HOST-API to include deterministic activation of existing W-DOMAIN services and JDBC adapters through `snoozeshare.persistence.enabled` (`true` by default; `false` in DB-free contexts), without changing service/domain behavior | Project owner approved amending and continuing after T2 exposed that `@ConditionalOnBean(PropertyRepository.class)` runs before JPA repository registration. Deferring the prerequisite to separate work was rejected because all Host API endpoints depend on those services | Operator conversation |
| C130 | 2026-10-22 | All Host listing route/load failures use one generic error page with a link to `/host/listings`; form validation and mutation failures remain inline | Project owner clarified scope as route/load failures only and approved the shared error-page design; the listings index additionally offers retry on its own load failure | Operator conversation |
| C131 | 2026-10-22 | Reverse bounded Host collection/history pagination: listing, request, conversation-summary, message-history, and wallet-statement GETs return every matching row in one response; remove `page`, `size`, `items`, `hasMore`, and pagination controls. Preserve owner scoping, authorization, deterministic ordering, DTOs, and the calendar's one-month bound | Project owner explicitly chose to remove pagination and return whole collections after the bounded database-pagination decision was surfaced; rejected retaining pagination or silently loading full rows and slicing them in memory. This knowingly accepts unbounded response size and memory use, with no added response-size, per-request rate, or concurrency limits | Operator conversation |
| C132 | 2026-10-22 | Replace W-HOST-API's explicit `snoozeshare.persistence.enabled` bean gate (C129) with `@ConditionalOnDatabase`, which matches unless datasource auto-configuration is excluded | Project owner approved adopting `development`'s convention after learning the former gate would be replaced by the shared backend condition; this aligns W-HOST-API with the integrated service/repository activation model | Operator conversation |
| C133 | 2026-10-22 | Correct Host listing metrics, avoid full-content reloads for listing status and calendar month changes, preserve listing times exactly across detail/edit views, show occupancy to two decimals, and leave the calendar block date range empty initially | Project owner approved this follow-up after review found collection responses omit metrics, status/month loads replace the page with a skeleton, and local-time parsing followed by UTC formatting shifts times (e.g. 07:00 to 23:00 prior day); rejecting timezone conversion and API N+1 detail reads | Operator conversation |
| C134 | 2026-10-22 | Approve the W-HOST experience-corrections spec and proceed to the plan gate | Trevor approved the reviewed Medium-risk spec after two rounds of Implementer, Tester, and Attacker review; code remains gated on a separate plan approval | Operator, 2026-10-22 |
| C135 | 2026-10-22 | New/Edit listing forms validate check-in and check-out, require check-out strictly earlier than check-in, use required/non-negative copy for bedroom and bathroom errors, and show request failures to the right of the form actions | The project owner requested these form validation and error-placement behaviors after reviewing the listing pages | Operator conversation |
| C136 | 2026-10-22 | Listing detail check-in/out display uses zero-padded `hh:mm AM/PM`; calendar block submission has no confirmation modal, submits by button or Enter, and reports booking/blocked-period overlap distinctly | The project owner requested these listing-detail and calendar interaction/error behaviors | Operator conversation |
| C137 | 2026-10-22 | Top-up and withdrawal dialogs follow their distinct approved artifact flows: top-up shows balance and $50/$100/$500/$1,000 presets; withdrawal shows available balance, supports filling the full amount, and cannot exceed available non-escrowed funds; each has its specific explanatory copy and confirm label | The project owner asked to resolve the identified differences from the Host UI artifact | Operator conversation |
| C138 | 2026-10-22 | Display the full canonical booking UUID with one `#` in booking Messages and Wallet references; do not derive a short booking number absent from the API contract | The project owner asked for the booking number in both views; the backend supplies only the UUID, so a derived short number would be synthetic and could collide | Operator conversation |
| C139 | 2026-10-22 | Calendar block conflicts return distinct 409 codes and safe UI messages for booking overlaps (`BOOKING_OVERLAP`) and existing host blocks (`HOST_BLOCK_OVERLAP`); when both overlap, report the booking conflict first and do not expose IDs or dates | The project owner asked to identify what a proposed block overlaps after learning the existing combined message cannot tell which source caused the conflict; booking precedence follows the calendar's existing Booked-over-Blocked precedence | Operator conversation |
| C140 | 2026-10-22 | Amend the W-HOST-API calendar contract: malformed input is 400, past/reversed ranges remain 422, booking overlaps return 409 `BOOKING_OVERLAP`, existing Host-block overlaps return 409 `HOST_BLOCK_OVERLAP`, and booking wins if both apply; both use the shared safe message `Request could not be completed`, with source-specific UI copy selected by code and no conflicting IDs/dates exposed | The project owner directed resolution of the API-spec discrepancy and approval afterward; rejected changing the implemented typed 409 behavior back to generic 422 because it would discard the approved C139 distinction | Operator conversation |
| C141 | 2026-10-22 | Approve W-HOST and W-HOST-API after reconciling the API spec and plan with the implemented calendar conflict contract | The project owner explicitly asked to resolve the discrepancy and then approve; spec/plan lint is clean, second-round High-risk lenses found no blocking findings, and the remaining test coverage limitation is recorded under D90 | Operator conversation |
| C142 | 2026-10-22 | Resolve Q6: adopt the Host AI Listing Optimisation Assistant from webapp-architecture-design § 5.2 — it uses listing details plus host-selected style or instructions to generate a natural-language description for Host review and editing | The project owner pointed out that § 5.2 already defines the feature; keeping Q6 open contradicted the design document. Rejected: treating the feature as undecided until the implementation spec exists | Operator conversation; [Webapp design § 5.2](docs/Webapp-architecture-design.md) |

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
- **D45 (W-DOMAIN)** — W-AUTH's V002 uses `id` as PK for users/wallets (not `user_id`/`wallet_id` from the desktop schema), `status` (not `account_status`), and includes `firebase_uid` + `updated_at`. The plan's original migration had to be restructured as V003 (ALTER + new tables) instead of V002 (CREATE all). All entity PK fields renamed to `id`; User entity gained `firebaseUid`, `updatedAt`; `accountStatus` renamed to `status`. Future services must use `getId()` not entity-specific getters
- **D46 (W-DOMAIN)** — Flyway version conflict: W-AUTH claimed V002, domain tables shifted to V003, seed to V004. Migration numbering diverged from the plan; no functional impact
- **D47 (W-AI-GATEWAY)** — **Wave 2 (T1 to T5) wrote implementation and tests together, not test-first.** The tests are committed with the code and pass; no red run was recorded.
- **D48 (W-AI-GATEWAY)** — **T3 output validation is stricter than conventions.md in three places:** `data:` and `javascript:` links match on a word boundary (so "metadata:" passes); Unicode format characters (zero-width, bidi) are stripped before the key, tag and link checks; leak checks also cover JSON keys, not only values. A fence with no newline after the opening fence is not unwrapped.
- **D49 (W-AI-GATEWAY)** — **T5:** `AttemptResult.Failure` carries an extra `httpStatus` for the WARN line; a 429 body over 4,096 bytes is cut mid-JSON and so classed RATE_LIMITED; the request timeout bounds time to headers only, so T6's deadline is the backstop for a stalled body, and T6 must check the interrupt flag after each attempt.
- **D50 (W-AI-GATEWAY)** — **T4:** the gateway (T6) must call `abandonProbe()` on the breaker or budget block that issued a `PROBE` when the call ends without an outcome; none of the `ai.limit` classes is a Spring bean yet, so T6 wires them.
- **D51 (W-AI-GATEWAY)** — **W-DOMAIN's `@ConditionalOnBean(PropertyRepository.class)` never matched in the running app (found in T6, fixed in `d049995`).** The condition is evaluated during component scanning, before Spring Data registers repositories, so all 15 W-DOMAIN service impls, the JDBC repositories and `LedgerWriter` were silently absent from the application. The operator chose to fix it on this branch: all 20 classes now use `@ConditionalOnDatabase`, and `DatabaseBeansTest` proves the beans exist. This makes those beans live in the app for the first time, so W-DOMAIN's wiring is exercised by the full build; fold this into W-DOMAIN's description of how beans register.
- **D52 (W-AI-GATEWAY)** — **T6:** `ModelCheck` creates its own single-thread daemon executor (a second constructor takes an `Executor` for tests) instead of an `Executor` bean in `AiConfig`, because any `Executor` bean switches off Spring Boot's auto-configured task executor. `OutputValidator` is a `@Lazy` bean so slice tests without an `ObjectMapper` still start. Tests of `LlmGatewayTest` use a mocked `AiAuditWriter`; real audit rows are covered by `LlmGatewayIntegrationTest`.
- **D53 (W-AI-GATEWAY)** — **T6 test coverage gaps against the plan list:** `pipelineOrder` (the six pairs of E49), `floodDoesNotStarveOthers`, `obedientModel` and `deadlineDuringBackoff` are not separate tests; the fixed check order is covered by the individual tests above, and these are carried to the branch review as a known gap in test depth.
- **D54 (W-AI-GATEWAY)** — **T7:** `ProdEnvironmentGuard` no longer imports `ai.config` (it keeps its own blank/placeholder check) so the boundary test holds; key-shaped strings in earlier AI tests were shortened to `clsk_..._fakekey` because `SecretScanTest` scans every tracked file.
- **D55 (W-AI-GATEWAY)** — **Code review (high, 2026-10-22) fixed in `070191e`:** the client now reads the whole response within the attempt timeout (a stall after the headers could hold a thread and its slots); the sanitizer re-checks injection phrases after delimiter tags are removed; `run` validates the feature it receives; `ConditionalOnDatabase` reads list-form excludes; `SecretScanTest` skips without git. One finding left open: four planned gateway tests are not separate tests (above). The plan's Traceability table names about 15 test methods that exist under other names (for example `retryMatrix` is `serverErrorsRetryTwiceWithBackoffThenReturnServer` and `rateLimitThenSuccessReportsTwoAttempts`); the table was not renamed, and the Close-out item "every Traceability row verified" stays unticked for that reason.
- **D56 (W-AI-GATEWAY)** — **T2:** tests build `LlmProperties` with `enabled=false` (enabled demands a URL and key).

---

- **D57 (W-HOST, 2026-10-21)** — Initial T8 comparison was blocked by missing Host authentication; operator subsequently logged in and enabled a live route comparison. Action: Resolved. Authenticated artifact comparison completed; all eight Host content routes were checked at 1680×945, 1280×800, and 768×1024 with no horizontal overflow
- **D58 (W-HOST, 2026-10-21)** — Next.js production build intermittently fails during page-data collection with missing page-entry errors while the local dev server is active; recurred 2026-10-22 with missing `/favicon.ico/route`, `/login/page`, and other page modules. Action: Shared `.next` state is the likely cause but is not proven. The full frontend build passed from an isolated frontend copy on both occurrences; isolate production builds if this recurs
- **D59 (W-HOST, 2026-10-22)** — Initial frontend dependency restore failed in the restricted environment: `ENOTFOUND` for `registry.npmjs.org`; the default npm cache also rejected log writes. Action: Resolved by restoring lockfile-pinned dependencies with approved network access and a `/private/tmp` npm cache. Baselines pass: backend build; frontend 143 tests, lint, and production build using local placeholder public config
- **D60 (W-HOST, 2026-10-22)** — The plan's `./gradlew checkstyle` shorthand is ambiguous because Gradle exposes `checkstyleMain` and `checkstyleTest`; the first invocation stopped before checks. Action: Resolved: plan invokes both tasks explicitly; T1 Checkstyle and the full backend build pass
- **D61 (W-HOST, 2026-10-22)** — T2's file list omitted `CreateListingRequest` and `UpdateListingRequest`, which need the approved title/description length constraints. Action: Plan-only correction added both DTOs to T2; the 120-character title and 5,000-character description limits are unchanged
- **D62 (W-HOST, 2026-10-22)** — `HostListingsControllerTest` context fails because `ListingService` is not registered; Spring's condition report says `PropertyRepository` is unavailable to service `@ConditionalOnBean` checks, although JPA later discovers 11 repository interfaces. Action: Resolved by operator approval C129; T0 now activates 15 service contracts and three JDBC adapters via explicit property
- **D63 (W-HOST, 2026-10-22)** — The partial `ListingServiceImpl.findByHostId` used `PageRequest.of(page, size + 1)`, whose offset is `page * (size + 1)` and skips rows on later pages. Action: Resolved in T2 with `OffsetLimitPageable` and repository plus controller page-continuity regression tests
- **D64 (W-HOST, 2026-10-22)** — The partial `HostListingController` was unconditional while `HostIdentityResolver` is gated by `snoozeshare.security.enabled`, making the default auth-disabled profile fail startup. Action: T2 now gates the controller on the same security property; DB-backed controller integration enables security, while dev and DB-free contexts omit the protected Host route
- **D65 (W-HOST, 2026-10-22)** — T3's approved file list omitted the create-block request DTO and audit-action enum; E18 described two different Hosts writing the same listing, which ownership rules make impossible. Action: Plan now includes the missing files; the concurrency regression will issue two simultaneous requests as the listing's single authorized Host, preserving both E3 ownership and the overlap invariant
- **D66 (W-HOST, 2026-10-22)** — The first complete T4 backend check hit a Gradle test-worker `EOFException` despite the focused new tests passing. Action: Re-running the same full backend Checkstyle/build completed successfully; no code or test changes were needed to clear it
- **D67 (W-HOST, 2026-10-22)** — T5's original file list omitted `BookingRepository` and `TicketRepository`, although existing summary services load and sort unbounded parent lists. Action: Added both repositories to T5 so the required summary pages are bounded in the database, not only the message histories
- **D68 (W-HOST, 2026-10-22)** — Host conversation summaries reached existing JDBC unread-count queries that compared `sent_at` to `last_read_message_id` (UUID), causing PostgreSQL type errors. Action: Corrected both booking and ticket read adapters to compare against the joined last-read message's `sent_at`; added the two adapters to T5
- **D69 (W-HOST, 2026-10-22)** — T5's checklist said every edge case applied identically to both channels, although E8 is ticket-specific. Action: Corrected the checklist coverage wording; E20 rollback behavior is verified for both booking and ticket messages
- **D70 (W-HOST, 2026-10-22)** — T6's concurrent-withdrawal regression showed both withdrawals succeeding because `WalletServiceImpl.move` loaded the wallet before `LedgerWriter` acquired its pessimistic row lock. Action: Resolved with ID-only wallet lookup before ledger posting; the lock now hydrates the current row, and concurrency test confirms one success/one insufficient-balance response
- **D71 (W-HOST, 2026-10-22)** — T6's first bounded statement read exposed `JdbcLedgerRepository` using `ResultSet.getObject(..., Instant.class)`, unsupported by the PostgreSQL JDBC driver for `timestamptz`. Action: Map via `getTimestamp(...).toInstant()` and exercise real PostgreSQL statement reads in the Host wallet integration test
- **D72 (W-HOST, 2026-10-22)** — T6's event-contract review found `LedgerWriter` publishes `WalletTransactionRecordedEvent` before transaction commit, contrary to the approved API spec. Action: Register wallet event dispatch with transaction synchronization and test that rolled-back operations publish no event
- **D73 (W-HOST, 2026-10-22)** — A direct frontend `npx tsc --noEmit` check includes existing test files and reports unrelated typing errors in `HostListings.test.tsx` and `api.test.ts`. Action: The required Next.js production build type-checks successfully; errors remain confined to untouched test lines and were not changed or suppressed
- **D74 (W-HOST, 2026-10-22)** — The fixture calendar's fixed September 15–17 default range would be in the past once the API calendar followed the current month. Action: API mode now defaults to a valid upcoming range in the selected month; fixture presentation defaults remain unchanged
- **D75 (W-HOST, 2026-10-22)** — T8 touched more than 15 files, exceeding the repository's preferred commit review size. Action: Split the task into listing/forms and calendar/requests commits; both pass the same full frontend verification
- **D76 (W-HOST, 2026-10-22)** — Initial T10 browser attempts showed Host routes loading, and normal shell HTTP probes could not reach the host listener. Action: Resolved for browser verification: an approved local health probe confirmed the backend is healthy; authenticated Host routes loaded with API-backed empty states. A transient browser navigation block cleared after the Next build completed; all eight routes were subsequently checked at desktop, tablet, and mobile widths
- **D77 (W-HOST, 2026-10-22)** — Live calendar route with a valid but nonexistent listing ID returned 404 for its listing/calendar requests, but the page rendered only breadcrumbs with no unavailable message. Action: Regression test now confirms the safe unavailable message is rendered; calendar route and test updated
- **D78 (W-HOST, 2026-10-22)** — Live 390px viewport showed the shared navbar top row extended the document by 15px on New/Edit listing routes. Action: Allow the navbar top row to wrap; targeted regression passes, and all eight routes now render within a 390px document width
- **D79 (W-HOST, 2026-10-22)** — The initial live browser route sweep verified rendered route states and viewport bounds but did not capture exact Host HTTP request URLs, query parameters, or request counts required by N5. Action: Resolved in T10: authenticated Network captures recorded all eight route request sets, status codes, and actual GET counts in the implementation plan; browser OPTIONS preflights were excluded
- **D80 (W-HOST, 2026-10-22)** — On a later T10 resume, the owner had signed in; a fresh read-only capture showed `/api/auth/me` returning `200` and the Host listing route rendering API-backed state. Action: Authenticated acceptance can continue without credentials being handled by the agent; no mutations were performed
- **D81 (W-HOST, 2026-10-22)** — A clean authenticated development reload duplicated the initial listings and shared wallet reads; React Strict Mode replayed mount effects and the provider did not coalesce same-key in-flight reads. Action: Added provider-level in-flight request sharing for keyed page resources and wallet summary reads; the Strict Mode regressions pass, all 176 frontend tests/lint/build and backend build pass, and a fresh `/host/listings` capture showed one request each for `/api/auth/me`, `/api/host/listings?page=0&size=50`, and `/api/host/wallet?page=0&size=1`; the remaining route requests were later captured during T10 (D79)
- **D82 (W-HOST, 2026-10-22)** — A clean Wallet route capture duplicated `GET /api/host/wallet?page=0&size=50` under Strict Mode even though the summary and keyed page resources were already coalesced. Action: Added separate in-flight sharing for wallet statement pages and a Strict Mode regression test; the regression failed with two page reads before the fix, passed after it, and a fresh authenticated Wallet capture now shows one statement request and one wallet summary request
- **D83 (W-HOST, 2026-10-22)** — Read-only T10 review found stale gate wording and a handoff that still said route captures remained. Action: Reconciled the evidence and next-action records; review found no code findings, and the final frontend/backend gates and plan closeout lint now pass
- **D84 (W-HOST, 2026-10-22)** — T11's approved file list omitted `PropertyRepositoryTest`, which directly asserted the listing page-query API that T11 removes. Action: Resolved: add the repository test to T11 and replace paging assertions with complete stable-order and owner-scope coverage; no API scope change
- **D85 (W-HOST, 2026-10-22)** — T14's original file list omitted the four API-mode Host pages, which still consumed page-envelope types and separate wallet summary/statement calls. Action: Resolved: include those route adapters and their route tests in T14 so each commit remains type-safe and consumes the complete response; T15/T16 still remove the now-inert pagination controls and handlers
- **D86 (W-HOST, 2026-10-22)** — T3's full frontend run found `HostListings.test.tsx` still asserted the prior `62%` presentation after occupancy changed to fixed two-decimal formatting. Action: Resolved: added the existing fixture-backed test to T3's file list and updated its assertion to `62.00%`; no scope or behavior change
- **D87 (W-HOST, 2026-10-22)** — T4's empty-range change also invalidated the existing local-only calendar interaction test, which was not listed in T4's file inventory. Action: Resolved: added `HostPresentationInteractions.test.tsx` to T4 and changed it to choose a partial range explicitly; full frontend tests pass
- **D88 (W-HOST, 2026-10-22)** — Read-only closeout review found the service query-count test used only a two-listing mocked batch, though the approved T1 checklist calls for sizes 1 and 101. Action: Added service coverage for both sizes and verified exactly two owner-scoped grouped queries in each case; backend checks rerun
- **D89 (W-HOST, 2026-10-22)** — The formerly approved API/edge-case text said overlap returns 422 `DOMAIN_RULE_VIOLATION`, while the implementation returned 409 `DATE_RANGE_UNAVAILABLE`. Action: Resolved by C140: retain 409 and specify `BOOKING_OVERLAP` / `HOST_BLOCK_OVERLAP`, booking precedence, and safe non-disclosing messages; API spec, edge cases, requirements, and T3 plan record now agree with implementation
- **D90 (W-HOST, 2026-10-22)** — Calendar integration tests assert overlap status/code and precedence, but do not assert the complete error body or absence of conflicting IDs/dates; the shared handler unit test verifies the generic fixed message. Action: Non-blocking QA/Attacker review finding. The documented safe contract matches the handler; accepted as a test-coverage limitation for this approval, with no behavior change (second-round spec review).

## 10. Record

The Done ledger lives in **[`docs/project-state/done-ledger.md`](docs/project-state/done-ledger.md)** — every change, big or small, newest first.

- **Latest entry:** 2026-10-22
- **Entries:** 220 (iteration 2 started; earlier entries in the archive)
