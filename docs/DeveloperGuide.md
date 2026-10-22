---
layout: default
title: Developer Guide
permalink: /developer-guide/
---

# Developer Guide — SnoozeShare

Handover brief for developers taking over this project. It describes **confirmed** work only and
is updated at checkpoints, so it can lag behind the code. For current status, work in flight, and
what to build next, read [`PROJECT_STATE.md`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/PROJECT_STATE.md) — that is the source of truth.

**Last updated:** 2026-10-22 — covers W1 to W16, the MP3-0 fixes, the W-INFRA web skeletons and dev deployment, the W-AUTH web authentication, the W-DOMAIN ported domain layer, and the W-AI-GATEWAY LLM gateway; the desktop sections were checked against the built app on 2026-10-19, the web sections against `backend/`, `frontend/`, `render.yaml` and the live health check on 2026-10-20, the W-AUTH parts against `backend/src/main/java/com/snoozeshare/auth`, `frontend/src` and their tests on 2026-10-21, the W-DOMAIN parts against `backend/src/main/java/com/snoozeshare/domain`, `repository`, `service`, `api` and their tests on 2026-10-22, and the W-AI-GATEWAY parts against `backend/src/main/java/com/snoozeshare/ai`, its tests and the live SoCLaaS checks on 2026-10-22

## Contents

1. [Overview & Scope](#1-overview--scope)
2. [Setting Up](#2-setting-up)
3. [Architecture](#3-architecture)
4. [Components](#4-components)
5. [Requirements](#5-requirements)
6. [Glossary](#6-glossary)
7. [Testing](#7-testing)
8. [Acknowledgements](#8-acknowledgements)

---

## 1. Overview & Scope

SnoozeShare is a Java 25 / JavaFX desktop application for short-term property rentals. Guests book
stays, hosts list properties, and support agents settle disputes between them. All money moves
through one wallet and transaction ledger, and the booking price is held in escrow until the stay
and its dispute window are over. The desktop app runs as a single process against an embedded SQLite
database; there is no server.

The project is being re-platformed as a web app (C76). The repository is a mono-repo of three
independent builds: `desktop/` (this guide's JavaFX app), `backend/` (a Spring Boot API on PostgreSQL) and
`frontend/` (a Next.js app). The web side has Firebase authentication with role authorization ([4.18](#418-authentication-and-role-authorization-backendauth-frontend-providers)), the full domain model, repositories and core services ported to Spring Data JPA ([4.19](#419-domain-model-repositories-and-services-backend-domain-layer)), a shared SoCLaaS LLM gateway for the AI features, off by default ([4.20](#420-ai-gateway-backendai)), CI and a live dev deployment. No REST controllers expose the domain services yet; the role pages are empty landing shells (see [3.2](#32-web-platform-and-deployment)).

**Target user:** the three roles the app serves — Guest (renter), Host (homeowner) and Support
Agent (platform admin). This guide documents the role shells, Host operations, Support Agent
dispute-resolution tooling, messaging, wallets and the platform audit trail.

**Delivered features**

| Feature | What it is | Section |
|---|---|---|
| F0 (W1) | Shared foundation: mocked auth/registration, role shells, SQLite persistence, wallet ledger, events, audit and session boundaries | [3.1](#31-shared-foundation) |
| F1 (W2) | Guest listing search and property discovery: city, capacity and date availability filters, detail view and cost estimate | [4.8](#48-guest-portal-uiguest) |
| F2 (W3) | Guest booking and Trip Hub: escrow-backed requests, trip groups, cancellation/refunds and event refresh | [4.8](#48-guest-portal-uiguest) |
| F3 (W4) | Guest feedback and disputes: ticket filing/history, requested remedies, supporting text and one post-stay review | [4.8](#48-guest-portal-uiguest) |
| F4 (W5) | Guest wallet management: shared wallet page, balance, statement, top-up, withdrawal and escrow display | [4.8](#48-guest-portal-uiguest) |
| F5 (W6) | Host listing management: create/edit, publish, status controls, listing details and metrics | [4.9.2](#492-host-listing-management-uihostlistings) |
| F6 (W7) | Host calendar: monthly availability, inclusive manual date blocks and removal | [4.10](#410-host-calendar-and-date-overrides-uihostcalendar) |
| F7 (W8) | Host booking requests, approval/rejection messages, earnings previews and guarded completion | [4.11](#411-host-booking-requests-uihostbookings) |
| F8 (W9) | Host wallet management: shared wallet page, transaction statement, escrow summary, top-up and withdrawal modals | [4.12](#412-host-wallet-management-host-integration-of-uicommonwallet) |
| F9 (W10) | Agent dispute resolution: ticket queue, assign / unassign / notes, settlement of held escrow, ticket categories | [4.1](#41-settlement-domain)–[4.13](#413-agent-ui-uiadmin) |
| F10 (W11) | Agent account governance: suspend/reactivate Guest and Host accounts, with atomic booking/listing cascade | [4.15](#415-account-governance) |
| F11 (W12) | **Platform Audit Trail:** every change is logged as one typed row, and agents search the log on the Audit Log screen | [4.14](#414-audit-trail) |
| F12 (W13) | Persistent ticket and booking conversations, with Guest and Host Messages inboxes | [4.4](#44-ticket-messaging-messageservice), [4.4a](#44a-booking-messaging-bookingconversationservice), [4.8](#48-guest-portal-uiguest) and [4.9](#49-host-portal-uihost) |
| — (W14) | **Unified Ledger:** every wallet movement is a single `audit_log` money row (with the wallet's running balance); the System user gets a real role and wallet and receives the platform's 3% fee | [4.16](#416-unified-ledger-ledgerwriter) |
| — (W-AUTH) | **Web authentication:** Firebase sign-up and sign-in, registration with Host and Agent codes, role-based API authorization, and client-side route guards | [4.18](#418-authentication-and-role-authorization-backendauth-frontend-providers) |
| — (W-DOMAIN) | **Web domain layer:** 12 JPA entities, 12 Spring Data repositories, 15 service implementations, 16 request DTOs and 21 response DTOs ported from the desktop app to Spring Boot with Spring Data JPA on PostgreSQL | [4.19](#419-domain-model-repositories-and-services-backend-domain-layer) |
| — (W-AI-GATEWAY) | **LLM gateway:** one backend entry point, `LlmGateway.run`, for every role's AI feature, with input and output guardrails, a shared rate and cost budget, per-user and daily caps, a metadata-only audit row, mock mode and fallbacks; off by default, no role feature yet | [4.20](#420-ai-gateway-backendai) |

**Value proposition:** a support agent can take a dispute ticket, read both parties' evidence and
chat, and settle the booking's held escrow between guest and host in one atomic, audited action.
Every payout follows one uniform fee rule, so the ledger always adds up.

**Goals**

- Every dispute ticket ends in a fully settled booking: the whole held escrow reaches the guest, the
  host, or both, and nothing is paid twice.
- The platform fee is taken only from host earnings, never from guest money.
- Any failure during settlement leaves wallets, ledger, ticket and booking exactly as they were.
- Every agent mutation leaves an audit record.
- Every supported audit hook for a booking, ticket, listing, ticket category or wallet balance writes
  rows that commit or roll back with the change itself; an agent can search them by user, id, action
  type and date (F11). Booking decision-state and Host calendar-block coverage remains incomplete.
- The ticket categories guests choose from are maintained by agents, not by code changes.

**Out of scope**

- **Force Cancel / Force Complete (backlog F9.2.1)** — redundant: accepting a ticket with a full
  refund already cancels in effect, and rejecting it already completes, and both close the ticket (C22).
- **The design canvas palette outside the agent screens** — the Agent and Guest shells currently
  load `agent-theme.css`, while the Host shell owns `host-theme.css`. The Agent-only canvas layout
  does not define the Host navigation or page styling (C24).
- **Suspending Agent accounts, and undoing a suspension's side effects** — agents are listed but cannot be
  suspended (including by themselves), and reactivating an account changes only its status and reason:
  cancelled bookings are not reinstated and deactivated listings stay inactive (C34, C36).
- **A separate auto-complete service** — `BookingService.completeEligibleBookings()` is the current
  completion sweep; it skips bookings with an open or in-review ticket and is invoked by the app
  startup and Host Requests flow (C17, W8).
- **Clawing money back from a host wallet** — agents only ever settle *held* escrow (C17).
- **Microservices, message brokers, horizontal scaling, JPMS modules** — ruled out for a course-scoped single-instance app (`PROJECT_STATE.md` § Known Gaps).

---

## 2. Setting Up

**Prerequisites**

| Tool | Version | Notes |
|---|---|---|
| JDK | 25 | `desktop/build.gradle` and `backend/build.gradle` set a Java 25 toolchain |
| Gradle | 9.7.1 | Each of `desktop/` and `backend/` has its own wrapper (`gradlew`, `gradlew.bat`); nothing to install |
| JavaFX | 25.0.4 | Fetched by the `org.openjfx.javafxplugin` Gradle plugin; no separate SDK |
| SQLite | embedded | `org.xerial:sqlite-jdbc` is a Gradle dependency; the `sqlite3` CLI is only needed to rebuild `desktop/db/snoozeshare-mock.db` |
| Docker | current | Local Postgres (`docker-compose.yml`) and the backend tests (Testcontainers) |
| Node.js | 22 | `frontend/.nvmrc`; `engine-strict` rejects other majors |
| PostgreSQL | 17 | `postgres:17` in `docker-compose.yml`; nothing to install |
| Spring Boot | 3.5.16 | `backend/build.gradle` |
| Next.js | 15.5.28 | `frontend/package.json` |

**Desktop: build, test, run** (Windows PowerShell; on macOS/Linux use `./gradlew`). There is no root build: run these inside `desktop/`.

```powershell
cd desktop
.\gradlew build   # compile, checkstyle, tests
.\gradlew test    # tests only
.\gradlew run     # start the app (main class com.snoozeshare.app.Launcher)
```

**Backend: build, test, run.** Start the database, then the app; the default `dev` profile connects to it.

```powershell
docker compose up -d postgres
cd backend
.\gradlew bootRun    # http://localhost:8080/actuator/health
.\gradlew build      # compile, Checkstyle, tests (needs Docker)
```

If port 5432 is taken, set `POSTGRES_PORT` (for example `$env:POSTGRES_PORT='5433'`) before `docker compose up` and before `bootRun`. On a machine short of memory, run `.\gradlew checkstyleMain checkstyleTest` and `.\gradlew test` as separate commands.

**Frontend: install, run, test.**

```powershell
cd frontend
npm ci
npm run dev      # http://localhost:3000
npm test         # Vitest
npm run lint
npm run build    # needs NEXT_PUBLIC_API_BASE_URL
```

**Turn authentication on locally.** With `FIREBASE_ENABLED` unset the `dev` profile leaves every path open and the
`/api/auth` endpoints do not exist, so the health page works without a Firebase project. To sign in you need a Firebase
project with the Email/Password provider enabled and a service-account key kept outside the repository. In PowerShell,
from `backend/` (the placeholders are yours to choose; never commit real values):

```powershell
$env:FIREBASE_ENABLED = "true"
$env:FIREBASE_PROJECT_ID = "<your-firebase-project-id>"
$env:SNOOZESHARE_HOST_REGISTRATION_CODE = "<code-for-hosts>"
$env:SNOOZESHARE_AGENT_REGISTRATION_CODE = "<code-for-agents>"
$env:GOOGLE_APPLICATION_CREDENTIALS = "<path-to-service-account.json>"
.\gradlew bootRun
```

Then put the six `NEXT_PUBLIC_FIREBASE_*` values from the Firebase console, plus `NEXT_PUBLIC_API_BASE_URL`, in the
gitignored `frontend/.env.local` and run `npm run dev`. With auth on, the backend fails at startup if the project ID, either
code or the credentials are missing.

**Run the AI gateway locally without SoCLaaS.** The gateway is off by default. To switch it on in mock mode (each feature answers from its own `mockResponse`; no key and no network), set these before `bootRun`. No role feature uses the gateway yet, so the settings take effect when one registers an `AiFeature` ([4.20](#420-ai-gateway-backendai)):

```powershell
$env:SNOOZESHARE_AI_ENABLED = "true"
$env:SNOOZESHARE_AI_MOCK = "true"
.\gradlew bootRun
```

**Continuous integration.** Two workflows live in `.github/workflows/`. `ci.yml` has two jobs, `checkstyle` and `build`, and each covers all three components (steps are named `desktop:`, `backend:` and `frontend:`). `checkstyle` runs on every push to any branch: Checkstyle for `desktop/` and `backend/`, and `npm run lint` for `frontend/`. `build` runs on every pull request and on every push to `development` and `main`: the desktop build (under `xvfb-run` so the JavaFX suites have a display), the backend build (its tests use Testcontainers on the runner's Docker) and the frontend `npm test` and `npm run build`. A failed run uploads the `checkstyle-reports` and `test-reports` artifacts. The frontend production build also needs the six public `NEXT_PUBLIC_FIREBASE_*` values; CI reads them from repository variables and falls back to dummy values, so builds without those variables (forks, Dependabot) still pass. `jekyll-gh-pages.yml` builds `docs/` with Jekyll and publishes it to GitHub
Pages on every push to `main` (Settings > Pages > Source is GitHub Actions). Making the `build` check
required before merge is a repository branch-protection setting, not something the workflows enforce.

**Environment variable**

| Name | Purpose | Where to obtain the value |
|---|---|---|
| `POSTGRES_PORT` | Optional host port for the local Postgres container and the backend's `dev` profile (default 5432) | Choose a free port |
| `PORT` | Backend listening port (default 8080); the host sets it | The hosting platform |
| `NEXT_PUBLIC_API_BASE_URL` | Backend base URL for the frontend, without a trailing slash (default `http://localhost:8080` in development; required for a production build) | `frontend/.env.example`; the Render URL on Vercel |
| `SPRING_PROFILES_ACTIVE` | `prod` on the hosted backend; unset means `dev` | The runbook |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | `prod` database settings, no defaults; the URL is a JDBC URL ending in `?sslmode=require` | Neon (see the runbook) |
| `SNOOZESHARE_CORS_ALLOWED_ORIGIN` | `prod` only: the one browser origin allowed to call the backend | The Vercel production domain |
| `FIREBASE_ENABLED` | `true` turns on Firebase token verification and role authorization. In `dev` it defaults to `false` (every path is open) and also supplies `snoozeshare.security.enabled`; in `prod` it must be `true` | You locally; Render |
| `SNOOZESHARE_SECURITY_ENABLED` | `prod` only: must be `true`, or startup fails | Render |
| `FIREBASE_PROJECT_ID` | The Firebase project whose ID tokens the backend accepts; required when auth is on | Firebase console > Project settings |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to the Firebase Admin service-account JSON (Application Default Credentials); keep it out of git | Firebase console > Project settings > Service accounts |
| `SNOOZESHARE_HOST_REGISTRATION_CODE`, `SNOOZESHARE_AGENT_REGISTRATION_CODE` | The two secrets a person must enter to register as a Host or an Agent; required when auth is on; use long random values | Chosen by the operator; set on Render |
| `SNOOZESHARE_TRUST_FORWARD_HEADERS` | `true` makes the backend take the client address for rate limits from `CF-Connecting-IP` (Render sits behind Cloudflare); `false` uses the direct peer. `dev` defaults to `false`; `prod` requires it | Render |
| `SNOOZESHARE_AI_ENABLED` | Turns the LLM gateway on. Defaults to `false` in `dev`; in `prod` it has no default and startup fails naming it when unset (`render.yaml` sets `"false"`) | Render |
| `SOCLAAS_BASE_URL`, `SOCLAAS_API_KEY` | The NUS SoCLaaS base URL (ending `/v1`, `https` in `prod`) and your environment's own key. Needed only when the gateway is enabled and not in mock mode. Development and production use separate keys; never commit one | SoCLaaS portal (`soclaas-portal.comp.nus.edu.sg`) and the integration guide |
| `SNOOZESHARE_AI_MOCK` | `true` answers AI calls from each feature's mock response instead of calling SoCLaaS; the `prod` profile rejects it | Local only |
| `SNOOZESHARE_AI_MODEL_FAST`, `_STANDARD`, `_STRONG` and other `snoozeshare.ai.*` keys (for example `SNOOZESHARE_AI_MAX_RPM`) | Optional overrides of the model names and limits listed in [4.20](#420-ai-gateway-backendai) | `GET /v1/models` for model ids |
| `NEXT_PUBLIC_FIREBASE_API_KEY`, `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN`, `NEXT_PUBLIC_FIREBASE_PROJECT_ID`, `NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET`, `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID`, `NEXT_PUBLIC_FIREBASE_APP_ID` | The public Firebase web-app configuration; all six are required for sign-in and for a production build | Firebase console > Project settings > Your apps |
| `SNOOZESHARE_DB_URL` | Optional JDBC URL that points the app at a SQLite file, for example `jdbc:sqlite:build/acceptance.db`. When unset or blank the app opens `desktop/db/snoozeshare-mock.db` if that file exists in the working directory (it does when you run from the project root); only if the file is missing does it fall back to an empty in-memory database, which nothing survives a restart. | Build it from a copy of the mock DB (below). No secret is involved. |

`Main.resolveDatabaseUrl` picks the database in this order: an explicit `SNOOZESHARE_DB_URL`, then `desktop/db/snoozeshare-mock.db` if it exists, otherwise in-memory. The result goes to `AppContext.create(String)`, or to `AppContext.create()` for the in-memory case.

**Run the desktop app against a copy of the mock database** (from `desktop/`)

`desktop/db/snoozeshare-mock.db` is a committed, pre-populated reference database. A plain `.\gradlew run`
opens it directly, and the app writes to whatever it opens, so your changes land in the committed
file and show up in `git status`. To keep the reference database clean, copy it first:

```powershell
Copy-Item db\snoozeshare-mock.db build\acceptance.db -Force
$env:SNOOZESHARE_DB_URL = "jdbc:sqlite:build/acceptance.db"
.\gradlew run
Remove-Item Env:SNOOZESHARE_DB_URL   # when finished
```

The first start records the schema baseline in `schema_history` (D10); the copy needs no other
preparation.

**Mock logins.** Authentication is mocked (`UserService.authenticate(email)`): the login screen takes
an email and checks no credential. The mock agents seeded in [`desktop/db/seed-mock-data.sql`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/db/seed-mock-data.sql) are
`amy.tanaka@snoozeshare.test`, `ben.alvarez@snoozeshare.test` and `chen.wu@snoozeshare.test`. Registering
a Host or Agent through the app needs a registration code; those are mock constants in
[`RegistrationCodes`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/config/RegistrationCodes.java).

**Rebuilding the mock database** (only after editing the seed):
`sqlite3 x.db < desktop/db/schema.sql` then `sqlite3 x.db < desktop/db/seed-mock-data.sql`, and replace
`desktop/db/snoozeshare-mock.db`. See the conventions in [Testing](#7-testing) before editing the seed.

---

## 3. Architecture

The desktop app is a layered modular monolith in one Gradle module (sections 3.1 and 4). The web platform is described in [3.2](#32-web-platform-and-deployment). Dependencies run strictly downward:
`ui` → `service` → `domain` and `repository` → `infra.db`. The diagram shows the dispute-resolution
slice only; `AppContext` wires every box and hands the services to the UI.

### 3.1 Shared foundation

W1 establishes the application boundaries used by every later workstream:

- `Main`/`Launcher` start the native JavaFX application; `SceneRouter` selects the Guest, Host or
  Support Agent shell after authentication. `HostShellController` owns the Host Portal's four
  center-replacement tabs: Listings, Requests, Messages and Wallet.
- `AuthController` provides the combined Login/Register screen. Authentication is intentionally
  mocked by email; registration codes gate Host and Agent registration, while role checks remain in
  the service layer.
- `AppContext` owns the SQLite connection and wires repositories, services, the in-process event bus,
  `SessionContext`, and the role-aware scene router. Controllers receive services through this context
  and do not access JDBC directly.
- `ConnectionFactory`, `MigrationRunner` and `TransactionManager` define the embedded SQLite
  persistence boundary. Repository interfaces isolate domain/service code from `repository.jdbc`.
  `TransactionManager` takes an optional `TransactionListener` (`onBegin`, `afterCommit`,
  `afterRollback`); `inTransactionUnchecked` wraps work that cannot throw `SQLException`.
- `domain.policy` is the single home of two date rules, each built from a `Clock` only:
  [`DisputeWindowPolicy`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/policy/DisputeWindowPolicy.java)
  (a ticket can be filed from check-in through check-out + 7 days; a `CONFIRMED` booking auto-completes
  only from check-out + 8 days, so an accepted dispute always reaches a booking that can still be
  settled; C48) and
  [`StayDatePolicy`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/policy/StayDatePolicy.java)
  (a stay or a search range may not start or end in the past, and check-out must follow check-in;
  `requireNotInPast` backs Host calendar blocks). Services call these; controllers and services never
  re-implement the rules. `AppContext` builds one `Clock.systemDefaultZone()` and passes it to the
  policies, the completion sweep and the services, so the UI and the rules agree on what "today" is.
- `WalletService` provides atomic top-up, withdrawal, balance and statement operations.
  Every wallet-moving service writes through `LedgerWriter` ([4.16](#416-unified-ledger-ledgerwriter)),
  which updates a wallet's `balance` and writes its `audit_log` money row (with the new
  `balanceAfter`) in the same transaction; there is a single ledger, not a separate one.
- `EventBus`/`InProcessEventBus` publish committed domain events after successful transactions, and
  the shared audit contracts provide typed audit records for mutating services. Services do not publish
  wallet events themselves: [`PostCommitEvents`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/infra/events/PostCommitEvents.java)
  is the `TransactionListener` that queues events during a transaction, publishes them in order through an
  `EventSink` (the one-method seam over the bus) after commit, and discards them on rollback. It is one
  shared queue, which relies on the single-process assumption (C50); nested transactions are rejected.

The foundation is a single-process desktop boundary: there is no server, external authentication,
message broker or distributed transaction coordinator. Package dependency tests protect the key
domain/UI and repository/JDBC boundaries.

```mermaid
flowchart TB
  subgraph UI["ui.admin (JavaFX)"]
    Shell["AdminShellController"]
    Detail["DisputeDetailController"]
    Dialog["ResolutionDialogController"]
  end
  subgraph Service["service"]
    Tickets["TicketServiceImpl"]
    Settle["DisputeSettlementServiceImpl"]
    Query["DisputeQueryServiceImpl"]
    Chat["MessageServiceImpl"]
    BookingChat["BookingConversationServiceImpl"]
    Audit["AuditServiceImpl"]
  end
  subgraph Domain["domain"]
    Calc["SettlementCalculator + EscrowPolicy"]
    SM["TicketStateMachine + BookingStateMachine"]
  end
  Repos["repository.jdbc.Jdbc*Repository"]
  Tx["TransactionManager"]
  Bus["InProcessEventBus"]
  DB[("SQLite")]
  Shell --> Detail
  Detail --> Dialog
  Detail --> Tickets
  Detail --> Query
  Detail --> Chat
  Shell --> BookingChat
  Tickets --> Settle
  Settle --> Calc
  Settle --> SM
  Settle --> Tx
  Settle --> Audit
  Settle --> Bus
  Tickets --> Repos
  Query --> Repos
  Settle --> Repos
  Chat --> Repos
  BookingChat --> Repos
  Chat --> Bus
  BookingChat --> Bus
  Repos --> DB
```

Left out for clarity: `AppContext` wiring, `SessionContext`, the other repositories (booking,
property, user, wallet, ledger), and the queue and category screens, which call the same services.
Services with audit hooks (ticket, listing, governance, settlement and the ledger writer) write
through `AuditService`; booking and availability mutations currently have ledger/event behavior but
do not emit an audit row for every state or block change. The Audit Log screen calls its `search`.
`ui.host.messaging.HostMessagesController` (see [4.9](#49-host-portal-uihost)) and
`ui.guest.messaging.GuestMessagesController` are the built UI callers of
`BookingConversationServiceImpl`; both also call `MessageServiceImpl` for ticket threads.
`DisputeDetailController` is shown here as the Agent-side caller of `MessageServiceImpl`. Guest and Host pages use the same service/repository boundaries: controllers
receive capabilities through `AppContext`, while
`host-theme.css` owns Host shell and page styling and the common wallet module owns wallet presentation.

**Audit rows are written inside the change's own transaction.** A service calls
`AuditService.record(...)` while its `TransactionManager.inTransaction` block is open. The audit
repository shares the transaction's connection, so the audit rows commit with the change they describe
and roll back with it: a failed change leaves no audit row, and an audit write that fails rolls the
change back. See [4.14](#414-audit-trail).

**One request: an agent resolves a ticket**

1. In `DisputeDetailController` the agent presses Accept, Reject dispute or Manual adjustment.
   `ResolutionDialogController` collects the reason and, where needed, a guest refund amount (with a live
   `ResolutionPreview`), and returns a `ResolutionRequest(mode, guestRefund, reason)`.
2. The controller calls `TicketService.resolve(ticketId, request, agentId)`.
3. `TicketServiceImpl.resolve` checks the caller is an agent, loads the ticket and booking, and
   turns the request into a single guest refund figure: Reject is 0, Manual uses the entered amount,
   and Accept uses the ticket's requested remedy (the whole escrow for `FULL_REFUND`, 0 for
   `HOST_PAYOUT`, the entered amount otherwise). It then calls `DisputeSettlementService.settle`.
4. `DisputeSettlementServiceImpl.settle` requires a non-blank reason and the `AGENT` role, then runs
   the rest inside one `TransactionManager.inTransaction` call.
5. Inside the transaction it checks the preconditions (ticket `IN_REVIEW` and assigned to the caller;
   booking `CONFIRMED`; escrow still held per `EscrowPolicy.isHeld`), splits the escrow with
   `SettlementCalculator.split`, and confirms both state machines allow the transitions.
6. Still inside the transaction it updates the guest wallet and writes a ledger row (if the refund is
   above zero), does the same for the host (if the host share is above zero), saves the ticket as
   resolved, saves the booking as `COMPLETED`, and records the corresponding status and money audit rows.
7. The transaction commits; any exception rolls everything back and nothing is published.
8. After commit, `TicketResolvedEvent` and one `WalletTransactionRecordedEvent` per ledger row are
   published on the `InProcessEventBus`. `DisputeQueueController` subscribes to the ticket event and
   refreshes its rows.

**Main parts**

- **`ui.admin`** owns the agent screens. It may call `service` interfaces, the domain enums and
  records, and `AppContext` (to reach services). It must not import `repository` or `java.sql`.
- **`service`** owns business rules. `TicketServiceImpl` owns the ticket lifecycle and categories,
  `DisputeSettlementServiceImpl` is the only class that moves money for a dispute, and
  `DisputeQueryServiceImpl` builds read models for the screens.
- **`domain`** is pure Java (no JavaFX, no JDBC): the settlement maths, the escrow-held test and the
  state machines.
- **`AccountGovernanceService`** (`AccountGovernanceServiceImpl`) owns account suspension and reactivation.
  A suspension and its cascade (force-cancelled bookings with refunds, deactivated listings, audit rows) run
  in one transaction, and its events publish after commit ([4.15](#415-account-governance)).
- **`AuditService`** (with `AuditServiceImpl` and `JdbcAuditLogRepository`) owns the audit log: services
  write rows through it, and only the Audit Log screen reads them ([4.14](#414-audit-trail)).
- **`repository.jdbc`** is the only place that touches `java.sql`. **`infra.db`** owns the connection,
  the transaction boundary and migrations.

**Key Decisions**

| Decision | Why | Rejected | Ref |
|---|---|---|---|
| Escrow is held through the 7-day dispute window; a ticket opened in it leaves escrow held and the agent controls settlement. Agent money actions therefore always act on held escrow, with no clawback. | Consistent with the backlog's auto-complete and guest dispute rules; removes the "already paid out" case. | | C17 |
| Every resolution settles the **full** held escrow: guest refund `R` with no fee, host receives `(escrow − R) × 0.97`. Accept is the requested remedy, Reject is `R = 0`, Manual is an agent-set `R`. | The platform cut is only ever taken from host earnings, never from guest money. | The mockup's single-wallet "Adjust wallet" dropdown | C20 |
| A separate `DisputeSettlementService` moves the money, instead of `TransactionService.applyTicketRemedy` / `manualOverride`. | Those single-sided methods cannot express a two-sided full-escrow settlement. Also keeps W10 from editing shared W3 code. | | C16, D5 |
| Ticket chat runs through a general `MessageService` interface; W10 ships a temporary in-memory implementation. | Agent chat is one more participant in a general messaging feature, so it is not owned by dispute resolution. | W10 owning a `ticket_messages` table | C21 |
| No Force Cancel / Force Complete. The agent only settles by resolving a ticket. | Accepting or rejecting already closes the ticket and settles the booking. | Force actions on the detail screen | C22 |
| Resolving a ticket moves the booking `CONFIRMED → COMPLETED`, so `BookingStateMachine` allows `Role.AGENT` on that transition. "Stay ended — escrow held" is a derived display label, not a status. | Bookings in the dispute window are `CONFIRMED`; the label avoids a new status. | | C23 |
| The agent shell uses the design canvas layout (top bar plus tab strip Disputes / Accounts / Audit Log / Categories) and "Fall Light" palette, applied through `agent-theme.css` on the agent scene only. | Operator ruled canvas differences to be defects after a visual check. | Keeping the sidebar and navy theme as a documented deviation (D11) | C24 |
| Internal notes are one persisted free-text field per ticket (`Ticket.agentNotes`), replaced on Save. An assigned agent can Unassign (`IN_REVIEW → OPEN`), which is audited. | Operator preference after a visual check. | A note history list; disabling Assign after assigning | C25 |
| Agents can delete a category, but not while any ticket uses its label; deactivate it instead. Modals get a 50% light-grey scrim, and chat and notes boxes are user-resizable. | Delete is a new capability beyond backlog F9.3.1; protecting used labels keeps ticket history intelligible. | | C26 |
| `UNDER_REVIEW` was renamed `IN_REVIEW` in code, schema, seed data and UI text. | One term across code and UI. | A UI-only rename | C27 |
| The audit log is one row per change: status text in `beforeState` / `afterState`, money in a typed `walletAdjustment` column, plus typed reason and id columns. | The old single `afterState` blob was unreadable and unsearchable. | Two per-side booking rows; keeping JSON in `afterState`; `entityId`-only lookups | C28 |
| **W14 (executed 2026-09-27, confirmed 2026-09-29):** every wallet movement is one `audit_log` money row carrying its wallet's `balanceAfter`, written through `LedgerWriter`. Until then, a movement wrote both a `wallet_transactions` row and an `audit_log` money row in one transaction (historical: D13). | The dual ledger could not be removed without touching W1/W3/W5/W6/W10 money paths, so it was scheduled as its own workstream instead of folded into W12. | Doing the unification inside W12 | C31, C39–C42, D13 |
| **Reversed by W14:** the System user is role `SYSTEM`, status `ACTIVE`, and owns a real wallet that receives a `PLATFORM_FEE` row (via `LedgerWriter.postPayout`) whenever a payout's fee is positive. Until W14, the fee was recorded only as text in the host payout row's reason, with no platform wallet. | Keeps W12 small first; the operator wanted the fee as a real System-account entry, delivered by W14. | A platform wallet in W12 | C30, C39–C42 |
| Reactivate is built although backlog F10.1.1 says only suspend. Agent accounts are listed with no action and cannot be suspended. | The operator wanted suspension to be reversible; agents governing agents was out of scope. | Suspend-only | C34 |
| The suspension reason lives in a nullable `users.suspensionReason` column (migration V003) and is also written to the audit row. | Looking a reason up should not mean reading the audit log, which is not a data store. | A `suspensions` table; deriving the reason from `audit_log` | C34 (reverses the no-column part of C32) |
| Suspending a Guest or Host force-cancels their PENDING and not-yet-started CONFIRMED bookings (check-in after today) with a 100% refund; a Host's ACTIVE listings become INACTIVE. Started and ended stays are untouched, and reactivation restores nothing. | Upcoming stays cannot be honoured by a suspended party; a stay in progress settles through the normal path (W8 auto-completion, or W10 dispute resolution). | Cancelling PENDING only; reinstating on reactivation | C36 |


### 3.2 Web platform and deployment

The web side has authentication and the full ported domain layer: a Spring Boot API with Firebase auth, the domain model as JPA entities, Spring Data repositories, and 15 service implementations; a Next.js app with sign-in, registration and empty role landing pages; a shared LLM gateway for the AI features that is off by default, and a hosted dev environment that tracks the `development` branch. No REST controllers expose the domain services yet — the role pages are empty landing shells.

```mermaid
flowchart LR
  subgraph Local["Local development"]
    FE1["frontend: npm run dev :3000"]
    BE1["backend: bootRun :8080 (profile dev)"]
    PG1[("Postgres 17: docker-compose.yml")]
  end
  subgraph Hosted["Hosted dev environment (branch development)"]
    V["Vercel: frontend, Next.js"]
    R["Render: backend/Dockerfile via render.yaml (profile prod)"]
    N[("Neon: branch dev")]
  end
  FB["Firebase Authentication (Email/Password)"]
  SC["NUS SoCLaaS LLM (OpenAI-compatible API)"]
  FE1 -->|"GET /actuator/health"| BE1
  BE1 -->|"JDBC"| PG1
  V -->|"GET /actuator/health (one allowed origin)"| R
  V -->|"sign-up, sign-in (Firebase JS SDK)"| FB
  V -->|"Bearer ID token: /api/auth/*"| R
  R -->|"verify ID token (Firebase Admin)"| FB
  R -->|"JDBC, sslmode=require"| N
  R -->|"LlmGateway: chat/completions, Bearer key (off by default)"| SC
```

1. The browser loads the page from `frontend/` (locally on port 3000, in the hosted environment from Vercel).
2. `BackendStatus` calls `fetchBackendHealth` (`src/lib/api.ts`), which requests `NEXT_PUBLIC_API_BASE_URL` + `/actuator/health`; the component aborts the request after 5 seconds.
3. The backend answers only if the request's `Origin` equals `snoozeshare.cors.allowed-origin` (the actuator and application endpoints are configured separately but fed from that one property).
4. The health check runs a query against Postgres: locally the compose container, hosted a Neon branch. Flyway applied `V001__baseline.sql` through `V005__tighten_nightly_rate_constraint.sql` at startup (V002 auth, V003 domain tables, V004 system-user seed, V005 nightly-rate constraint; see [4.19](#419-domain-model-repositories-and-services-backend-domain-layer)).
5. The page shows `Backend: UP` for a 200 response whose JSON status is `UP`, and `Backend: unreachable` for anything else; `Re-check` repeats the request.
6. An authenticated request starts in the browser: `authenticatedFetch` (`src/lib/api.ts`) takes the signed-in user's Firebase ID token and sends it as `Authorization: Bearer <token>` (the sign-up and register flow is in [4.18](#418-authentication-and-role-authorization-backendauth-frontend-providers)).
7. `FirebaseAuthFilter` verifies the token with Firebase Admin, including a revocation check and the project ID; a missing or invalid token gets `401 INVALID_TOKEN`, and an unreachable Firebase `503 AUTH_PROVIDER_UNAVAILABLE`.
8. For `/api/guest/**`, `/api/host/**` and `/api/agent/**`, `RoleAuthorizationFilter` loads the `users` row by Firebase UID and compares its role with the URL namespace; a mismatch gets `403 ROLE_MISMATCH`. The controller then runs.
9. When a role feature calls the AI gateway (none does yet), `LlmGateway.run` sanitizes the input, checks the per-user and daily limits, sends one request to SoCLaaS through the global scheduler, validates the reply and writes one audit row, as described in [4.20](#420-ai-gateway-backendai).

The hosted backend runs from `backend/Dockerfile` on Render's free plan, so it sleeps when idle and the first
request after a pause is slow. `docs/runbooks/dev-environment.md` holds the deployment order, variables and
evidence.

**Hosting and deployment.** The full procedure, every variable and the dated evidence of the live checks are in `docs/runbooks/dev-environment.md`; this is what a newcomer needs to know first. Account-bound steps are done by a human, and an agent never enters credentials or tokens.

Order: Render first, then Vercel, then Render again, because the frontend needs the backend URL and the backend needs the frontend's domain for CORS.

1. **Render:** New > Blueprint from this repository. It reads `render.yaml` (a Docker web service on the free plan, branch `development`, build context `backend`, health check `/actuator/health`). Enter the `sync: false` secrets in the dashboard when prompted.
2. **Neon:** create a project and a branch `dev`. On Render set `SPRING_PROFILES_ACTIVE=prod`, the three `SPRING_DATASOURCE_*` values and a placeholder `SNOOZESHARE_CORS_ALLOWED_ORIGIN` (for example `http://localhost:3000`); add the Firebase Admin settings and deploy.
3. **Vercel:** create a project with root directory `frontend`, Production Branch `development` and `NEXT_PUBLIC_API_BASE_URL` set to the Render public URL (no trailing slash). A first deploy without that variable must fail the build.
4. **Render:** set `SNOOZESHARE_CORS_ALLOWED_ORIGIN` to Vercel's stable production domain (scheme and host, no trailing slash) and redeploy.
5. **Verify:** `curl.exe https://<render-url>/actuator/health` returns `{"status":"UP"}`, and the Vercel page shows `Backend: UP`.

| Trap | What happens and what to do |
|---|---|
| Neon connection string | Neon shows a libpq string (`postgresql://USER:PASSWORD@HOST/DB?sslmode=require&channel_binding=require`). The backend needs `jdbc:postgresql://HOST/DB?sslmode=require` with the user and password in separate variables and no `channel_binding`. Use the direct host, not the `-pooler` one, because Flyway's migration lock can misbehave through the pooler. A URL that does not start with `jdbc` fails startup with `'url' must start with "jdbc"` |
| New variables on an existing Render service | `render.yaml` sets its variables only when Render applies the Blueprint, which it does not do for an existing service, so add new ones by hand. A missing variable always shows as an `Error processing condition on ...` wrapper (for example on `FirebaseConfig`); the variable is named in the last `Caused by:` line, such as `Could not resolve placeholder 'FIREBASE_ENABLED'` |
| Vercel Framework Preset | It must be **Next.js**. Left on "Other", the build fails with `No Output Directory named "public" found` (`frontend/vercel.json` also pins it) |
| Vercel Production Branch | It must be `development` until the production deployment work. While it was `main`, `development` builds were Previews, which the backend's single allowed CORS origin cannot serve. **Redeploy** rebuilds the same commit as the deployment it is pressed on, so after changing the setting push a commit that touches `frontend/` |
| Memory on the free plan | The service has limited memory, so `render.yaml` caps the JVM heap with `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70` |

| Decision | Why | Rejected | Ref |
|---|---|---|---|
| Desktop code moves under `desktop/`; `backend/` and `frontend/` sit beside it; independent builds, no root build | One repo for the re-platform, with the desktop tests still guarding the business rules | Leave in place; delete now; a root multi-project build | C81 |
| `application-dev.yml` is committed with non-secret local defaults; secrets go in a gitignored `application-local.yml` | A fresh clone runs without setup | | C82 |
| The `checkstyle` and `build` checks keep their names and each covers all three components; backend tests use Testcontainers Postgres 17 | Branch protection keeps working; tests run on the same database engine as production | | C83 |
| Backend hosting is Render (free Docker web service); Vercel and Neon unchanged | Railway costs money | Railway; Cloudflare (no free JVM hosting); Cloud Run (needs a card) | C85 |
| Roles live in PostgreSQL keyed by Firebase UID, not in Firebase custom claims | The backend controls authorization and there is no claim synchronization to go wrong | Firebase custom claims | C86 |
| Host and Agent registration needs a server-side registration code; Guest needs none | A user cannot give themselves a privileged role | | C87 |
| The API derives the UID from the verified bearer token, and route protection is client-side guards, not Next.js middleware | The client is never trusted for identity, and there is no session-cookie system to specify | A client-sent `firebaseUid`; Next.js middleware auth | C92 |
| The `prod` profile fails startup unless security, Firebase, both codes and the Admin credentials are configured | An unset toggle would otherwise serve a permit-all production API | | C95 |
| The client address for rate limits is `CF-Connecting-IP` when forwarded headers are trusted, and `X-Forwarded-For` is never read | Render's proxy appends to a client-supplied `X-Forwarded-For`, so its first entry is forgeable | The first `X-Forwarded-For` entry | C97 |
| Spring Data JPA for persistence; entities use `id` as the PK column on every table | Cleaner Spring integration than ported JDBC; column naming follows W-AUTH's convention | Ported JDBC DAOs (C5 desktop) | C99 |
| One generic LLM gateway; each role registers its own `AiFeature` (prompt, output type, caps) | A role feature should not need a gateway change, and Host's feature is undecided | A gateway that owns every feature's prompt | C103 |
| The audit row stores metadata only, never prompt or reply text | Avoids copying personal data into `audit_log` | Truncated sanitized text | C104 |
| A custom scheduler and circuit breaker, in package `com.snoozeshare.ai` | One shared key needs a global queue with priorities, which a per-user limiter does not give; no new dependency | Bucket4j; `service/ai` | C105 |
| Daily caps count `AI_CALL` audit rows plus in-flight reservations, the audit is fail-closed, and no gateway call runs inside a transaction | Caps survive restarts and bursts; unaudited output must not reach a user; a 45 s call must not hold a connection | In-memory counters | C106 |
| Replies with links, HTML or Markdown images are rejected by default; the base URL must be `https` and redirects are never followed; a budget 429 blocks calls for 15 minutes, then re-probes | Exfiltration through rendered links, the key leaving for another host, and a forged "quota" phrase causing a day-long outage | Blocking until 00:00 UTC | C107 |
| Priority 0 (staff-facing features) may use the last 5% of the global daily cap | Guest registration needs no code, so many accounts could otherwise starve Host and Agent features; the key is not shared with any other system, so a large reserve is not needed | A 20% reserve; no reserve | C109 |
| `max-rpm` defaults to 25 | The development key allows 30 requests a minute | 55 | C110 |
| `reasoning-effort` defaults to `none` | `qwen3.5:9b` and `gemma4:26b` spend `max_tokens` on reasoning and return empty content on small limits; all three models accept `none` | Other, costlier models; reasoning on with large `maxTokens` | C111 |

---

## 4. Components

Dependencies come first. Money is `BigDecimal` at scale 2 with `HALF_UP` throughout.

### 4.1 Settlement domain

**Purpose:** pure rules for dividing a booking's held escrow and deciding whether it is still held.

**API**

- [`SettlementCalculator.split(escrow, guestRefund)`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/settlement/SettlementCalculator.java) returns a `SettlementBreakdown`.
- [`SettlementBreakdown`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/settlement/SettlementBreakdown.java) is a record of `escrow`, `guestRefund`, `hostGross`, `fee`, `hostNet`.
- [`EscrowPolicy.isHeld(bookingTransactions)`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/settlement/EscrowPolicy.java) is true when the list has an `ESCROW_HOLD` row and no `ESCROW_REFUND`, `BOOKING_PAYOUT`, `TICKET_REMEDY` or `AGENT_OVERRIDE` row.

**Depends on:** `domain.enums`, `domain.model.WalletTransaction`, `DomainValidation`. No JavaFX or JDBC.

**Invariants**

- The full escrow is settled: `guestRefund + hostGross = escrow`.
- The 3% fee is `round(hostGross × 0.03, 2, HALF_UP)` and applies to the host share only; the guest refund is fee-free.
- `0 ≤ guestRefund ≤ escrow`, with at most two decimal places; otherwise `IllegalArgumentException`.
- A zero amount produces no ledger row (the caller omits it).

**Deviations:** none.

### 4.2 State machines

**Purpose:** the single legality check for booking and ticket transitions.

**API:** [`BookingStateMachine.canTransition(from, to, role)`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/statemachine/BookingStateMachine.java) and [`TicketStateMachine.canTransition(from, to, role)`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/statemachine/TicketStateMachine.java).

**Depends on:** `domain.enums` only.

**Invariants**

| Machine | Transition | Allowed for |
|---|---|---|
| Booking | `CONFIRMED → COMPLETED` | `HOST` and, for dispute resolution, `AGENT` (C23) |
| Ticket | `OPEN → IN_REVIEW` | `AGENT` |
| Ticket | `IN_REVIEW → OPEN` (Unassign, C25) | `AGENT` |
| Ticket | `IN_REVIEW → RESOLVED_APPROVED` or `RESOLVED_REJECTED` | `AGENT` |
| Ticket | any other, including out of a resolved status | nobody |

Terminal booking statuses never transition. The `FORCE_*` booking statuses remain in the enum and the
booking machine but nothing in the agent UI uses them (C22).

**Deviations:** D9 (the `AGENT` permission on `CONFIRMED → COMPLETED` is an additive change to a shared file).

### 4.3 Persistence: tickets and ticket categories

**Purpose:** store dispute tickets and the category lookup that guests choose from.

**API**

- [`TicketRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/TicketRepository.java) and its adapter [`JdbcTicketRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/jdbc/JdbcTicketRepository.java): `findById`, `findByStatus`, `findQueue(status, assignee, agentId)`, `existsByCategory(label)`, `save` (upsert).
- [`TicketCategoryRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/TicketCategoryRepository.java) and [`JdbcTicketCategoryRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/jdbc/JdbcTicketCategoryRepository.java): `findActive`, `findAll`, `findById`, `labelInUse(label, excludeCategoryId)`, `save`, `deleteById`.
- [`MigrationRunner.migrate`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/infra/db/migration/MigrationRunner.java) applies the ordered V001–V008 migrations, or adopts a database that already has the foundation tables (such as the mock DB) by recording the baseline before applying later migrations.

```mermaid
erDiagram
  tickets {
    TEXT ticketId PK
    TEXT bookingId FK
    TEXT raisedByUserId FK
    TEXT raisedByRole
    TEXT category
    TEXT title
    TEXT description
    TEXT requestedRemedy
    TEXT supportingText
    TEXT status
    TEXT assignedAgentId FK
    TEXT agentNotes
    TEXT resolutionReason
    TEXT createdAt
    TEXT resolvedAt
  }
  ticket_categories {
    TEXT categoryId PK
    TEXT label
    INTEGER active
  }
  bookings {
    TEXT bookingId PK
  }
  users {
    TEXT userId PK
  }
  bookings ||--o{ tickets : "bookingId"
  users ||--o{ tickets : "raisedByUserId, assignedAgentId"
  ticket_categories ||..o{ tickets : "label text, no FK"
```

1. `tickets` references the booking it disputes and the users involved (the raiser, and the assigned agent when there is one).
2. `tickets.category` holds the category **label text**, not a foreign key to `ticket_categories`.
3. `ticket_categories.active` is `1` or `0`; deactivating hides a category from new filings only.
4. `bookings` and `users` show only their keys; their other columns are not part of this component.

`raisedByRole` is limited to `GUEST` or `HOST`; `requestedRemedy` to `FULL_REFUND`, `PARTIAL_REFUND`,
`HOST_PAYOUT`, `OTHER`; `status` to `OPEN`, `IN_REVIEW`, `RESOLVED_APPROVED`, `RESOLVED_REJECTED`
(all `CHECK` constraints in [`desktop/db/schema.sql`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/db/schema.sql)).

**Depends on:** `infra.db.ConnectionFactory`, `JdbcCodecs` and `RowMappers` (`repository.jdbc.support`), `java.sql` (only here).

**Invariants**

- Only `repository.jdbc.*` imports `java.sql`.
- `findQueue` orders by `createdAt, ticketId`; a null status means every status; `MINE` requires an agent id.
- The ticket upsert only updates `status`, `assignedAgentId`, `agentNotes`, `resolutionReason`, `resolvedAt`; the filing fields are immutable after insert.
- `labelInUse` and `existsByCategory` compare case-insensitively.
- Timestamps are read with `JdbcCodecs.instant`, which needs UTC ISO-8601 text ending in `Z`.

**Deviations:** D8 (label text, so a rename does not rewrite existing tickets); D10 (`MigrationRunner` adoption); D12 (a) (ordering by `createdAt` text means sub-second ties can mis-order) and (b) (adoption only checks for a `users` table). The repository method names above are what the code has; the design spec used slightly different names, and the code wins.

### 4.4 Ticket messaging (`MessageService`)

**Purpose:** persistent chat on a dispute ticket, one thread per side.

**API**

- [`MessageService`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/MessageService.java): `thread(ticketId, channel, viewerId, viewerRole)`, `post(ticketId, channel, authorId, authorRole, body)`, `conversationsFor(userId, role)`, `unreadCount(userId, role)`, `markRead(ticketId, channel, userId, role)`.
- [`MessageServiceImpl`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/impl/MessageServiceImpl.java) and [`JdbcMessageRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/jdbc/JdbcMessageRepository.java), over the `messages` and `message_reads` tables.
- [`Message`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/model/Message.java), [`ThreadChannel`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/enums/ThreadChannel.java) (`GUEST`, `HOST`), [`ConversationSummary`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/ConversationSummary.java), [`MessagePostedEvent`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/infra/events/events/MessagePostedEvent.java), published after the insert commits.

**Depends on:** `TicketRepository`, `BookingRepository`, `PropertyRepository`, `UserRepository`, `EventBus`, a `Clock`, `DomainValidation`.

**Invariants**

- A non-blank body is required; it is trimmed. Messages are ordered by `rowid` (insertion order), not the `sentAt` text, since `Instant.toString()`'s variable-length fraction can misorder rows within a second.
- A guest may read and post only the `GUEST` thread of a ticket on their own booking; a host only the `HOST` thread of a ticket on their own property; an agent both. A caller who is not a party, or names the wrong role for themselves, gets `IllegalArgumentException`.
- Once a ticket is resolved its threads become read-only: `post` throws `IllegalStateException`; `thread` still returns the history.

**Deviations:** C21 (chat ownership), C37 (replaces the session-only `InMemoryMessageService`), D22 (`thread` gained the viewer arguments; the old two-argument form and `InMemoryMessageService` are gone).

### 4.4a Booking messaging (`BookingConversationService`)

**Purpose:** a private chat between a booking's guest and host, separate from ticket chat and with no agent access.

**API**

- [`BookingConversationService`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/BookingConversationService.java): `thread(bookingId, viewerId, viewerRole)`, `post(bookingId, authorId, authorRole, body)`, `conversationsFor(userId, role)`, `unreadCount(userId, role)`, `markRead(bookingId, userId, role)`.
- [`BookingConversationServiceImpl`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/impl/BookingConversationServiceImpl.java) and [`JdbcBookingMessageRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/jdbc/JdbcBookingMessageRepository.java), over the `booking_messages` and `booking_message_reads` tables.
- [`BookingMessage`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/model/BookingMessage.java), [`BookingConversationSummary`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/BookingConversationSummary.java), [`BookingMessagePostedEvent`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/infra/events/events/BookingMessagePostedEvent.java).

**Depends on:** `BookingRepository`, `PropertyRepository`, `UserRepository`, `EventBus`, a `Clock`, `DomainValidation`.

**Invariants**

- Only the booking's guest and its property's host may read or post; an agent gets `IllegalArgumentException` (they may not read this chat at all, even as dispute evidence).
- Writable while the booking is `CONFIRMED` and today is on or before check-out + 7 days (the dispute window, same rule as escrow); otherwise `post` throws `IllegalStateException` and `thread` stays readable.
- Text only: no attachments, no edit or delete, no typing indicator.

**Deviations:** C38 (reverses the original W13 scope, which excluded chat outside tickets).

### 4.5 `DisputeSettlementService`

**Purpose:** the only class that moves money when an agent resolves a dispute.

**API:** [`DisputeSettlementService.settle(ticketId, mode, guestRefund, agentId, reason)`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/DisputeSettlementService.java), implemented by [`DisputeSettlementServiceImpl`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/impl/DisputeSettlementServiceImpl.java). It returns a [`Settlement`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/Settlement.java) (ticket, booking, `SettlementBreakdown`, and the guest and host `WalletTransaction`, either of which is `null` when its amount was zero). [`ResolutionMode`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/enums/ResolutionMode.java) is `ACCEPT`, `REJECT` or `MANUAL`.

**Outcome by mode**

| Mode | Guest row | Host row | Ticket ends as |
|---|---|---|---|
| `ACCEPT` | `TICKET_REMEDY` when the refund is above zero | `BOOKING_PAYOUT` when the host share is above zero | `RESOLVED_APPROVED` |
| `REJECT` (refund must be 0) | none | `BOOKING_PAYOUT` for the whole escrow, net of fee | `RESOLVED_REJECTED` |
| `MANUAL` | `AGENT_OVERRIDE` when the refund is above zero | `BOOKING_PAYOUT` when the host share is above zero | `RESOLVED_APPROVED` if the refund is above zero, else `RESOLVED_REJECTED` |

The host row's `amount` is already `hostGross − fee`; when `fee` is positive, `LedgerWriter.postPayout`
also writes a second `PLATFORM_FEE` row crediting the System wallet, in the same transaction
([4.16](#416-unified-ledger-ledgerwriter)). Every row carries `bookingId`, `ticketId` and
`actorUserId = agentId`. `ACCEPT` with a zero refund is allowed only when the ticket's requested
remedy is `HOST_PAYOUT`.

```mermaid
sequenceDiagram
  participant TS as TicketServiceImpl
  participant DS as DisputeSettlementServiceImpl
  participant TM as TransactionManager
  participant R as Jdbc repositories
  participant AU as AuditServiceImpl
  participant EB as InProcessEventBus
  TS->>DS: settle(ticketId, mode, refund, agentId, reason)
  DS->>TM: inTransaction(apply)
  TM->>R: load ticket, booking, wallets, ledger rows
  R-->>TM: current state
  Note over DS: EscrowPolicy.isHeld, SettlementCalculator.split, state machine checks
  TM->>R: save wallets, ledger rows, ticket, booking
  TM->>AU: record audit rows (4.14)
  TM-->>DS: commit (or rollback and rethrow)
  DS->>EB: TicketResolvedEvent, WalletTransactionRecordedEvent
  DS-->>TS: Settlement
```

1. `TicketServiceImpl.resolve` derives the refund and calls `settle`.
2. `settle` validates the reason and the `AGENT` role, then hands the work to `TransactionManager`.
3. Inside the transaction the service loads the current ticket, booking, both wallets and the booking's ledger rows.
4. It applies the preconditions and the split (shown as a note; the domain classes are pure).
5. It writes wallets, ledger rows, ticket and booking, then the audit rows (ticket, booking and the wallet sides; see [4.14](#414-audit-trail)), all on the same connection.
6. Commit makes everything visible at once; any exception rolls back all of it.
7. Only after commit are events published, then the `Settlement` is returned.

**Depends on:** `TicketRepository`, `BookingRepository`, `PropertyRepository`, `UserRepository`, `WalletRepository`, `LedgerWriter`, `LedgerRepository`, `AuditService`, `EventBus`, `TransactionManager`, the settlement domain (4.1) and state machines (4.2).

**Invariants**

- Preconditions are checked inside the transaction: caller is an agent; ticket is `IN_REVIEW` and assigned to the caller; booking is `CONFIRMED`; escrow is held (`LedgerRepository.entriesForBooking` plus `EscrowPolicy`); `0 ≤ refund ≤ escrow`; reason non-blank. Any failure writes nothing.
- One transaction covers both wallet balances, their money (and platform-fee) rows, the ticket, the booking (`COMPLETED`, `completedAt`) and its status audit rows. Events are published only after commit.
- Every wallet write goes through `LedgerWriter.post` / `postPayout` ([4.16](#416-unified-ledger-ledgerwriter)); the service never touches `WalletRepository.save` itself.
- A wallet's `balance` changes only in the same transaction as the money row that explains it, and that row carries the wallet's new `balanceAfter`.

**Deviations:** D5 (a dedicated service instead of `TransactionService.applyTicketRemedy` / `manualOverride`); D12 (c) (guest and host wallets are read once, so a self-booking where guest and host are the same user would lose an update), (d) (resolved by W12: `AuditServiceImpl` now stamps the record's own time, or the injected clock), (e) (the escrow amount comes from `booking.totalAmount()`, not the `ESCROW_HOLD` row).

### 4.6 `TicketService`

**Purpose:** the ticket lifecycle for agents (queue, assign, unassign, notes, resolve) and category administration.

**API:** [`TicketService`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/TicketService.java), implemented by [`TicketServiceImpl`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/impl/TicketServiceImpl.java).

| Method | Behaviour |
|---|---|
| `queueForAgent(status, AssigneeFilter, agentId)` | Tickets oldest first; `AssigneeFilter` is `ALL`, `UNASSIGNED` or `MINE`; a null status means all. |
| `assignToMe(ticketId, agentId)` | `OPEN → IN_REVIEW`, sets the assignee. Fails if not `OPEN` or already assigned to another agent. |
| `unassign(ticketId, agentId)` | `IN_REVIEW → OPEN`, clears the assignee; only the assigned agent. |
| `saveNotes(ticketId, notes, agentId)` | Replaces the single internal-notes text; only while `IN_REVIEW` and assigned to the caller. |
| `resolve(ticketId, ResolutionRequest, agentId)` | Derives the refund and delegates to `DisputeSettlementService.settle`. |
| `listCategories()` / `listAllCategories()` | Active categories only (what guests see) / all categories. |
| `createCategory`, `renameCategory`, `setCategoryActive` | Label must be non-blank and unique, case-insensitively. |
| `deleteCategory(categoryId, agentId)` | Permanent delete; refused while any ticket uses the label (deactivate instead). |

`ResolutionRequest` is a record of `mode`, `guestRefund` and `reason`; the refund is required for
`MANUAL` and for `ACCEPT` of a `PARTIAL_REFUND` or `OTHER` request, and is ignored or derived otherwise.
`fileTicket` validates the guest, booking eligibility and active category before creating a ticket;
`addHostResponse` is deprecated (see 4.4).

**Depends on:** `TicketRepository`, `TicketCategoryRepository`, `BookingRepository`, `UserRepository`, `DisputeSettlementService`, `AuditService`, `AuthorizationService`, `TicketStateMachine`.

**Invariants**

- Agent-only methods enforce the `AGENT` role in the service layer, not only in the UI. `fileTicket`
  and `myTickets` enforce Guest ownership and eligibility instead.
- Every mutation makes exactly one `AuditService.record` call: `TICKET_ASSIGNED`, `TICKET_UNASSIGNED`, `TICKET_NOTE_SAVED`, `TICKET_CATEGORY_CREATED`, `TICKET_CATEGORY_RENAMED`, `TICKET_CATEGORY_TOGGLED`, `TICKET_CATEGORY_DELETED` (renamed from `CATEGORY_DELETED`, D17); resolution is audited by the settlement service as `TICKET_RESOLVED`.
- Ticket status changes go through `TicketStateMachine`.

**Deviations:** the design spec described notes as an appended, attributed list (`addAgentNote`); the built behaviour is one replaceable text (`saveNotes`, C25). `unassign` (C25) and `deleteCategory` (C26) were added after the spec. `tickets.category` is label text (D8).

### 4.7 `DisputeQueryService`

**Purpose:** read models for the agent screens, so controllers never touch repositories.

**API:** [`DisputeQueryService`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/DisputeQueryService.java) with `queue(status, assignee, agentId)` returning [`DisputeSummary`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/DisputeSummary.java) rows and `detail(ticketId)` returning a [`DisputeDetail`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/DisputeDetail.java), implemented by [`DisputeQueryServiceImpl`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/impl/DisputeQueryServiceImpl.java).

- `DisputeSummary`: ticket id and label, title, category, listing title, guest, host and assignee names, status, `createdAt`.
- `DisputeDetail`: the ticket, listing and dates, party names, booking status, `escrowAmount`, `escrowHeld` and `phaseLabel`.

**Depends on:** `TicketRepository`, `BookingRepository`, `PropertyRepository`, `UserRepository`, `LedgerRepository`, `EscrowPolicy`, a `Clock`.

**Invariants**

- The ticket label is `#` plus the last four characters of the ticket id (for example `#0002`).
- `phaseLabel` is derived, never stored: "Stay ended — escrow held" (booking `CONFIRMED`, end date before today, escrow held), "Upcoming", "Active", or the prettified booking status otherwise (C23).
- Read-only: it never writes.

**Deviations:** none.


### 4.8 Guest portal (ui.guest)

**Purpose:** the Guest shell supports discovery, booking, trip management, messaging, dispute filing,
reviews and wallet operations. It is role-isolated from Host and Agent screens while reusing the
shared service, event and wallet boundaries.

#### 4.8.1 Guest shell and shared UI

**Guest shell and navigation.** GuestShellController provides four center-replacement tabs:
Explore, Trips, Messages and Wallet. It cleans up subscriptions as views change and passes the shared
AppContext to each child controller. The Guest shell and Guest Messages view currently load the
shared `agent-theme.css`; the wallet page comes from the common wallet module. Controllers do not
access repositories directly.

**API**

| File | Role |
|---|---|
| GuestShellController + guest-shell.fxml | Guest navigation shell with Search, Trips, Messages and Wallet tabs (the Search page is titled Explore). |
| GuestSearchController + guest-search.fxml | Searches listings by city, guest count, check-in/check-out dates and an optional maximum nightly budget. Results show nightly price, capacity and availability, and unavailable matches are visibly marked. |
| ListingDetailController + listing-detail.fxml | Shows the listing's property type, address, description, capacity, beds, baths, check-in/out times, amenities, host name and dynamic nightly/total price; submits a booking request when dates are selected. |
| TripDashboardController + trip-dashboard.fxml | Loads every booking for the guest and groups them into three collapsible sections, Upcoming, Active and Past (Upcoming holds pending requests and confirmed stays not yet started, Active holds stays under way, and Past holds ended and completed stays plus cancelled and rejected requests); reacts to booking/ticket events; supports eligible cancellation, dispute filing and review actions. |
| TicketFilingController + ticket-filing.fxml | Files a ticket for the selected booking with an active category (the category "Other" is always listed last), a requested remedy, a title, a description and optional supporting text. The dialog sizes to its content, with Category and Requested Remedy side by side. |
| TicketHistoryController + ticket-history.fxml | Displays the Guest's own tickets, status, category, filing time, description, requested remedy, supporting information and resolution. |
| GuestMessagesController + guest-messages.fxml | Merges ticket and private booking conversations, loads persisted threads, marks them read, posts replies while writable and opens ticket filing for a booking conversation. |
| ReviewDialogController + review-dialog.fxml | Submits one 1–5 star rating and optional comment for a completed stay. |
| WalletDashboardController + wallet-dashboard.fxml | Shared wallet screen used by Guest and Host: balance, held escrow, statement, top-up and withdrawal actions. |

#### 4.8.2 Guest feature behavior

**Guest service surface**

| Service | Guest-facing responsibility |
|---|---|
| `ListingService` | `search` applies city, capacity and date availability criteria; `getDetail` and `estimateCost` supply the detail modal. |
| `BookingService` | `submitRequest` creates a pending booking with escrow and availability; `tripsFor` supplies the Trips page, which passes no filter and groups the bookings itself; `cancel` applies the cancellation policy. |
| `TicketService` | `fileTicket` validates the booking party, eligibility and active category; `myTickets` supplies ticket history. |
| `ReviewService` | `submit` validates completed-stay ownership, rating range and one-review-per-booking; `hasReview` controls the Review action. |
| `MessageService` / `BookingConversationService` | Provide the Guest's ticket-thread and private Host conversation inboxes, thread reads, posts and read-state updates. |
| `WalletService` / common wallet controllers | Provide balance, statement, top-up, withdrawal and held-escrow display through the shared wallet path. |

**Explore screen.** Search results are availability-aware and identify unavailable date matches;
selecting a card opens the listing detail modal. The detail view shows the complete property and host
summary, calculates the selected stay total, and disables booking until both dates are selected.

**Trips screen.** Bookings are grouped into three collapsible sections, Upcoming (including pending
requests), Active and Past (including cancelled and rejected requests). Eligible cards expose Cancel Booking, File Dispute and Leave Review actions; booking, cancellation and
ticket events refresh the current tab without a manual navigation cycle.

**Messages screen.** One inbox merges ticket and booking conversations, preserves selection on refresh,
marks the selected thread read, and disables the composer after ticket resolution or the booking-chat
cutoff. Ticket filing can be opened from an eligible booking conversation.

**Wallet screen.** The common wallet presentation is role-neutral; Guest and Host use the same
transaction/ledger invariants with different session owners. It displays available balance, held escrow,
statement rows, top-up and withdrawal actions.

**Guest booking behavior.** Search results are availability-aware, but booking submission revalidates
the listing, dates, guest ownership, account status and wallet balance in the service layer. A
successful request creates a PENDING booking, places the total in an atomic ESCROW_HOLD, and
creates the booking availability block. Guests may cancel pending or pre-check-in confirmed bookings;
pending bookings receive a full refund, while confirmed bookings receive 100% when more than 48 hours
remain and 50% otherwise; the cancel dialog states both cases (a pending booking is refunded in full at
any time). Trip and message views refresh from booking, ticket and message events.

**Dates.** No date picker accepts a past date. In the UI, `GuestVisuals.blockPastDates` greys out
and disables every day before today and, because greyed cells do not stop typing, also reports a past date
typed into the field with a message while leaving it there; the service then refuses the search, booking
or block with the same reason, so the action never runs with a silently dropped date. The services
repeat the rule through `StayDatePolicy` (booking stay, search range, Host block). The Audit Log date
filters are deliberately excluded because they must reach history.

```mermaid
sequenceDiagram
  actor G as Guest
  participant UI as Guest listing detail
  participant BS as BookingService
  participant TM as TransactionManager
  participant AR as AvailabilityRepository
  participant LW as LedgerWriter
  participant BR as BookingRepository
  participant BUS as EventBus

  G->>UI: choose dates and submit booking
  UI->>BS: submitRequest(guest, listing, start, end)
  BS->>BS: validate active Guest, listing, dates and wallet
  BS->>TM: inTransaction(create request)
  TM->>BR: save PENDING booking
  TM->>AR: save BOOKING availability block
  TM->>LW: post ESCROW_HOLD against Guest wallet
  TM-->>BS: commit booking and hold
  BS->>BUS: publish booking-created event
  BS-->>UI: show submitted request
```

Cancellation follows the same transaction boundary: the service verifies that the acting user is the
booking Guest and that the booking is cancellable, removes its booking block, then posts the policy
refund before publishing the cancellation event.

**Guest post-stay behavior.** A Guest may file one ticket for an eligible confirmed/completed stay
from check-in through seven days after checkout, unless a ticket already exists. The ticket uses an
Agent-managed active category and accepts a requested remedy plus supporting text. A completed stay
can receive one 1–5 rating and comment; the review is audited.

**Guest messages.** Ticket messages are persisted per-party Guest-to-Agent threads. Booking messages
are private Guest-to-Host conversations, remain writable through checkout plus seven days, become
read-only afterward, and are never visible to Agents. The Guest Messages inbox includes both kinds of
conversation; its ticket-filing action is available from an open booking conversation.

**Depends on:** AppContext, ListingService, BookingService, TicketService, ReviewService,
MessageService, BookingConversationService, WalletService, SessionContext, EventBus,
NavShellController, shared wallet components and the domain state machines.

**Invariants**

- The Guest can act only on their own bookings, tickets, wallet and message threads; service-layer role and ownership checks repeat the UI restrictions.
- Booking creation, escrow hold, availability reservation and cancellation refund are transactional; domain events publish after commit.
- Reviews are allowed only for the booking guest after COMPLETED, with one review per booking and a rating from 1 through 5.
- A Guest cannot read or post in another party's ticket or booking conversation, and cannot post to a read-only conversation.

**Tests:** service coverage includes SearchCriteriaTest, BookingServiceTest, ReviewServiceTest, wallet
tests and repository tests; shared UI smoke/layout/dependency tests cover the role shells and wallet
wiring. Guest-specific controller snapshot coverage is not currently present.

**Deviations:** the Guest shell and Guest Messages view currently reuse `agent-theme.css`; and Guest-specific controller
snapshot coverage has not been added. These are presentation/coverage limitations, not service-layer
ownership or booking-safety gaps.

**Traps from the W16 alignment (D38):**

- The shared wallet page means a change to the Guest wallet card also changes the Host Wallet. Theme
  overrides such as `.agent-root .wallet-balance-section` outrank `wallet.css`, so `wallet.css` owns the card
  and its label rules are written as `.wallet-page .label.wallet-balance-*`.
- Pending requests have no group of their own: they sit under Upcoming. A `CONFIRMED` stay whose end date
  has passed, inside the 7-day window, appears under Past so its File dispute button stays reachable.
- Every table's empty message is one row inside the table (`EmptyTableRow.install`); do not add a label
  beneath a table.
- The ticket status pill is a fixed 116x36 in CSS to match the `+ New Ticket` button; change both together.
  Messages keep the 32px content inset pinned by `HostPageLayoutTest`.
- Listing detail omits house rules and host response time, and the review modal has no pre-selected star,
  because there is no data for the first and the rating is required for the second.

### 4.9 Host portal (`ui.host`)

**Purpose:** the Host shell supports listing management, availability overrides, booking requests,
messaging and wallet operations. It is role-isolated from Guest and Agent screens while reusing the
shared service, event and wallet boundaries.

#### 4.9.1 Host shell and shared UI

**Host shell and navigation.** `HostShellController` owns four center-replacement tabs: Listings,
Requests, Messages and Wallet. It disposes page subscriptions when leaving live Messages or Wallet
views. The Host shell and Host pages load `host-theme.css`, which owns the Host palette and shared
page, modal and table styles. Listing detail, create/edit and calendar pages use Host-owned
breadcrumbs; create/edit retain their form titles while detail and calendar include listing context.

#### 4.9.2 Host listing management (`ui.host.listings`)

**Purpose:** hosts create, edit, publish, deactivate and inspect their own properties. A new listing is saved with the status the form sends (active or inactive), not always active.

**API**

| File | Role |
|---|---|
| `HostShellController` + `host-shell.fxml` | Host navigation shell with Listings, Requests, Messages and Wallet tabs. |
| `HostMessagesController` + `host-messages.fxml` | Merged Host inbox for ticket and private booking conversations; delegates to the appropriate persistent messaging service and disables replies after a thread becomes read-only. |
| [`HostListingsController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/host/listings/HostListingsController.java) + `host-listings.fxml` | Host-scoped listing dashboard with listing cards, booking/rating metrics, Active/Inactive control, Edit and calendar entry. |
| [`HostListingFormController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/host/listings/HostListingFormController.java) + `host-listing-form.fxml` | Shared create/edit form for details, location, capacity, pricing and amenities; validates checkout after check-in. |
| [`HostListingDetailController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/host/listings/HostListingDetailController.java) + `host-listing-detail.fxml` | Host-facing detail page with Back, Edit and booking-calendar actions. |
| [`ListingServiceImpl`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/impl/ListingServiceImpl.java) | Enforces host ownership, validates listing data, persists changes and emits audit records. |
| [`ListingMetricsService`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/ListingMetricsService.java) | Supplies booking, occupancy and earnings metrics with zero-value fallbacks. |

**Host service surface**

| Service | Host-facing responsibility |
|---|---|
| `ListingService` | `findByHostId`, `create`, `update` and `updateStatus` enforce Host ownership and listing validation. |
| `AvailabilityService` | Loads listing-scoped calendar blocks and creates/removes Host-owned manual blocks while protecting booking blocks. |
| `ListingMetricsService` | Produces booking count, occupancy, earnings and review projections for listing cards and detail. |
| `BookingService` | Supplies host-scoped pending/history projections, validates decisions, records the optional Host message, and runs guarded completion. |
| `MessageService` / `BookingConversationService` | Provide Host ticket-thread and private Guest conversation inboxes with role/ownership checks and read-only cutoffs. |
| `WalletService` / `TransactionService` / `LedgerWriter` | Provide the shared Host wallet, escrow display, top-up/withdrawal path and completion payout ledger writes. |

**Listings screen.** Cards show live booking, occupancy, earnings and review metrics. Hosts can create,
edit, activate/deactivate, open detail, or enter a listing's calendar. Detail, create/edit and calendar
views retain Host-owned breadcrumbs and page styling.

**Calendar screen.** The monthly view separates booking blocks from manual blocks, requires a reason for
manual blocks, shows block history, and prevents removal of booking-derived dates. The UI renders
inclusive dates while the availability service persists half-open ranges. The UI enforces the non-blank
reason; the service trims it and permits a nullable reason for lower-level callers.

**Requests screen.** Pending and historical rows are Host-scoped. The decision modal supports approval
or rejection and an optional persisted Host message, while projected net earnings and Guest rating remain
visible. Completion sweeps leave disputed bookings held.

**Messages screen.** The merged inbox distinguishes ticket and booking conversations, refreshes on
message events, and disables replies when a thread is resolved or read-only. Ticket and booking chat
continue to use their separate service boundaries.

**Wallet screen.** The shared wallet view displays available balance, held escrow, statement rows, top-up
and withdrawal actions; ledger ownership remains in the common wallet services.

Listings are host-owned and mutations are enforced in the service layer. New listings are active by
default; status and property values remain enum/database values even when the UI renders readable labels.
Listing cards and detail pages use `ListingMetricsService` for zero-safe booking, occupancy, earnings
and review metrics. The form requires complete location fields, a positive capacity, valid check-in/
check-out times, and a positive nightly rate with at most two decimal places (C42).

**Depends on:** `AppContext`, `ListingService`, `ListingMetricsService`, `UserService`,
`BookingService`, `AvailabilityService`, `SessionContext`, `NavShellController`, `host-theme.css`
and the shared domain models.

**Invariants**

- A Host can create, edit or change status only for listings owned by that Host; service-layer ownership checks repeat the UI restriction.
- New listings are `ACTIVE`; changing status does not change property data or ownership.
- Listing validation runs before persistence and rejects malformed or degenerate values, including non-positive capacity/rate and rates with more than two decimal places.
- Listing metrics are read-only projections and display zero-safe values when there are no bookings, occupancy nights, earnings or reviews.

**Tests:** `HostListingsControllerTest`, `HostBreadcrumbNavigationTest`, `HostPageLayoutTest`,
`HostThemeOwnershipTest`, `ListingServiceTest` and `ListingMetricsServiceTest` cover listing
validation/ownership, metrics, navigation, layout and Host stylesheet ownership.

### 4.10 Host calendar and date overrides (`ui.host.calendar`)

**Purpose:** hosts inspect one listing's monthly availability and create or remove manual blocks.

[`HostCalendarController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/host/calendar/HostCalendarController.java)
uses `AvailabilityService` to load the selected month, navigate months, add inclusive single-date or
range blocks, and remove existing manual blocks. Booking blocks and manual blocks are shown with separate
visual treatments. Validation rejects an end date before the start date and overlapping unavailable dates.
The calendar is listing-scoped and does not bypass booking availability checks. It shows a booking
range for every pending, confirmed or completed booking, including bookings with no stored block row
(`AvailabilityService.blocksFor` adds a synthetic `BOOKING` block). A block cannot start in the past:
the From and To pickers disable past days, and `createHostBlock` rejects a past start through
`StayDatePolicy.requireNotInPast`, so a typed past date is refused with "From date cannot be in the
past". Today is allowed.

**Behavior and invariants:** the UI accepts inclusive dates while persistence stores the half-open end
date. The UI requires a non-blank reason for every manual block; `AvailabilityService` trims a supplied
reason and stores `null` when a lower-level caller supplies only whitespace. The history list shows dates
and reason; Hosts may remove only their own manual blocks, never booking-derived blocks. Overlap checks
and service ownership checks are enforced again by `AvailabilityService`.

```mermaid
sequenceDiagram
  actor H as Host
  participant UI as Host calendar
  participant AS as AvailabilityService
  participant AR as AvailabilityRepository

  H->>UI: select inclusive dates and enter reason
  UI->>AS: createHostBlock(listing, start, end, reason, host)
  AS->>AS: verify ownership, dates and non-blank reason
  AS->>AR: check unavailable and booking overlaps
  alt overlap or invalid request
    AS-->>UI: reject with validation error
  else valid manual block
    AS->>AR: persist manual block with half-open end
    AR-->>AS: saved block
    AS-->>UI: refresh month and block history
  end
```

**Tests:** `HostCalendarControllerTest`, `HostCalendarVisualSnapshotTest`, `AvailabilityServiceTest`
and `JdbcAvailabilityBlockRepositoryTest` cover date navigation, inclusive rendering, UI reason
validation, service-side reason trimming/normalization,
overlap rejection, block history and removal rules.

### 4.11 Host booking requests (`ui.host.bookings`)

**Purpose:** hosts review pending requests, decide them, inspect earnings and see request history.

[`HostBookingsController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/host/bookings/HostBookingsController.java)
uses `BookingService.pendingRequestRowsFor` and `historyRowsFor` for host-scoped projections. The request
table includes guest, dates, nights, amount, estimated net earnings and guest rating. Approve and reject
decisions use the shared host modal style; rejection may include the persisted `hostDecisionMessage`.
Rejected requests refund held escrow and approved requests retain it until completion or dispute settlement.
`BookingService.completeEligibleBookings()` guards automatic completion so bookings with open or in-review
tickets remain held.

```mermaid
sequenceDiagram
  actor H as Host
  participant UI as Host Requests
  participant BS as BookingService
  participant SM as BookingStateMachine
  participant TM as TransactionManager
  participant LW as LedgerWriter
  participant BUS as EventBus

  H->>UI: approve or reject request
  UI->>BS: decide(booking, decision, host, message)
  BS->>BS: verify listing ownership and pending status
  BS->>SM: validate host transition
  BS->>TM: inTransaction(save decision)
  alt reject
    TM->>LW: post ESCROW_REFUND to Guest wallet
  else approve
    TM->>LW: retain existing ESCROW_HOLD
  end
  TM-->>BS: commit decision and money rows
  BS->>BUS: publish booking decision event
  BS-->>UI: refresh request/history projections
```

Completion is a separate guarded path: the sweep checks checkout plus seven days and leaves a booking
untouched when its ticket is `OPEN` or `IN_REVIEW`; eligible bookings are settled through the unified
ledger and then appear in Host history.

`HostBookingDecisionDialogController` owns the decision modal and validates the optional host message;
`BookingService` owns the state transition, escrow refund/hold behavior and host ownership check. The
request table is a host-scoped projection, so a Host never receives another Host's booking rows.

The host-facing slices share the same service and repository boundaries as the agent slice: controllers
receive capabilities through `AppContext` and do not access repositories directly. `TransactionService`
settles eligible completion payouts, while `DisputeSettlementService` owns agent-directed two-sided
settlement of held escrow.

**Depends on:** `AppContext`, `BookingService`, `TransactionService`, `DisputeSettlementService`,
`ListingService`, `ReviewService`, `SessionContext`, `EventBus` and the shared wallet/ledger services.

**Invariants**

- Only the listing's Host can approve or reject its pending requests; decisions go through `BookingStateMachine`.
- Rejecting a held request refunds the Guest; approving it leaves the escrow held until completion or dispute settlement.
- Completion sweeps settle only eligible bookings and leave bookings with `OPEN` or `IN_REVIEW` tickets held.
- Host request and history rows are projections; controllers do not write repositories directly.

**Tests:** `HostBookingsControllerTest`, `BookingServiceTest` and `TransactionServiceTest` cover
host-scoped projections, decisions, refunds, completion guards and transaction behavior; shared state
machine coverage is exercised through the booking service tests.

### 4.12 Host wallet management (`ui.host.wallet`)

**Purpose:** provide hosts with the same native wallet capability and visual language as the guest wallet,
while keeping the presentation owned by the common wallet module.

The shared [`WalletDashboardController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/common/wallet/WalletDashboardController.java)
and `wallet-dashboard.fxml` are loaded by both Guest and Host shells. The page includes the available
balance, the amount currently held in escrow for pending bookings, top-up and withdrawal actions, and a
five-column transaction statement. The statement uses compact type badges, aligned date/related/amount/
balance columns, dollar-formatted values, and green/red directional styling for credits and deductions.

`WalletActionDialogController` and `WalletModal` provide native application modals for top-up and
withdrawal. Preset top-up amounts fill the input, the withdrawal link fills the full available balance,
and both dialogs use full-window scrims with content-sized modal cards, outlined Cancel actions, and
contained confirmation actions. Wallet-specific presentation lives in `wallet.css`; it does not depend on
agent styling.

**Escrow display.** The balance hint is computed from ledger entries: escrow holds are grouped by
booking and reduced by releasing transactions, so the displayed amount reflects currently held escrow
rather than a fixed placeholder. The `audit_log` money rows are the source of balance truth (the unified
ledger, [4.16](#416-unified-ledger-ledgerwriter)); `wallets.balance` is a cache of the owner's newest
`balanceAfter`.

**Tests:** `WalletDashboardControllerTest`, `WalletUiStructureTest`, `WalletLedgerTest` and the wallet
modal tests cover the statement layout, dynamic escrow copy, all wallet transaction types, preset/full-
balance actions, and FXML/CSS wiring.

**Depends on:** `WalletService`, `TransactionService`, `LedgerWriter`, `BookingService`, `SessionContext`
and the common `ui.common.wallet` components. Host wallet mutations use the same service-layer role
checks and unified ledger as Guest wallet mutations; no Host-specific wallet table or transaction path
exists.

**Invariants:** withdrawals cannot exceed available non-escrow balance; every balance-changing action
is represented by an `audit_log` money row with `balanceAfter`; the displayed escrow summary is derived
from held and releasing rows rather than a fixed UI value.

**Deviations:** Host calendar ranges are inclusive in the UI but half-open in persistence; Host formal
response notes/evidence on dispute tickets remain out of scope and are represented by messaging instead;
and booking decision/calendar-block audit coverage is incomplete as recorded in Known Limitations.

### 4.13 Agent UI (`ui.admin`)

**Purpose:** the Support Agent's shell with the Disputes, Accounts, Audit Log and Categories screens.

**API**

| File | Role |
|---|---|
| [`AdminShellController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/AdminShellController.java) + [`admin-shell.fxml`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/resources/com/snoozeshare/ui/admin/admin-shell.fxml) | Top bar and tab strip (Disputes, Accounts, Audit Log, Categories); swaps the centre view and disposes the live views (queue, accounts) when it does. |
| [`DisputeQueueController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/tickets/DisputeQueueController.java) + `dispute-queue.fxml` | Oldest-first table, All / Unassigned / Mine chips, status filter (All statuses, Open, In review, Approved, Rejected), "N unassigned" badge; refreshes on `TicketResolvedEvent`. |
| [`DisputeDetailController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/tickets/DisputeDetailController.java) + `dispute-detail.fxml` | Booking summary, guest and host chat panes, one notes field, Assign to me / Unassign, Accept, Reject dispute, Manual adjustment. The Accept label follows the ticket raiser ("remedy guest" or "remedy host"). |
| [`ResolutionDialogController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/tickets/ResolutionDialogController.java) + `resolution-dialog.fxml` | One dialog for the three modes; reason required; live preview. |
| [`ResolutionPreview`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/tickets/ResolutionPreview.java) | Pure logic that turns a refund text into a preview or an error message, using `SettlementCalculator`. |
| [`CategoryAdminController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/categories/CategoryAdminController.java), [`CategoryDialogController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/categories/CategoryDialogController.java) | Category table with active toggle, Add and Edit modals; Edit has Delete. |
| [`AuditLogController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/audit/AuditLogController.java) + [`audit-log.fxml`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/resources/com/snoozeshare/ui/admin/audit/audit-log.fxml) | The read-only Audit Log screen (below). |
| [`AccountGovernanceController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/accounts/AccountGovernanceController.java) + [`account-governance.fxml`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/resources/com/snoozeshare/ui/admin/accounts/account-governance.fxml) | The Accounts screen (below). Refreshes on `AccountStatusChangedEvent`. |
| [`SuspensionDialogController`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/accounts/SuspensionDialogController.java) + `suspension-dialog.fxml` | The Suspend and Reactivate modal (one card, two modes). |
| [`AccountText`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/accounts/AccountText.java), [`AccountSearch`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/accounts/AccountSearch.java) | Pure helpers: role and status labels, `DD MMM YYYY` dates, and the live-search match. |
| [`MultiSelectMenu`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/audit/MultiSelectMenu.java) | The action-type dropdown with a check per option; an empty selection means every action. |
| [`AgentModal`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/AgentModal.java) | Shared modal shell: transparent undecorated stage plus a light-grey scrim over the owner window. |
| [`HeightGrip`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/ui/admin/HeightGrip.java) | Drag handle that resizes chat panes together and the notes box, within min and max heights. |
| [`agent-theme.css`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/resources/com/snoozeshare/ui/admin/agent-theme.css) | The "Fall Light" palette, loaded on the agent scene root only. |

**Audit Log screen (W12).** A read-only table over `AuditService.search` ([4.14](#414-audit-trail)).

- **Filters:** one **Search** box (user name, or booking / ticket / user id; Enter applies it), an **Action type** multi-select (empty means all actions), separate **From** and **To** date pickers, an **Apply filters** button and a red-outline **Clear** button. Filters take effect only on Apply (selecting does not auto-apply); Apply refuses a From date after the To date. The date pickers and the multi-select popup are styled after the design board's Date picker component in `agent-theme.css` (C33).
- **Table:** TIMESTAMP (`dd/MM/yyyy HH:mm`), ACTOR (a user, can be the System user), ACTION TYPE (a coloured pill), TARGET (the row's one direct entity — a user's name, or `Property/Booking/Ticket/Category #last4`; a wallet movement targets the wallet owner, so a `PLATFORM_FEE` row shows `SnoozeShare System`; a ticket resolution names only the ticket, not its booking — replaced the last-four-character `REF` column, W14 follow-up), STATUS (`Before → After`, or one status, or a dash), REASON and AMOUNT (signed, for example `+SGD 175.00`), 200 rows at a time with a Load more button. Rows are not clickable: they get the queue's grey hover but keep the default cursor.

**Accounts screen (W11).** Every Guest, Host and Agent account except the System user, in a fixed-header table. The System user is hidden here (it cannot be governed) but does appear as an actor or target on the Audit Log screen above.

- **Columns:** DISPLAY NAME, EMAIL, ROLE, JOINED (`DD MMM YYYY`), STATUS (an ACTIVE or SUSPENDED pill, with `Reason: …` under a suspended one) and ACTION. Guests and Hosts get a red-outline **Suspend** or a **Reactivate** button of the same size; Agent rows show a dash and no action.
- **Search:** one live box (no Apply button) that matches the displayed display name, email, role, joined text and status. It does not search the reason.
- **Modals:** both use `AgentModal` with the 50% scrim. The banner shows the display name large, the email below it and `Role · joined DD MMM YYYY`; Suspend is red and Reactivate is green (#40680C). A reason is required, and Confirm stays disabled until one is typed.
- **Sizing:** the scroll area takes its height from the laid-out rows, so a filtered list that fits shows without a scroll bar instead of being squeezed.

- **Fixed headers on every agent table:** in the dispute queue, the audit log, the accounts list and the categories list only the rows scroll; the header stays put and the slim scroll bar starts below it. The queue and audit tables size to their rows but shrink to the window and scroll internally; the categories list scrolls inside a `ScrollPane` under its fixed header.

**Depends on:** `AppContext` (to reach `TicketService`, `DisputeQueryService`, `MessageService`, `AuditService`, `AccountGovernanceService`, `SessionContext`, `EventBus`), the domain enums and records, and `NavShellController` from the shared shell.

**Invariants**

- `ui.*` must not import `com.snoozeshare.repository` or `java.sql`; `LayerDependencyTest` and `UiDependencyTest` check this. Controllers reach data only through service interfaces. (The rule against importing `infra.db` is a convention; the tests do not check it.)
- The resolution dialog requires a non-blank reason and a refund within escrow when the selected mode requires one; the suspension dialog requires a non-blank reason. The corresponding services enforce these rules again.
- There is no Force Cancel / Force Complete control anywhere (C22).
- Subscriptions are released in `DisputeQueueController.dispose()` and `AccountGovernanceController.dispose()` when the shell navigates away.

**Deviations:** D18 (the JavaFX date-picker popup cannot be restyled to match the board exactly: it keeps two month/year spinners and mixed-case weekday names, has no unavailable-day state, and the categories header columns sit about 5px left of the rows while the scroll bar shows); the design board's Audit Log artboard is older than the built screen (it has User ID / Booking ID inputs and no REF/TARGET column) and C29 supersedes it. D7 (the design canvas shows Force actions, a newest-first queue, an "Adjust wallet" dropdown and no Accept amount field; the built screens follow C22, oldest-first per F9.1.1, and the refund-amount field of C20); D11 (a sidebar shell) was superseded by C24, so the built shell is the tab strip; D12 (f) (`DisputeDetailController.load()` still calls `render()` outside the error handler). The dispute screens use the `SGD` currency label (C10).

### 4.14 Audit trail

**Purpose:** record every change to bookings, tickets, listings, ticket categories and wallet balances as typed, searchable rows, and let a support agent search them.

**API**

- [`AuditService`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/AuditService.java), implemented by [`AuditServiceImpl`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/impl/AuditServiceImpl.java): `record(AuditRecord)` and `search(AuditFilter, limit, offset)`. `AuditService.SYSTEM_ACTOR_ID` is the seeded System user. (W14 removed the `recordWalletTransaction` helper: every money row now goes through `LedgerWriter`, [4.16](#416-unified-ledger-ledgerwriter).)
- [`AuditRecord`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/AuditRecord.java) is what a service asks to write; [`AuditLogEntry`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/model/AuditLogEntry.java) is what a search returns; [`AuditAction`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/enums/AuditAction.java) is the enum of action types (the column stores the constant's name).
- [`AuditFilter`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/AuditFilter.java) (`text`, a set of `actions`, inclusive `from` / `to` dates) is the screen's filter; [`AuditCriteria`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/AuditCriteria.java) is its repository form. [`AuditLogRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/AuditLogRepository.java) and [`JdbcAuditLogRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/jdbc/JdbcAuditLogRepository.java) store and query rows.
- The schema change is [`V002__audit_trail.sql`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/resources/db/migration/V002__audit_trail.sql), which is also reflected in [`desktop/db/schema.sql`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/db/schema.sql).

**Column layout.** One row is one change. Migration V002 added seven columns to the V001 `audit_log` table:

| Column | Holds |
|---|---|
| `logId`, `actorUserId`, `actionType`, `entityType`, `entityId`, `timestamp` | From V001: the row id, who acted, the `AuditAction` name, what kind of entity changed and its id, and the time (`Instant.toString()` text). |
| `beforeState`, `afterState` | **Status text only** (for example `IN_REVIEW`, `RESOLVED_APPROVED`); null means none. Never money and never JSON. |
| `walletAdjustment` | The **signed** amount that actually moved in one wallet (a payout net of fee, a negative escrow hold). Set on wallet rows only. |
| `reason` | Free text: the agent's resolution reason, or a note such as "Payout net of 3% platform fee (...)". |
| `ticketId`, `bookingId` | The ticket and booking the row concerns, so a search by either id finds every related row. |
| `subjectUserId` | The user the change happened to (the guest whose wallet moved, the ticket raiser), as opposed to the actor. |
| `actorName`, `subjectName` | Snapshots of the display names taken when the row was written. They are null on rows written before V002. |
| `balanceAfter` | The wallet's balance immediately after this row's `walletAdjustment`. Set on money rows only; added by V007 (W14). |

**`AuditRecord` and its builder.** `AuditRecord`'s constructor rejects a record that carries both a wallet adjustment and a status, so a
row is never both, and a `balanceAfter` is rejected unless `walletAdjustment` is also set. Services build
records with `AuditRecord.builder(actorId, action, entityType, entityId)` and chain
`.status(before, after)` (enums are stored by name), `.wallet(amount, balanceAfter)`, `.reason(text)`
(blank becomes null), `.subject(userId)`, `.booking(id)`, `.ticket(id)` and `.at(instant)`; `build()`
returns the record. Money rows reuse the wallet transaction type's name (`AuditAction.forWallet`), so
`TOP_UP`, `ESCROW_HOLD`, `BOOKING_PAYOUT`, `PLATFORM_FEE` and the like name the row's `actionType`. Since
W14 the `audit_log` row **is** the ledger row — there is no separate ledger table to also appear in
([4.16](#416-unified-ledger-ledgerwriter)).

**Writing rows.** `AuditServiceImpl.record` fills `actorName` and `subjectName` from the user table
("Unknown user" if the id has no user) and stamps the row with the record's `at`, or the injected `Clock` when none is set. The
audit repository is built on the same connection as the caller's transaction (`AppContext`), so the row commits or
rolls back with the change.

| Source | Rows written |
|---|---|
| `BookingServiceImpl` | Escrow holds and refunds through `LedgerWriter`; booking state changes and Host calendar blocks do not yet have complete audit coverage (see Known Limitations) |
| `DisputeSettlementServiceImpl` | `TICKET_RESOLVED`, `BOOKING_COMPLETED`, and the guest and host money rows (see the sequence diagram below) |
| `TicketServiceImpl` | `TICKET_OPENED`, `TICKET_ASSIGNED`, `TICKET_UNASSIGNED`, `TICKET_NOTE_SAVED`, `TICKET_CATEGORY_CREATED`, `TICKET_CATEGORY_RENAMED`, `TICKET_CATEGORY_TOGGLED`, `TICKET_CATEGORY_DELETED` |
| `ListingServiceImpl` | `LISTING_CREATED`, `LISTING_UPDATED`, `LISTING_STATUS_CHANGED` |
| `AccountGovernanceServiceImpl` | `ACCOUNT_SUSPENDED` or `ACCOUNT_REACTIVATED`; on a suspension also `LISTING_STATUS_CASCADE`, `BOOKING_FORCE_CANCELLED` and an `ESCROW_REFUND` money row per affected booking, all through `LedgerWriter` ([4.15](#415-account-governance)) |
| `LedgerWriter` (used by `WalletServiceImpl`, `TransactionServiceImpl`, `BookingServiceImpl`, `DisputeSettlementServiceImpl` and `AccountGovernanceServiceImpl`) | Every money row: `TOP_UP`, `WITHDRAWAL`, `ESCROW_HOLD`, `ESCROW_REFUND`, `BOOKING_PAYOUT`, `TICKET_REMEDY`, `AGENT_OVERRIDE`, and `PLATFORM_FEE` (the System wallet's cut of a payout, written by `postPayout` only when the fee is positive) — see [4.16](#416-unified-ledger-ledgerwriter) |

The `AuditAction` value `BOOKING_CANCELLED_BY_HOST` exists, but no service writes it yet; `TICKET_OPENED`
is emitted by `TicketServiceImpl`. See [Known Limitations](#known-limitations).

The System user (`SnoozeShare System`, role `SYSTEM`, status `ACTIVE` since W14) is seeded so
system-initiated rows can keep `actorUserId` non-null, and since W14 also owns a real wallet that
receives a `PLATFORM_FEE` row on every payout whose fee is positive. It is hidden from the Accounts
screen, and cannot sign in, be suspended, or be governed ([4.16](#416-unified-ledger-ledgerwriter)).

**Single ledger (W14).** A wallet movement writes exactly one `audit_log` money row — the row itself
carries the wallet's new `balanceAfter` — through `LedgerWriter`. Before W14, a movement wrote both a
`wallet_transactions` row and this `audit_log` row in one transaction (D13); `wallet_transactions` was
dropped once the fold shipped ([4.16](#416-unified-ledger-ledgerwriter)).

**Searching.** `AuditServiceImpl.search(filter, limit, offset)` returns rows newest first. Filters combine with AND.

- **Text** (one search box) matches in two ways, OR-ed. First, the text is resolved to user ids by a case-insensitive contains-match on `users.displayName` and `users.email` and on the `actorName` / `subjectName` snapshots, and the log is queried by those ids as actor or subject; because it goes by id, a user's rows are still found after a rename. Second, if the text is an **id fragment** (4 to 36 hex characters and dashes) it is also matched as a substring against `entityId`, `bookingId`, `ticketId`, `actorUserId` and `subjectUserId`. Text that resolves to nothing returns nothing.
- **Action types** are a set: rows must have one of the chosen actions (an `IN` clause with bound parameters); an empty set means every action.
- **Dates** are inclusive. `From` is the start of that day and `To` runs to the end of that day, both in the clock's zone, compared to the whole second.
- **Ordering** is `timestamp DESC`, then `rowid DESC` within an identical timestamp, so the last row written by an action is on top, like the rest of the list (operator change 2026-09-27).
- **Paging** is `limit` and `offset`. The screen asks for 200 rows at a time and shows a Load more button while a full page came back.

**Sequence diagram: a dispute resolution writes its audit rows.** All are written inside the
settlement transaction. A zero-amount side writes no wallet row, so a resolution writes three or four rows.

```mermaid
sequenceDiagram
  participant DS as DisputeSettlementServiceImpl
  participant TM as TransactionManager
  participant R as Jdbc repositories
  participant AU as AuditServiceImpl
  participant LW as LedgerWriter
  participant AL as JdbcAuditLogRepository
  DS->>TM: inTransaction(apply)
  TM->>R: save ticket (resolved), save booking (COMPLETED)
  TM->>AU: record TICKET_RESOLVED (status IN_REVIEW to resolved, reason)
  AU->>AL: save row 1
  TM->>AU: record BOOKING_COMPLETED (status CONFIRMED to COMPLETED)
  AU->>AL: save row 2
  TM->>LW: post(guest wallet, TICKET_REMEDY/AGENT_OVERRIDE, refund) [if above zero]
  LW->>AL: save row 3 (walletAdjustment = refund, balanceAfter)
  TM->>LW: postPayout(host wallet, net, fee) [if host share above zero]
  LW->>AL: save row 4 (walletAdjustment = net, balanceAfter, reason names the fee)
  LW->>AL: save row 5 (PLATFORM_FEE to the System wallet) [if fee above zero]
  TM-->>DS: commit (any failure rolls back all rows)
```

1. `settle` opens one transaction and saves the resolved ticket and the `COMPLETED` booking.
2. Row 1 is the ticket status change (`beforeState` is `IN_REVIEW`, `afterState` the resolved status) with the agent's reason.
3. Row 2 is the booking status change to `COMPLETED`, with the reason "Escrow settled by ticket resolution".
4. Row 3 is the guest wallet: `LedgerWriter.post` writes `walletAdjustment` as the refund (with the wallet's new `balanceAfter`); skipped when the refund is zero.
5. Row 4 is the host wallet: `LedgerWriter.postPayout` writes `walletAdjustment` as the payout **net** of the 3% fee, `reason` reads "Payout net of 3% platform fee (...)"; skipped when the host share is zero.
6. Row 5 is the System wallet's `PLATFORM_FEE` row, written by the same `postPayout` call, only when the fee is positive ([4.16](#416-unified-ledger-ledgerwriter)).
7. Rows 1 and 2 record status only; rows 3–5 record money only, each with its own `balanceAfter`. All carry the ticket and booking ids.
8. If anything throws, the transaction rolls back and no audit row survives.

**Data model: the whole schema.** Every table in [`desktop/db/schema.sql`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/db/schema.sql). To stay readable, each table other than `audit_log` lists only its keys and the columns that matter here; the full column lists are in the schema file, and [4.3](#43-persistence-tickets-and-ticket-categories) has the full `tickets` and `ticket_categories`. `audit_log` shows all its columns, with the V002 and V007 additions marked. `wallet_transactions` no longer exists — W14's V008 dropped it ([4.16](#416-unified-ledger-ledgerwriter)).

```mermaid
erDiagram
  users {
    TEXT userId PK
    TEXT role "GUEST/HOST/AGENT/SYSTEM, V006"
    TEXT displayName
    TEXT email
    TEXT accountStatus
    TEXT suspensionReason "V003"
  }
  properties {
    TEXT propertyId PK
    TEXT hostId FK
    TEXT status
    TEXT title
    REAL baseNightlyRate
  }
  availability_blocks {
    TEXT blockId PK
    TEXT propertyId FK
    TEXT bookingId FK
    TEXT source
    TEXT startDate
    TEXT endDate
  }
  bookings {
    TEXT bookingId PK
    TEXT listingId FK
    TEXT guestId FK
    TEXT status
    REAL totalAmount
  }
  wallets {
    TEXT walletId PK
    TEXT userId FK
    REAL balance "cache of newest audit_log.balanceAfter"
    TEXT currency
  }
  ticket_categories {
    TEXT categoryId PK
    TEXT label
    INTEGER active
  }
  tickets {
    TEXT ticketId PK
    TEXT bookingId FK
    TEXT raisedByUserId FK
    TEXT assignedAgentId FK
    TEXT category
    TEXT status
  }
  reviews {
    TEXT reviewId PK
    TEXT bookingId FK
    TEXT guestId FK
    INTEGER rating
  }
  audit_log {
    TEXT logId PK
    TEXT actorUserId FK
    TEXT actionType
    TEXT entityType
    TEXT entityId
    TEXT beforeState
    TEXT afterState
    TEXT timestamp
    TEXT actorName "V002"
    REAL walletAdjustment "V002"
    TEXT reason "V002"
    TEXT subjectUserId FK "V002"
    TEXT subjectName "V002"
    TEXT bookingId FK "V002"
    TEXT ticketId FK "V002"
    REAL balanceAfter "V007, W14"
  }
  schema_history {
    INTEGER version PK
    TEXT appliedAt
  }
  users ||--o{ properties : "hostId"
  users ||--o{ bookings : "guestId"
  properties ||--o{ bookings : "listingId"
  properties ||--o{ availability_blocks : "propertyId"
  bookings |o--o{ availability_blocks : "bookingId"
  users ||--o| wallets : "userId"
  bookings ||--o{ tickets : "bookingId"
  users ||--o{ tickets : "raisedByUserId"
  users |o--o{ tickets : "assignedAgentId"
  ticket_categories ||..o{ tickets : "label text, no FK"
  bookings ||--o{ reviews : "bookingId"
  users ||--o{ reviews : "guestId"
  users ||--o{ audit_log : "actorUserId"
  users |o--o{ audit_log : "subjectUserId"
  bookings |o--o{ audit_log : "bookingId"
  tickets |o--o{ audit_log : "ticketId"
```

1. `users` is the hub: it owns properties (as host), bookings (as guest), one wallet, tickets (as raiser and as assigned agent), reviews and audit rows. Since W14 one seeded row (the System user) has role `SYSTEM` and its own wallet.
2. `wallets` holds one cached `balance` per user; `audit_log` is the single money ledger (a row with a `walletAdjustment` and a `balanceAfter`), through which every top-up, withdrawal, escrow hold/refund, payout, ticket remedy, override and platform fee is written (W14, [4.16](#416-unified-ledger-ledgerwriter)). Only `actorUserId` is required among `audit_log`'s foreign keys; `entityId` is not a foreign key because it can name a money row, property, category, booking or ticket.
3. `ticket_categories` has no foreign key: a ticket stores the category's label text (D8).
4. `schema_history` is migration bookkeeping and has no relationships. Left out for size: each table's remaining columns, the `CHECK` constraints and the indexes.

**Depends on:** `UserRepository` (names), `AuditLogRepository`, a `Clock`, and the caller's `TransactionManager` transaction.

**Invariants**

- Audit rows are written only by services, on the caller's connection, inside the change's own transaction. A rolled-back change leaves no row; tests cover the rollback (`WalletLedgerAuditTest`, `DisputeSettlementAtomicityTest`, using `FailingAuditService`).
- One row is one change: a row has a status change or a wallet adjustment, never both (`AuditRecord`).
- A ticket resolution writes the rows in the sequence diagram above; the guest and host wallet rows are skipped when their amount is zero.
- Names are snapshotted onto each row when written, and text searches go by user id, so renames do not hide history.
- A money row's `balanceAfter` is written in the same `LedgerWriter` call as its `walletAdjustment`; `wallets.balance` is a cache of the owner's newest `balanceAfter`, checked by `LedgerConservationTest` ([4.16](#416-unified-ledger-ledgerwriter)).

**Deviations:** D13 (dual-write until W14 — historical, see [4.16](#416-unified-ledger-ledgerwriter)); D15 (the committed mock DB ships already migrated — now at `schema_history` v8 after W14, see 4.16; the System user has existed since V002, and `desktop/db/schema.sql` creates `schema_history`); D16 (seed `availability_blocks` ids were remapped to valid hex); D17 (the spec's `AuditService.query` became `search`, `CATEGORY_DELETED` became `TICKET_CATEGORY_DELETED`, and the search was corrected to a contains-match with a 4-character minimum for id fragments; the Audit Log's `REF` column was later replaced by `TARGET`, W14 follow-up).

### 4.15 Account governance

**Purpose:** let a support agent suspend and reactivate Guest and Host accounts, cascading a suspension to the account's upcoming bookings and listings atomically.

**API**

- [`AccountGovernanceService`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/AccountGovernanceService.java), implemented by [`AccountGovernanceServiceImpl`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/impl/AccountGovernanceServiceImpl.java): `listAccounts()` (every account except the System user, as [`AccountSummary`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/AccountSummary.java) read models), `suspend(userId, agentId, reason)` and `reactivate(userId, agentId, reason)`.
- `AccountStatusChangedEvent` ([`infra/events/events`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/infra/events/events/AccountStatusChangedEvent.java)) is published after commit; the Accounts screen refreshes on it.
- Supporting changes: `User.suspensionReason`, `UserRepository.findAll`, `BookingRepository.findByListing`, migration [`V003__suspension_reason.sql`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/resources/db/migration/V003__suspension_reason.sql), and a guard in `BookingServiceImpl.submitRequest` that refuses a guest whose account is not ACTIVE. The old `UserService.suspend` stub was removed (D19).

**Depends on:** the user, booking, property, wallet, transaction and availability repositories, `TransactionManager`, `AuditService`, the event bus and `BookingStateMachine`.

**Suspension flow** (a Host; a Guest skips the listing step and uses `findByGuest`):

```mermaid
sequenceDiagram
  actor Agent
  participant UI as AccountGovernanceController
  participant Svc as AccountGovernanceServiceImpl
  participant Repos as repositories
  participant Audit as AuditService
  participant Bus as EventBus
  Agent->>UI: Suspend, enter reason
  UI->>Svc: suspend(userId, agentId, reason)
  Svc->>Svc: validate actor, target, reason
  Svc->>Repos: save user SUSPENDED with reason
  Svc->>Audit: ACCOUNT_SUSPENDED
  loop each ACTIVE listing
    Svc->>Repos: save property INACTIVE
    Svc->>Audit: LISTING_STATUS_CASCADE
  end
  loop each PENDING or not-yet-started CONFIRMED booking
    Svc->>Repos: save FORCE_CANCELLED, release blocks
    Svc->>Repos: credit guest wallet, save refund transaction
    Svc->>Audit: BOOKING_FORCE_CANCELLED, ESCROW_REFUND
  end
  Svc-->>UI: commit, return user
  Svc->>Bus: AccountStatusChangedEvent and booking and wallet events
  Bus-->>UI: refresh the list
```

1. The service checks that the actor is an active Agent, the target is a Guest or Host, the reason is not blank, and the target is ACTIVE (SUSPENDED for reactivation).
2. Inside one `TransactionManager.inTransaction` call it saves the user with the new status and reason and writes `ACCOUNT_SUSPENDED`.
3. For a Host it sets each ACTIVE listing INACTIVE and writes `LISTING_STATUS_CASCADE` rows.
4. For each affected booking it moves the booking to `FORCE_CANCELLED` through `BookingStateMachine` (the agent role), releases the availability blocks and refunds the full booking amount to the guest's wallet with a ledger row, writing `BOOKING_FORCE_CANCELLED` and `ESCROW_REFUND` rows.
5. Any failure rolls back everything and publishes nothing; after commit the events are published.

**Invariants**

- Affected bookings are PENDING, or CONFIRMED with a check-in date after today (the injected clock's zone). A stay that has started, including one checking in today, or ended is untouched (C36).
- Reactivation only sets the status to ACTIVE, clears the reason and writes `ACCOUNT_REACTIVATED`. Nothing is restored (C34).
- Wallet writes go through `LedgerWriter.post`, on the same transaction as the suspension (W14); `LedgerWriter` never opens its own transaction, so this and every other caller wrap it in theirs ([4.16](#416-unified-ledger-ledgerwriter)).
- Money is `BigDecimal`; tests compare it with `compareTo` because SQLite returns `REAL`.

**Deviations:** D19 (`UserService.suspend` removed); D20 (the System user is hidden from the list; `BookingServiceImpl` gained a `UserRepository` parameter; the cascade refunds `totalAmount` without checking for an `ESCROW_HOLD` row).

### 4.16 Unified ledger (`LedgerWriter`)

**Purpose:** one place that changes a wallet balance, so every money-moving service writes the same
shape of row and a balance can never drift from the rows that explain it.

**API**

- [`LedgerWriter`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/service/impl/LedgerWriter.java): `post(walletId, type, amount, actorId, bookingId, ticketId, reason, at)` moves a signed `amount` in one wallet and writes its `audit_log` money row; `postPayout(hostWalletId, net, fee, actorId, bookingId, ticketId, at)` writes the host's `BOOKING_PAYOUT` row (`net`) and, only when `fee` is positive, a second `PLATFORM_FEE` row crediting the System wallet. Neither method opens a transaction — the caller wraps the call (and the state change it belongs with) in its own `TransactionManager.inTransaction` block.
- [`LedgerRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/LedgerRepository.java), implemented by [`JdbcLedgerRepository`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/repository/jdbc/JdbcLedgerRepository.java): `entriesForWallet(walletId)` and `entriesForBooking(bookingId)`, both newest-row-order reads of the money rows in `audit_log` (joined to `wallets` on `subjectUserId`). `EscrowPolicy` and the wallet statement/escrow-hint screens read through this instead of a dedicated ledger table.
- [`WalletTransactionType`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/main/java/com/snoozeshare/domain/enums/WalletTransactionType.java) gained `PLATFORM_FEE` alongside `TOP_UP`, `WITHDRAWAL`, `ESCROW_HOLD`, `ESCROW_REFUND`, `BOOKING_PAYOUT`, `TICKET_REMEDY` and `AGENT_OVERRIDE`.
- Callers: `WalletServiceImpl`, `TransactionServiceImpl`, `BookingServiceImpl`, `DisputeSettlementServiceImpl` ([4.5](#45-disputesettlementservice)) and `AccountGovernanceServiceImpl` ([4.15](#415-account-governance)). `WalletService` and `TransactionService` stay separate interfaces (C3, narrowed rather than removed by C39) — both now write through `LedgerWriter` instead of each hand-rolling "read wallet, add, save, insert ledger row".

**Events and the fee rule.** `LedgerWriter` queues a `WalletTransactionRecordedEvent` for every money row
it writes into `PostCommitEvents` ([3.1](#31-shared-foundation)); the shared `TransactionManager` publishes
the queue after commit and discards it on rollback, so no service publishes wallet events by hand and a
cancellation refreshes the header balance without a navigation cycle. A host payout therefore emits two
events (the payout and the System `PLATFORM_FEE` row). The fee is defined once:
`SettlementCalculator.platformFee(gross)` rounds 3% half-up and `hostNet(gross)` is gross minus that fee,
for normal completion and dispute settlement alike; a guest refund never carries a fee.

**Migrations:** three, run in order by `MigrationRunner`.

| Migration | Does |
|---|---|
| `V006__system_role.sql` | Rebuilds `users` (SQLite cannot `ALTER` a `CHECK` constraint) so `role` allows `SYSTEM`; sets the seeded System user's row to `role = 'SYSTEM'`, `accountStatus = 'ACTIVE'`. Run with foreign keys off, restored after (a SQLite requirement for rebuilding a referenced table). |
| `V007__unified_ledger.sql` | Adds `audit_log.balanceAfter`; backfills it on existing money rows from `wallet_transactions`; inserts any legacy `wallet_transactions` row that was never audited; creates the System wallet; backfills one legacy `PLATFORM_FEE` row per historical payout that had a fee, with a running System balance; adds `idx_audit_money` on `(subjectUserId, walletAdjustment)`. |
| `V008__drop_wallet_transactions.sql` | Drops `wallet_transactions` now that V007 copied everything into `audit_log`. |

**Sequence diagram: a host payout with a fee.**

```mermaid
sequenceDiagram
  participant Svc as a money-moving service
  participant TM as TransactionManager
  participant LW as LedgerWriter
  participant WR as WalletRepository
  participant AU as AuditServiceImpl
  Svc->>TM: inTransaction(...)
  TM->>LW: postPayout(hostWalletId, net, fee, actorId, bookingId, ticketId, at)
  LW->>WR: find, then save host wallet at balance + net
  LW->>AU: record BOOKING_PAYOUT (walletAdjustment=net, balanceAfter)
  alt fee > 0
    LW->>WR: find, then save System wallet at balance + fee
    LW->>AU: record PLATFORM_FEE (walletAdjustment=fee, balanceAfter)
  end
  LW-->>TM: the host's WalletTransaction
  TM-->>Svc: commit (or rollback and rethrow)
```

**Invariants**

- `LedgerWriter` never opens a transaction; every caller wraps it in the `TransactionManager` transaction that also carries the booking/ticket/account state change, so a balance change, its money row and the triggering state change commit or roll back together.
- A wallet's `balance` always equals its newest `audit_log` row's `balanceAfter` for that wallet, and each row's `balanceAfter` equals the previous balance plus that row's `walletAdjustment` — the conservation invariant `LedgerConservationTest` checks against the mock DB and against a real settlement.
- `postPayout` writes the `PLATFORM_FEE` row only when `fee` is positive, so a zero-fee payout (for example a $0.01 listing) never posts a zero-amount row — `LedgerWriter.post` rejects a zero amount outright.
- Listing rates must be positive with at most 2 decimal places (`ListingServiceImpl`, `HostListingFormController`), so a booking's settlement always nets to a positive host payout or an explicit zero handled by the caller (C42; a $0 listing is forbidden, not supported end to end).

**Deviations:** the spec's single ledger-fold migration was split into V006/V007/V008 so code could stop writing `wallet_transactions` (V006, V007) before it was dropped (V008), which the plan anticipated. C42: Task 8's review found a $0 or sub-cent listing rate crashed settlement once `LedgerWriter` rejected its zero-amount payout; the operator chose to forbid degenerate rates rather than build free-stay support end to end.


### 4.17 Web skeletons (`backend/`, `frontend/`)

**Purpose.** Prove that the web stack builds, deploys and talks to its database; no domain logic yet.

**API.**
- `GET /actuator/health` (backend): 200 with exactly `{"status":"UP"}`, or 503 with `{"status":"DOWN"}` when the database login fails. Every other actuator path is 404; details and probe groups are off. The authentication endpoints are in [4.18](#418-authentication-and-role-authorization-backendauth-frontend-providers).
- `SnoozeShareApplication` (`backend/src/main/java/com/snoozeshare/`), `config/CorsConfig`, `config/CorsProperties`, `ProdEnvironmentGuard`.
- `fetchBackendHealth(signal)` (`frontend/src/lib/api.ts`), `apiBaseUrl()` and `validateEnv()` (`frontend/src/lib/env.ts`), `BackendStatus` (`frontend/src/components/BackendStatus.tsx`).

**Depends on.** PostgreSQL 17 through Spring JDBC/JPA and Flyway (`db/migration/V001__baseline.sql`, a comment-only baseline; `V002__auth_users_wallets.sql` belongs to [4.18](#418-authentication-and-role-authorization-backendauth-frontend-providers); `V003`–`V005` belong to [4.19](#419-domain-model-repositories-and-services-backend-domain-layer)); the frontend depends on the backend URL.

**Invariants.**
- The `prod` profile has no defaults for the database or the allowed origin; `ProdEnvironmentGuard` fails startup and names any of `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` or `SNOOZESHARE_CORS_ALLOWED_ORIGIN` that is missing, and fails unless `snoozeshare.security.enabled` and `snoozeshare.firebase.enabled` are true ([4.18](#418-authentication-and-role-authorization-backendauth-frontend-providers)). It also fails unless `snoozeshare.ai.enabled` is set (naming `SNOOZESHARE_AI_ENABLED`), rejects `snoozeshare.ai.mock=true`, and, when the AI gateway is enabled, requires a non-blank `https` base URL and key ([4.20](#420-ai-gateway-backendai)). With no profile set the `dev` profile applies.
- The allowed origin is one `http(s)` origin with no wildcard and no trailing slash, or the application does not start.
- `src/lib/env.ts` is the only reader of `NEXT_PUBLIC_API_BASE_URL`, and a production build fails without it.
- The health body must stay exactly `{"status":"UP"}`.

**Deviations.** D39 (the guard exists because Spring's binder leaves an unresolved `${VAR}` in place); D40 (the request `Origin` is compared after Spring strips a trailing slash); D41 (Render runs on Kubernetes, so Spring's probe groups are disabled explicitly and `HealthOnKubernetesTest` simulates the platform).


### 4.18 Authentication and role authorization (`backend/auth/`, frontend providers)

**Purpose.** Establish who is calling the API and which role they hold, and keep Guest, Host and Support Agent access apart on the web. Firebase Authentication owns passwords and tokens; the backend owns each person's role and wallet. The role pages are empty landing shells so far.

**API.** Every error body is `{"code", "message", "requestId"}`; `requestId` is logged with the status, code and path.

| Endpoint | Caller | Result |
|---|---|---|
| `POST /api/auth/registration-code/validate` | Anyone; body `{role: HOST or AGENT, registrationCode (1-128 characters)}` | `204` for a correct code; `400 VALIDATION_ERROR`, `403 INVALID_REGISTRATION_CODE`, `429 REGISTRATION_RATE_LIMITED` |
| `POST /api/auth/register` | Bearer ID token; body `{role, registrationCode? (at most 128 characters), displayName? (at most 80 characters)}` | `201` with the new profile, `200` for a same-role retry; `400`, `403 INVALID_REGISTRATION_CODE` or `PROFILE_INACTIVE`, `409 ROLE_ALREADY_ASSIGNED`, `429` |
| `GET /api/auth/me` | Bearer ID token | The profile `{id, email, role, displayName, walletId}`; `404 PROFILE_NOT_REGISTERED`, `403 PROFILE_INACTIVE` |
| `/api/guest/**`, `/api/host/**`, `/api/agent/**` | Bearer ID token and the matching active role | `403 ROLE_MISMATCH` otherwise; no endpoints exist under these yet |

At the security boundary a missing or invalid token is `401 INVALID_TOKEN` and an unreachable Firebase is `503 AUTH_PROVIDER_UNAVAILABLE`; an unexpected failure under `/api/auth` is `500 INTERNAL_ERROR`. Request bodies reject unknown fields, a code longer than 128 characters and a Guest request that carries a code.

- Backend (`backend/src/main/java/com/snoozeshare/auth/`): `config/SecurityConfig`, `config/FirebaseConfig`; `security/FirebaseAuthFilter`, `security/RoleAuthorizationFilter`, `security/RoleAuthorizationService`, `security/FirebaseTokenVerifier` and `FirebaseAdminTokenVerifier`, `security/ClientIpResolver`, `security/AuthErrorWriter`; `controller/AuthController`; `service/AuthRegistrationService`, `AuthRegistrationWriter`, `RegistrationCodeValidationService`, `RegistrationRateLimiter`, `PublicRegistrationRateLimiter`; `repository/AuthUserRepository` and `WalletRepository` with their JDBC implementations; the request and response records in `api/`.
- Frontend (`frontend/src/`): `lib/firebase.ts` (`signIn`, `registerWithEmail`, `signOutUser`, `observeAuth`, `resetPassword`), `lib/api.ts` (`authenticatedFetch`, `validateRegistrationCode`, `registerProfile`, `fetchCurrentProfile`, `BackendApiError`), `providers/AuthProvider.tsx` (`useAuth`), `components/auth/RouteGuard.tsx` (`RouteGuard`, `AuthOnly`), `components/auth/useRedirectAuthenticated.ts`, `components/role/RoleShell.tsx` and `RoleNavbar.tsx`. Pages: `/login`, `/register`, `/forgot-password`; layouts `/guest`, `/host`, `/agent` wrap their landing page in `RouteGuard` and `RoleShell`.

**Depends on.** Firebase Authentication (Email/Password) through the Firebase JS SDK 12 in the browser and Firebase Admin 9.11 on the server, which takes credentials from Application Default Credentials; Spring Security; PostgreSQL through Spring JDBC and Flyway (`db/migration/V002__auth_users_wallets.sql`).

**Data model.** `users` holds the role and status; `wallets` has one row per Guest or Host and none for an Agent.

```mermaid
erDiagram
  users ||--o| wallets : "has (GUEST and HOST only)"
  users {
    uuid id PK
    varchar firebase_uid UK
    varchar email
    varchar role "GUEST, HOST or AGENT"
    varchar display_name
    varchar status "ACTIVE or INACTIVE"
    timestamptz created_at
    timestamptz updated_at
  }
  wallets {
    uuid id PK
    uuid user_id FK
    varchar currency "SGD"
    numeric balance
  }
```

**Registration flow.** The Host and Agent code is checked before the Firebase account exists, so a wrong code never creates one.

```mermaid
sequenceDiagram
  actor U as User
  participant P as register/page.tsx
  participant FB as Firebase Authentication
  participant F as FirebaseAuthFilter
  participant C as AuthController
  participant S as AuthRegistrationService
  participant W as AuthRegistrationWriter
  U->>P: role, code, email, password
  P->>C: POST /api/auth/registration-code/validate (Host or Agent)
  C-->>P: 204, or 403 / 429 (stop here)
  P->>FB: createUserWithEmailAndPassword
  FB-->>P: signed-in user
  P->>F: POST /api/auth/register with Bearer ID token
  F->>FB: verify ID token
  F->>C: authenticated principal
  C->>S: register(identity, request, ip)
  S->>W: create(user)
  W-->>P: 201 profile (users row, plus wallets row for Guest and Host)
```

1. For a Host or Agent, `validateRegistrationCode` asks the backend to check the code. The limiter is consulted before the comparison, and the comparison is constant-time. A wrong or rate-limited code stops the flow, so no Firebase account is created. A Guest skips this step.
2. If nobody is signed in, `registerWithEmail` creates the Firebase account. A person who is already signed in after an earlier failed registration skips it.
3. `registerProfile` sends the request through `authenticatedFetch`, which adds the ID token.
4. `FirebaseAuthFilter` verifies the token (revocation checked, project ID matched) and sets the principal. The UID and email come only from this token.
5. `AuthRegistrationService.register` checks the code again for Host and Agent, so a modified client cannot skip step 1, then looks the user up by `firebase_uid`. An existing profile with the same role returns `200` unchanged; a different role is `409`; a non-`ACTIVE` profile is `403`.
6. For a new user, `AuthRegistrationWriter.create` inserts the `users` row and, for Guest and Host, a `wallets` row of SGD 0.00 in one transaction. Two simultaneous registrations for one UID leave one profile: the loser reads the winner and returns it.
7. The page routes to `/guest`, `/host` or `/agent`.

**Frontend states and guards.** `AuthProvider` loads `/api/auth/me` whenever Firebase reports a user and exposes one of six states. A profile response that arrives after a newer request or a sign-out is ignored.

| State | Meaning | `RouteGuard` result |
|---|---|---|
| `loading` | Firebase or `/me` not resolved | "Loading…", no redirect |
| `signed_out` | No Firebase user, or the backend rejected the token (`401` from `/me`), in which case the provider also ends the Firebase session | Redirect to `/login?returnTo=<path>` |
| `registering` | Signed in, `PROFILE_NOT_REGISTERED` | Redirect to `/register` |
| `authenticated` | Profile loaded | Render the page if the role matches; otherwise redirect to the person's own role page |
| `inactive` | `PROFILE_INACTIVE` | "Profile unavailable" |
| `error` | Any other failure, or incomplete Firebase configuration | Retry and Sign out buttons |

**Invariants.**
- The role is the `users.role` column, never a Firebase claim (C86), and one account has one role (`409 ROLE_ALREADY_ASSIGNED`). `RoleAuthorizationFilter` reads it from the database on every request and requires `ACTIVE`.
- With security off (the `dev` default) every path is open and `AuthController` is not registered. The `prod` profile cannot start in that state (C95).
- The Host and Agent codes are configuration secrets; they are never logged, returned or compared with `equals`. Both the public check and `/register` apply the limits first: 5 invalid attempts per IP and role (public) or per user and IP (register), and 20 per IP, within 10 minutes. A correct code resets only its own counter, and a Guest registration resets nothing.
- The client address is the direct peer unless `SNOOZESHARE_TRUST_FORWARD_HEADERS=true`, and then `CF-Connecting-IP`; `X-Forwarded-For` is never read (C97). The limiters are process-local.
- Email verification is not required (C89), so an unverified Firebase account can register and sign in.
- A failed registration keeps the Firebase session and the `registering` state so the person can retry or sign out (C90). `returnTo` is followed only when it parses to a same-origin path.
- Route guards run in the browser only. The backend filters are the authority.
- Errors and logs never contain a token, a password or a code.

**Deviations.** D44 (Firebase beans exist only when `FIREBASE_ENABLED` is true, so contexts without a database or credentials still start).


### 4.19 Domain model, repositories and services (`backend/` domain layer)

**Purpose.** Port the desktop app's domain model, repositories and core services to Spring Boot with Spring Data JPA on PostgreSQL, so the role-feature workstreams (W-GUEST, W-HOST, W-AGENT) can build REST controllers on top of working business logic.

**Packages.**

| Path | Contents |
|---|---|
| `backend/src/main/java/com/snoozeshare/domain/model/` | 12 JPA entity classes (`User`, `Wallet`, `Property`, `Booking`, `AvailabilityBlock`, `Ticket`, `TicketCategory`, `Review`, `AuditLogEntry`, `Message`, `BookingMessage`) + `AmenitySetConverter` (comma-separated `TEXT` ↔ `Set<AmenityType>`) + `WalletTransaction` record (a projection, not an entity) |
| `domain/enums/` | 13 enums copied from desktop: `AccountStatus`, `AmenityType`, `AssigneeFilter`, `AuditAction`, `BookingStatus`, `ListingStatus`, `PropertyType`, `RemedyType`, `ResolutionMode`, `Role`, `ThreadChannel`, `TicketStatus`, `WalletTransactionType` |
| `domain/statemachine/` | `BookingStateMachine`, `TicketStateMachine` — the single legality check for status transitions, copied from desktop |
| `domain/policy/` | `StayDatePolicy`, `DisputeWindowPolicy` — date rules, updated for entity-style getters |
| `domain/settlement/` | `SettlementCalculator`, `SettlementBreakdown`, `EscrowPolicy` — escrow split maths, copied from desktop |
| `domain/validation/` | `DomainValidation` — field validation (postal codes, rates, required text), copied from desktop |
| `repository/` | 12 Spring Data JPA repository interfaces (`UserRepository`, `WalletRepository`, `PropertyRepository`, `BookingRepository`, `AvailabilityBlockRepository`, `TicketRepository`, `TicketCategoryRepository`, `ReviewRepository`, `AuditLogRepository`, `MessageRepository`, `BookingMessageRepository`, `LedgerRepository`) plus custom query implementations (`PropertySearchRepositoryImpl`, `AuditLogSearchRepositoryImpl`, `TicketQueueRepositoryImpl`) and two `JdbcTemplate`-based repositories (`JdbcMessageReadRepository`, `JdbcBookingMessageReadRepository`, `JdbcLedgerRepository`) |
| `service/` | 13 service interfaces (`BookingService`, `ListingService`, `TicketService`, `DisputeSettlementService`, `DisputeQueryService`, `AccountGovernanceService`, `AvailabilityService`, `MessageService`, `BookingConversationService`, `ReviewService`, `WalletService`, `TransactionService`, `AuditService`, `ListingMetricsService`, `UserService`) and 14 value-object records |
| `service/impl/` | 15 service implementations + `AuthorizationService` utility + `LedgerWriter` |
| `service/requests/` | `NewTicketRequest`, `ResolutionRequest` — internal request types used by services |
| `api/request/` | 16 request DTOs (Java records with `@Valid` and `@JsonIgnoreProperties(ignoreUnknown = false)`) |
| `api/response/` | 21 response DTOs (Java records with `static from()` factory methods) |
| `api/mapper/` | 3 mapper utilities: `ListingMapper`, `TicketMapper`, `SearchMapper` |
| `db/migration/V003__domain_tables.sql` | ALTERs W-AUTH's `users`/`wallets` tables and creates 11 domain tables |
| `db/migration/V004__seed_system_user.sql` | Seeds the SYSTEM user and platform-fee wallet |
| `db/migration/V005__tighten_nightly_rate_constraint.sql` | Rejects zero nightly rate and enforces two-decimal precision |

**Depends on.** Spring Data JPA and Hibernate on PostgreSQL 17 (Flyway V003–V005 on top of V001–V002); Spring's `@Transactional` and `ApplicationEventPublisher` replace the desktop's `TransactionManager` and `InProcessEventBus`.

**Data model.** The 14 tables managed by Flyway V002–V005 (V002 creates `users` and `wallets`; V003 ALTERs them and creates the rest). All primary keys are `id UUID`.

```mermaid
erDiagram
  users ||--o| wallets : "has (GUEST, HOST)"
  users ||--o{ properties : "hosts"
  users ||--o{ bookings : "books as guest"
  users ||--o{ tickets : "raises"
  users ||--o{ reviews : "writes"
  properties ||--o{ bookings : "booked on"
  properties ||--o{ availability_blocks : "blocked on"
  bookings ||--o| availability_blocks : "sourced block"
  bookings ||--o{ tickets : "disputed by"
  bookings ||--o{ booking_messages : "chat"
  bookings ||--o{ booking_message_reads : "read markers"
  tickets ||--o{ messages : "threads"
  tickets ||--o{ message_reads : "read markers"
  bookings ||--o{ reviews : "reviewed"
  users ||--o{ audit_log : "actor"
```

Left out for clarity: the `ticket_categories` lookup table (no FK from `tickets`, which stores the label text — D8); the self-referencing `assigned_agent_id` on `tickets`; and the `subject_user_id`, `booking_id` and `ticket_id` FKs on `audit_log`.

**Invariants.**

- JPA is configured with `ddl-auto: validate` (Hibernate checks the entity mappings against the Flyway-managed schema at startup) and `open-in-view: false` (no lazy loading outside a transaction).
- Every entity uses `id` as the PK column name, matching W-AUTH's `users` and `wallets` convention (C99). The getter is `getId()`, not an entity-specific name.
- All service implementations and JDBC repositories carry `@ConditionalOnDatabase` (`config/ConditionalOnDatabase`), which matches unless `spring.autoconfigure.exclude` lists `DataSourceAutoConfiguration`, so test contexts that have no database (e.g. `ServerPortDefaultTest`) start without them. `DatabaseBeansTest` checks that they exist in the running application.
- Service implementations use entity-style getters (`.getField()`) and mutation via setters + `repository.save()`, not record accessors or new-record construction.
- `BookingServiceImpl.submitRequest` runs at `Isolation.SERIALIZABLE` to prevent double-booking the same dates.
- `WalletRepository.findByIdForUpdate` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` so concurrent money operations on the same wallet are serialised.
- Wallet balance uses `NUMERIC(19,2)` (from W-AUTH's V002); other money columns (rates, amounts, adjustments) use `NUMERIC(19,4)` (C100).
- `WalletTransaction` is a record, not a JPA entity — it is a projection from `audit_log` rows, read by `JdbcLedgerRepository`.
- The domain enums in `domain/enums/` are copied verbatim from the desktop, with only the package declaration changed.
- `LedgerWriter` is the single class that changes a wallet balance on the web, just as on the desktop ([4.16](#416-unified-ledger-ledgerwriter)). It posts audit rows and publishes `WalletTransactionRecordedEvent` via Spring's `ApplicationEventPublisher` after the transaction commits.
- `AuthorizationService` checks that the acting user exists, is `ACTIVE`, and holds the required role; services delegate to it instead of repeating the check.
- Request DTOs use Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@DecimalMin`, `@Size`) and reject unknown JSON fields. Response DTOs expose a `static from(entity)` factory.

**Deviations.** D45 (W-AUTH's V002 used `id` as the PK for `users` and `wallets` instead of entity-specific names, so the plan's V002 migration was restructured as V003 ALTER + CREATE, and all entity PK fields were renamed to `id`). D46 (the Flyway version numbers shifted: domain tables to V003, seed to V004, constraint to V005). D51 (these classes first carried `@ConditionalOnBean(PropertyRepository.class)`, which is evaluated while Spring scans components, before Spring Data registers its repositories, so it never matched and the services and JDBC repositories were absent from the running application until W-AI-GATEWAY replaced it; [4.20](#420-ai-gateway-backendai)).

### 4.20 AI gateway (`backend/ai/`)

**Purpose.** One backend entry point, `LlmGateway.run`, through which every role's AI feature calls the NUS SoCLaaS LLM, with guardrails, a shared request budget and an audit row. It adds no endpoint, table or migration, and contains no role feature: each role registers its own `AiFeature` and owns its prompt, output type and endpoint.

**Packages.** All under `backend/src/main/java/com/snoozeshare/ai/`.

| Path | Contents |
|---|---|
| `ai/` | `LlmGateway`, `AiFeature`, `AiFeatureRegistry`, `LlmCall`, `LlmResult`, `LlmErrorCode`, `OutputSpec`, `Turn`, `TurnRole`, `AuditContext`, `ModelTier` |
| `ai/config/` | `LlmProperties` (the validated `snoozeshare.ai.*` settings), `AiConfig` (beans that need no database), `ModelCheck` (startup model check) |
| `ai/guard/` | `InputSanitizer`, `PiiRedactor`, `InjectionDetector`, `PromptBuilder`, `OutputValidator` — pure classes: no Spring, no I/O, no clock |
| `ai/limit/` | `SchedulerCore` and `LlmScheduler` (global rate, concurrency and priority queue), `UserThrottle`, `CircuitBreaker`, `BudgetBlock`, `Backoff`, and the seams `Sleeper` and `Jitter` |
| `ai/client/` | `SoclaasClient` (one HTTP attempt) and `AttemptResult` |
| `ai/audit/` | `AiAuditWriter` (audit rows, daily-cap counts and reservations) |
| `config/ConditionalOnDatabase.java` | Registers a bean only when the application has a database ([4.19](#419-domain-model-repositories-and-services-backend-domain-layer)) |
| `domain/enums/AuditAction.java` | Gains `AI_CALL` and `AI_CALL_REJECTED` |

**API.** A role service registers an `AiFeature<T>` bean and calls the gateway:

```java
LlmResult<T> r = gateway.run(feature, new LlmCall(userId, role, turns, trustedContext, untrustedContext, auditContext));
```

| Type | Contents |
|---|---|
| `AiFeature<T>` | `id` (`^[a-z]+\.[a-zA-Z]+$`, at most 40 characters, unique), `allowedRoles` (a non-empty subset of GUEST, HOST, AGENT), `tier` (`FAST`, `STANDARD`, `STRONG`), `systemPrompt`, `maxTokens` (1–4,000), `temperature` (0.0–1.0), `priority` (0 for staff-facing features, up to 2), `perUserPerMinute`, `maxInputChars` (default 2,000), `maxTurns` (default 10), `output` (`OutputSpec.text(maxChars, allowLinks)` or `OutputSpec.json(RecordClass, allowLinks)`), `fallback` and `mockResponse` |
| `LlmCall` | `userId`, `role`, `turns` (USER or ASSISTANT `Turn`s, the last one USER), `trustedContext` (server-built text only), `untrustedContext` (user- or host-authored text), `audit` (`AuditContext` with optional ticket, booking and subject ids) |
| `LlmResult<T>` | `ok`, `value` (the validated output, or the feature's fallback), `code`, `flagged` (the input matched an injection pattern), `truncated`, `callId` |
| `LlmErrorCode` | `DISABLED`, `ROLE_DENIED`, `USER_THROTTLED`, `BLOCKED_INPUT`, `DAILY_CAP`, `BUDGET_EXHAUSTED`, `CIRCUIT_OPEN`, `QUEUE_FULL`, `QUEUE_TIMEOUT`, `RATE_LIMITED`, `SERVER`, `TIMEOUT`, `NETWORK`, `BAD_REQUEST`, `AUTH`, `FORBIDDEN`, `INVALID_OUTPUT`, `REFUSED`, `AUDIT_FAILED` |

`run` never throws for an expected failure: it returns the feature's fallback with a code. It throws `IllegalArgumentException` for a malformed call or feature and `IllegalStateException` when a database transaction is active. A bad `AiFeature` bean, or two with the same id, fails startup naming the feature id and the field.

**Pipeline.** `run` applies a fixed order of checks, and the first that fails decides the code.

```mermaid
flowchart TD
  A["LlmGateway.run(feature, call)"] --> B{"shape, no active transaction"}
  B -- "malformed" --> X["IllegalArgumentException / IllegalStateException"]
  B --> C{"enabled, role allowed, UserThrottle"}
  C -- "no" --> F["fallback with a code, no audit row"]
  C --> E["PromptBuilder: sanitize and assemble"]
  E -- "last turn blank" --> R["AI_CALL_REJECTED row"]
  E --> G{"AiAuditWriter.reserve: daily caps"}
  G -- "over a cap" --> R
  G --> H{"mock mode?"}
  H -- "yes" --> K["mockResponse"]
  H -- "no" --> I["attempt loop: BudgetBlock, CircuitBreaker, LlmScheduler, SoclaasClient"]
  K --> J["OutputValidator"]
  I --> J
  J --> L["AiAuditWriter.write, release the reservation"]
  R --> M["LlmResult"]
  L --> M
  F --> M
```

1. `run` rejects a malformed `LlmCall` or `AiFeature` and any call made inside a transaction (a call can take 45 s and must not hold a connection).
2. A disabled gateway, a role outside `allowedRoles`, or an exhausted per-user minute or in-flight limit returns the fallback with `DISABLED`, `ROLE_DENIED` or `USER_THROTTLED`. These three write no row.
3. `PromptBuilder` sanitizes every turn and the contexts, sets `flagged` and `truncated`, and assembles the messages. A last turn that is empty after sanitizing returns `BLOCKED_INPUT` with a rejected row.
4. `AiAuditWriter.reserve` counts today's (UTC) `AI_CALL` rows plus in-flight reservations against the per-user and global caps; over a cap returns `DAILY_CAP` with a rejected row, and a failed count returns `AUDIT_FAILED`.
5. Mock mode answers from the feature's `mockResponse`; otherwise the attempt loop runs (next diagram).
6. `OutputValidator` checks the reply. `AiAuditWriter.write` then writes one row (`AI_CALL` when a request was sent or in mock mode, otherwise `AI_CALL_REJECTED`) and the reservation is released. If the row of an otherwise good result cannot be written, the result is `AUDIT_FAILED` with the fallback.

**One call, with a retry.**

```mermaid
sequenceDiagram
  participant S as Role service
  participant G as LlmGateway
  participant A as AiAuditWriter
  participant Q as LlmScheduler
  participant C as SoclaasClient
  participant U as SoCLaaS
  participant V as OutputValidator
  S->>G: run(feature, call)
  G->>A: reserve(userId, priority)
  loop up to max-retries + 1 attempts
    G->>G: BudgetBlock.tryAdmit, CircuitBreaker.tryAdmit
    G->>Q: acquire(priority, arrival, maxWait)
    Q-->>G: Permit
    G->>C: attempt(request, timeout)
    C->>U: POST /chat/completions
    U-->>C: 503, then 200 on the retry
    C-->>G: Failure(SERVER), then Success
    G->>G: Backoff.delayMs, Sleeper.sleep
  end
  G->>V: validate(content, finishReason, output)
  V-->>G: Outcome
  G->>A: write(callId, ...), release
  G-->>S: LlmResult
```

1. The gateway reserves one call against the daily caps before anything is sent.
2. Every attempt, retries included, first asks the budget block and the circuit breaker; a rejection ends the loop with `BUDGET_EXHAUSTED` or `CIRCUIT_OPEN` (or the last failure's code if a request was already sent).
3. The attempt takes a permit from the one global scheduler. Priority 0 jobs run first and may use every concurrency slot; other priorities may use all but one.
4. `SoclaasClient.attempt` sends one request and classifies the response. `RATE_LIMITED`, `SERVER`, `TIMEOUT` and `NETWORK` are retried after a backoff (500 ms doubling, at most 8 s, jittered; the `Retry-After` value when present, at most 10 s); everything else ends the loop. The whole call stops at `deadline-ms`.
5. After the loop the reply is validated and audited as above.

**Defaults** (all overridable through `snoozeshare.ai.*`; an out-of-range value fails startup naming the key):

| Setting | Default |
|---|---|
| `max-rpm` / `max-concurrent` / `max-queue` | 25 / 4 / 20 |
| `queue-wait-ms` / `request-timeout-ms` / `deadline-ms` | 10,000 / 30,000 / 45,000 |
| `max-retries` | 2 |
| `breaker-failures` / `breaker-open-ms` | 5 (from at least 3 different users) / 30,000 |
| `budget-block-ms` | 900,000 (15 minutes, never past 00:00 UTC) |
| `per-user-max-in-flight` | 2 |
| `daily-cap-per-user` / `global-daily-cap` | 200 / 3,000 per UTC day |
| `priority-zero-reserve-percent` | 5 (the share of the global cap only priority 0 calls may use) |
| `max-prompt-chars` | 40,000 |
| `response-format-json` / `reasoning-effort` | `false` / `none` |
| `model-fast` / `model-standard` / `model-strong` | `llama3.1:8b` / `qwen3.5:9b` / `gemma4:26b` |

**Security measures.**

- **Key.** The key comes only from the environment (`SOCLAAS_API_KEY`), development and production use separate keys, and it never appears in logs, errors, results, request bodies or the audit log. `LlmProperties.toString` prints it as `***`; `SecretScanTest` fails the build if a key-shaped string is committed; `AiLogLeakTest` checks the logs across failure scenarios.
- **Transport.** The base URL must be `https` (loopback `http` is allowed outside `prod`) and redirects are never followed, so the key cannot be sent to another host. The whole response is read within the attempt timeout and capped at 262,144 bytes.
- **Input.** Text is Unicode-normalised, control and format characters are stripped, card numbers, NRIC/FIN numbers, Singapore phone numbers and e-mail addresses are redacted, prompt delimiter tags are removed, and the result is wrapped in `<user_input>` and `<untrusted_data>` tags under a fixed security suffix. Phrases such as "ignore the previous instructions" set `flagged`; the role code decides what to do.
- **Output.** A reply is rejected as `INVALID_OUTPUT` if it contains a link (unless the feature sets `allowLinks`), HTML, a Markdown image, key-shaped text or a leftover prompt tag, if the finish reason is not `stop`, or if it fails the length or JSON-schema check. JSON strings are checked after parsing, so escaped tricks do not pass. A reply that is only `{"refusal":true}` returns `REFUSED`.
- **Abuse and cost.** Per-user minute and in-flight limits, daily caps counted from `audit_log` (so they survive a restart), a priority-0 share reserved for staff-facing features, and a circuit breaker that needs failures from three different users before it opens.
- **Audit.** One metadata-only row per call (feature, model, tokens, latency, attempts, `flagged`, `truncated`, a hash of the prompt). No prompt or reply text is stored. The audit is fail-closed: an unwritable row withholds the answer.

**Configuration.**

| Variable | Notes |
|---|---|
| `SNOOZESHARE_AI_ENABLED` | Required in `prod` (no default); `render.yaml` sets it to `"false"`. In `dev` it defaults to `false` |
| `SOCLAAS_BASE_URL`, `SOCLAAS_API_KEY` | Required only when enabled; the base URL ends in `/v1` |
| `SNOOZESHARE_AI_MOCK` | Local only: answer from each feature's `mockResponse`; `prod` rejects `true` |
| `SNOOZESHARE_AI_MODEL_FAST`, `_STANDARD`, `_STRONG` and other `snoozeshare.ai.*` keys | Optional overrides, for example `SNOOZESHARE_AI_MAX_RPM` |

**Observed upstream behaviour** (T8 live checks, 2026-10-22, recorded in `docs/runbooks/dev-environment.md` § Evidence):

| Item | Observed |
|---|---|
| Models | `llama3.1:8b`, `qwen3.5:9b` and `gemma4:26b` are offered as chat models to the development key |
| `response_format` `json_object` | Accepted (HTTP 200) by all three |
| `reasoning_effort` `none` | Accepted by all three; turns reasoning off on `qwen3.5:9b` and `gemma4:26b` |
| `reasoning_effort` `low` | `llama3.1:8b` returns HTTP 400 ("does not support thinking"); the other two accept it but still reason |
| Reasoning models without the option | `qwen3.5:9b` and `gemma4:26b` spend `max_tokens` on reasoning first, so a small limit returns empty `content` with `finish_reason` `length`; a one-word answer took 135 tokens on `qwen3.5:9b` |
| Rate limit | The development key allows 30 requests a minute (hence the `max-rpm` default of 25). A burst of 100 requests produced no 429, so `Retry-After` and the budget-429 wording are unobserved |
| Reachability | SoCLaaS is reachable from the Render dev service (confirmed by the owner) |

**Rules for role code** (also in `docs/runbooks/dev-environment.md`):

1. Take `userId` and `role` from the authorized principal, never from a request body (C79).
2. Call `run` outside any transaction.
3. Build ASSISTANT turns only from server-stored history; put only server-built text in `trustedContext`; put user- or host-authored text in `untrustedContext` or USER turns.
4. Act on `flagged` and `truncated`.
5. Render `value` as plain text, through a response DTO (C73); never as HTML or Markdown.
6. Treat a degraded result as final: do not loop on it.

**Depends on.** SoCLaaS `POST {base-url}/chat/completions` (and `GET {base-url}/models` once at startup, by `ModelCheck`) through one shared `java.net.http.HttpClient` and Jackson; `AuditService` and `AuditLogRepository` for the audit rows and cap counts. No new library. With the gateway disabled, or SoCLaaS unreachable, the backend and its health endpoint are unaffected.

**Invariants.**

- Nothing outside `com.snoozeshare.ai` imports `ai.config`, `ai.guard`, `ai.limit`, `ai.client` or `ai.audit`, and nothing outside `ai/client` uses `java.net.http` or the text `chat/completions` (`AiBoundaryTest`).
- Daily boundaries use UTC; waiting and randomness go through `Sleeper` and `Jitter`, so tests need no real sleep.
- Limiter, breaker and budget-block state is process-local and lost on restart (C69).
- `LlmGateway`, `AiAuditWriter` and `ModelCheck` carry `@ConditionalOnDatabase`, so a datasource-free context starts without a gateway bean (`AiDatasourceFreeContextTest`).

**Deviations.** D47–D56 (W-AI-GATEWAY). The ones that matter to a reader: D48 (output validation is stricter than the spec in three places), D51 (the `@ConditionalOnBean` registration bug found and fixed here, [4.19](#419-domain-model-repositories-and-services-backend-domain-layer)), D52 (`ModelCheck` makes its own executor, because any `Executor` bean would switch off Spring Boot's task executor) and D55 (the code-review fixes). Residual limits are listed in [Known Limitations](#known-limitations).

---

## 5. Requirements

### Functional Requirements

The list is ordered by the implemented feature progression and uses the backlog identifiers where
they exist. It describes current behavior, including deliberate deferrals and coverage gaps.

- **F0.1.1** (desktop) Registration creates a Guest, Host or Agent account; Host and Agent registration requires the corresponding mocked registration code.
- **F0.1.2** (desktop) Mocked email login establishes the active `SessionContext` and routes the user to the Guest, Host or Agent shell; no real credential verification is performed.
- **F0.1.3** (web) The `/login`, `/register` and `/forgot-password` pages call Firebase Email/Password sign-in, sign-up and password reset and show loading and error states.
- **F0.1.4** (web) A protected request with a missing, malformed, expired, revoked or wrong-project token gets `401 INVALID_TOKEN` and never reaches a controller.
- **F0.1.5** (web) A valid Firebase identity with no profile can register once, whether or not its email is verified; registration creates one backend profile and returns its role and ID.
- **F0.1.6** (web) Host and Agent registration needs the matching registration code; a missing, wrong or cross-role code gets `403 INVALID_REGISTRATION_CODE` and creates no profile.
- **F0.1.7** (web) A Guest or Host gets exactly one wallet at registration and an Agent gets none; a retry creates no second wallet.
- **F0.1.8** (web) A request to another role's URL namespace gets `403 ROLE_MISMATCH`.
- **F0.1.9** (web) `GET /api/auth/me` returns the profile keyed by the verified UID.
- **F0.1.10** (web) The frontend exposes the six states of [4.18](#418-authentication-and-role-authorization-backendauth-frontend-providers) and applies its guard matrix for sign-in, registration, wrong-role access, sign-out and backend failure.
- **F0.1.11** (web) For a Host or Agent, the register page asks the backend to validate the code before it creates the Firebase account.
- **F0.1.12** (web) A wrong, blank, cross-role or malformed code fails validation with a stable error and the page does not create a Firebase account.
- **F0.1.13** (web) A correct code passes validation without creating a database row, a Firebase account or a session.
- **F0.1.14** (web) `/api/auth/register` validates the code again and keeps its profile, wallet, role, idempotency and rate-limit behavior, so a modified client cannot skip the first check.
- **F0.1.15** (web) A Guest registration never calls code validation.
- **F0.1.16** (web) The page tells a code-validation failure from a Firebase sign-up failure, and neither shows the configured code.
- **F0.2.1** Successful Guest and Host registration provisions a zero-balance wallet.
- **F1.1.1** Guests can search active listings by city, minimum guest capacity, check-in/check-out dates and an optional maximum nightly budget (`SearchCriteria.maxNightlyRate`); availability considers host blocks and pending or confirmed bookings.
- **F1.2.1** Guests can open a listing detail view with property specifications, address, host name, amenities, check-in/out times and pricing.
- **F1.2.2** The detail view calculates and displays the nightly rate, number of nights and total price for the selected date range.
- **F2.1.1** Guests can submit a date-range booking request, creating a `PENDING` booking.
- **F2.1.2** Booking creation reserves the requested dates with a booking-sourced availability block and atomically places the total amount in an `ESCROW_HOLD` against the Guest wallet.
- **F2.2.1** The Trip Hub groups Guest bookings into Pending, Upcoming, Active, Completed and Cancelled tabs and refreshes on booking, cancellation and ticket events.
- **F2.3.1** Guests can cancel pending or pre-check-in confirmed bookings; cancellation releases the booking availability block and refunds 100% for pending bookings, 100% when more than 48 hours remain, or 50% otherwise for confirmed bookings.
- **F3.1.1** Guests can file one dispute ticket for an eligible confirmed/completed booking from check-in through seven days after checkout, using an active Agent-managed category, title, description, requested remedy and optional supporting text.
- **F3.1.2** Guests can view their own ticket history, including status, requested remedy, supporting information and resolution details; ticket chat is available through Messages.
- **F3.1.3** Guests can submit one 1–5 star rating and optional review comment for a completed stay.
- **F4.1.1** Guests can view their wallet balance, held escrow and chronological statement, and can top up through the mocked wallet flow.
- **F4.2.1** Guests can withdraw up to their available non-escrowed balance through the mocked withdrawal flow.
- **F5.1.1** Hosts can create and edit listings with title, description, address, guest capacity, base rate, property type, beds, baths, check-in/out times and amenities.
- **F5.1.2** Listing validation rejects missing required text, non-positive capacity or nightly rate, negative beds/baths, rates with more than two decimal places, and invalid postal or check-in/out values before storage; service-layer ownership checks prevent editing another host's listing.
- **F5.2.1** Hosts can toggle a listing between `ACTIVE` and `INACTIVE`; listing detail shows the listing's current state and performance metrics.
- **F6.1.1** Hosts can view a monthly calendar and create inclusive single-day or date-range availability blocks for private maintenance or use; a non-blank reason is required.
- **F6.1.2** The system rejects a host block that overlaps an existing unavailable or confirmed-booking block. Hosts can remove their own manual blocks, but booking-derived blocks remain visible and cannot be removed from the calendar.
- **F7.1.1** Hosts can view pending booking requests with guest identity, requested dates, stay length, gross amount, projected net earnings and the guest's rating or “No ratings yet”.
- **F7.1.2** Hosts can approve or reject a pending request, transitioning it to `CONFIRMED` or `REJECTED`; rejection refunds the guest and approval leaves the escrow hold in place.
- **F7.2.1** The system computes host earnings as gross booking value less the 3% host fee and credits the host through the unified ledger when a booking is completed.
- **F7.2.2** Formal host response notes/evidence on dispute tickets are not implemented; Host-to-Guest and Host-to-Agent communication is provided by the messaging features below.
- **F7.3.1** The completion sweep transitions eligible `CONFIRMED` bookings to `COMPLETED` after checkout plus seven days, settles the escrow, and skips bookings whose ticket is `OPEN` or `IN_REVIEW`.
- **F8.1.1** Hosts can view their wallet balance, escrow summary and chronological statement, including payouts, fees, refunds and remedies.
- **F8.1.2** Hosts can top up their wallet through the mocked top-up flow.
- **F8.1.3** Host wallet statements show the unified ledger's money rows and directional transaction information.
- **F8.2.1** Hosts can withdraw up to their available wallet balance through the mocked withdrawal flow.
- **F9.1.1** Agents can view an oldest-first active dispute queue with guest/host evidence and ticket chat, filtered by assignment and status.
- **F9.1.2** Agents can assign a ticket to themselves, unassign it, and record internal notes.
- **F9.2.1** *Dropped.* Force Cancel / Force Complete was removed as redundant with ticket acceptance and rejection (C22).
- **F9.2.2** Agents resolve a dispute by settling the booking's full held escrow: accept the requested remedy, reject the ticket with a host payout net of the 3% fee, or apply a full-refund, full-payout or custom split adjustment. Guest refunds are fee-free and resolution completes the booking (`CONFIRMED → COMPLETED`).
- **F9.3.1** Agents can create, rename, activate/deactivate and delete ticket categories. Deletion is refused once a category is referenced by a ticket; it must be deactivated instead (C26).
- **F10.1.1** Agents can suspend Guest or Host accounts with a required reason, and reactivate them later; the reason is stored on the user and in the audit log. Agent accounts are listed but cannot be suspended.
- **F10.1.2** Suspending an account atomically force-cancels its pending and not-yet-started confirmed bookings with a full refund and deactivates its active listings; a suspended guest cannot submit a booking request (C36).
- **F11.1.1** The system provides an audit service and table and records supported state and wallet changes as typed rows. Booking decision-state and Host calendar-block coverage remain incomplete.
- **F11.1.2** Supported audit coverage includes wallet holds, refunds, payouts, remedies, overrides, top-ups, withdrawals, platform fees and ticket resolutions.
- **F11.1.3** Agents can filter the Audit Log with one search (user name or user/booking/ticket id), action types and a From/To date range.
- **F12.1.1** `MessageService` provides per-party ticket threads between the guest and assigned Agent and between the Host and assigned Agent; the Host Messages screen provides a merged inbox.
- **F12.1.2** Ticket messages persist, each party reads only its own thread, and the Agent reads both ticket threads.
- **F12.1.3** Confirmed bookings also have a private persistent Host-to-Guest conversation. It is writable through checkout plus seven days, becomes read-only afterward, and is not visible to Agents; the Guest Messages UI provides the Guest-facing inbox.

### Non-Functional Requirements

- **NFR1** Settlement is atomic: wallet balances, ledger rows, ticket, booking and audit rows commit together or not at all, and no event is published on failure.
- **NFR2** Money is `BigDecimal` at scale 2 with `HALF_UP` rounding. The platform fee is 3% of the host share, rounded once, and no fee is taken from guest money.
- **NFR3** Every mutating agent action makes an audit record; each change is one row, so a ticket resolution writes several (4.14).
- **NFR4** Layering: `ui.*` imports no `repository.*` and no `java.sql`; only `repository.jdbc.*` imports `java.sql`; `domain` imports no JavaFX or `java.sql`.
- **NFR5** Ticket and booking status changes go through `TicketStateMachine` and `BookingStateMachine`; a resolved ticket cannot be settled again.
- **NFR6** Ledger integrity: for each wallet, `balance` equals its newest `audit_log` money row's `balanceAfter`, and each row's `balanceAfter` equals the previous balance plus that row's `walletAdjustment` ([4.16](#416-unified-ledger-ledgerwriter)).
- **NFR7** Role checks run in the service layer, not only in the UI.
- **NFR8** Domain events are published only after the database transaction commits.
- **NFR9** Credentials, bearer tokens, passwords and registration codes never appear in committed files, response bodies or application logs.
- **NFR10** The backend auth tests use a token-verifier double and the frontend tests mock the Firebase client, so no test calls Firebase.
- **NFR11** Repeated or concurrent registration for one UID leaves at most one profile and one required wallet.
- **NFR12** The backend and frontend tests cover code-validation ordering, no sign-up on an invalid code, valid-code continuation, rate limiting and a modified client skipping the pre-check.
- **NFR13** The SoCLaaS key comes only from the environment and never appears in committed files, logs, error messages, results, request bodies or the audit log.
- **NFR14** An AI call is bounded: at most `deadline-ms` (45 s by default) plus its database calls, with the whole response read within the attempt timeout.
- **NFR15** The AI audit row stores metadata only (feature, model, tokens, latency, attempts, flags, prompt hash); no prompt or reply text.
- **NFR16** The backend and its health endpoint are unaffected when the AI gateway is disabled or SoCLaaS is unreachable, and no test contacts SoCLaaS.

### Known Limitations

- **Agents cannot read the private booking chat** — by decision, not as a gap (C38); it is not available as dispute evidence.
- **Category renames do not update existing tickets** — `tickets.category` stores label text (D8).
- **Suspension refund is not checked against an escrow hold** — the cascade credits the booking's `totalAmount` without reading an `ESCROW_HOLD` row, as `BookingServiceImpl` does (D20).
- **A stay checking in today counts as started** — a CONFIRMED booking with today's check-in is not cancelled by a suspension (C36). A suspended guest's in-progress stay is paid out by the normal settlement (`BookingService.completeEligibleBookings()`, run at startup, skipping bookings with an open ticket) or by an agent through a dispute ticket.
- **Ticket categories are a lookup table, not a rules engine** — revisit only if workflow branching is needed (`PROJECT_STATE.md` § Known Gaps).
- **The desktop app's authentication is mocked** — its login takes an email and checks no credential, and role separation is enforced in service code. The web app uses real Firebase authentication ([4.18](#418-authentication-and-role-authorization-backendauth-frontend-providers), C70).
- **Web registration-code validation is a validity oracle** — the public check answers `204` or `403`, so the rate limits and long random codes are the protection (`PROJECT_STATE.md` § Known Gaps).
- **Rate limits are process-local** — and the client address comes from `CF-Connecting-IP`, which is sound only while all traffic reaches Render through Cloudflare; re-check it if the hosting changes.
- **Host and Agent codes are reusable configuration values** — rotating one means changing the deployment setting.
- **Web route guards are client-side** — the backend filters are the authority.
- **Each API request checks Firebase token revocation** — request latency and availability depend on Firebase.
- **AI limiter, breaker and budget-block state is process-local** — it is lost on restart, and a Render redeploy briefly runs two instances on one shared key (C69).
- **Many accounts can still use up the shared AI quota** — guest registration needs no code (C87), so enough accounts can exhaust the non-reserved 95% of the global daily cap and the queue, and a coordinated group can open the circuit breaker (it needs failures from three different users). The per-user and global daily caps bound the damage.
- **Steered model text can pass output validation** — prose that matches no leak or link pattern is accepted, and a bare-domain link (no scheme) is not detected; role features keep AI output a suggestion the user confirms and render it as plain text (C80).
- **A daily AI cap counts calls, not upstream requests** — the worst case is `global-daily-cap × (max-retries + 1)` requests a day. While audit writes fail, sent calls are counted in memory only until 00:00 UTC.
- **Reasoning is one global setting** — `reasoning-effort` (default `none`) applies to every feature and tier; a feature that needs reasoning needs a gateway change. The 429 behaviour of SoCLaaS (`Retry-After`, budget wording) is unobserved.
- **The role pages are empty landing shells** — the domain model and services are ported ([4.19](#419-domain-model-repositories-and-services-backend-domain-layer)) but no REST controllers expose them yet; the role workstreams (W-GUEST, W-HOST, W-AGENT) will add the endpoints and pages.
- **The hosted dev backend sleeps when idle** — Render's free plan stops the service after a pause; the first request is slow and the page shows `Backend: unreachable` until `Re-check` (C85; `PROJECT_STATE.md` § Known Gaps).
- **Vercel deploys from `development`** — its Production Branch stays `development` until the production deployment work re-points it at `main` (`PROJECT_STATE.md` § Known Gaps).
- **Single-process only** — no multi-instance or distributed deployment support (`PROJECT_STATE.md` § Known Gaps).
- **Sub-second ticket ordering** — the queue orders by `createdAt` text, so tickets created in the same second can mis-order (D12 (a)).
- **Migration adoption is shallow** — `MigrationRunner` adopts any database that has a `users` table (D12 (b)).
- **Self-booking loses an update** — if guest and host are the same user, settlement reads that wallet once (D12 (c)).
- **Escrow amount is `booking.totalAmount()`** — settlement does not read it from the `ESCROW_HOLD` row (D12 (e)).
- **A load error can escape the detail screen's handler** — `DisputeDetailController.load()` calls `render()` outside the error handler (D12 (f)).
- **Audit rows within one second can misorder** — timestamps are `Instant.toString()` text with a variable fractional part, so rows in the same second can sort out of order under `ORDER BY timestamp DESC, rowid DESC` (`PROJECT_STATE.md` § Known Gaps).
- **Some audit rows are not emitted yet** — no service writes `BOOKING_CANCELLED_BY_HOST`, and W7 host calendar blocks are not audited. Booking request/decision state changes also do not yet have complete audit coverage, although their escrow money rows are recorded by `LedgerWriter`.
- **Audit Log calendar differs from the board** — the date-picker popup keeps two month/year spinners and mixed-case weekday names, and the categories header sits about 5px left of its rows while the scroll bar shows (D18).
- **Audit Log date filters accept past dates** — by design: they filter history, unlike every other date picker.
- **Wallet events share one queue** — `PostCommitEvents` is a single queue, safe only while the app is single-process and single-connection (C50).
- **Auto-complete is not a background scheduler** — the current startup and Host Requests sweeps
  call `BookingService.completeEligibleBookings()`; any future trigger must still leave a booking
  alone while its ticket is `OPEN` or `IN_REVIEW` (C17).

---

## 6. Glossary

- **CF-Connecting-IP:** the request header Cloudflare sets to the real client address; with `SNOOZESHARE_TRUST_FORWARD_HEADERS=true` the backend uses it for rate limits and never reads `X-Forwarded-For`.
- **Firebase ID token:** the signed token Firebase issues to a signed-in user; the frontend sends it as `Authorization: Bearer <token>` and the backend verifies it on each protected request.
- **`@ConditionalOnDatabase`:** the annotation (`config/ConditionalOnDatabase`) on every database-backed service, JDBC repository and AI gateway bean; it matches unless the datasource auto-configuration is excluded, so no-database test contexts start without them. It replaced `@ConditionalOnBean(PropertyRepository.class)`, which never matched at scan time (D51).
- **`ddl-auto: validate`:** the Hibernate setting that checks entity mappings against the Flyway-managed schema at startup without creating or altering tables.
- **Flyway:** the database migration tool used by `backend/`; `V001__baseline.sql` through `V005__tighten_nightly_rate_constraint.sql` are its migrations (V001 baseline, V002 auth, V003 domain tables, V004 system-user seed, V005 nightly-rate constraint).
- **Neon:** the hosted PostgreSQL service; the hosted dev backend uses its `dev` branch.
- **`open-in-view: false`:** disables Hibernate's Open Session in View pattern, so lazy loading outside a `@Transactional` boundary throws instead of silently issuing queries.
- **Registration code:** the secret a person enters to register as a Host or an Agent, set by `SNOOZESHARE_HOST_REGISTRATION_CODE` and `SNOOZESHARE_AGENT_REGISTRATION_CODE`; a Guest needs none.
- **Render:** the host for the backend's Docker image, configured by `render.yaml`.
- **Role namespace:** a URL prefix reserved for one role: `/api/guest/**`, `/api/host/**` or `/api/agent/**`.
- **Spring Data JPA:** the persistence framework used by `backend/` for the domain layer (C99); repository interfaces extend `JpaRepository` and Hibernate maps entity classes to the Flyway-managed schema.
- **Testcontainers:** the library that starts a throwaway Postgres 17 container for the backend tests.
- **Vercel:** the host for the Next.js frontend.
- **`AuthorizationService`:** a utility in `service/impl/` that checks the acting user exists, is `ACTIVE` and holds the required role; services delegate to it instead of repeating the check.
- **`AiFeature`:** a Spring bean a role registers to use the LLM gateway: its id, allowed roles, model tier, prompt, limits, output type, fallback and mock response ([4.20](#420-ai-gateway-backendai)).
- **Budget block:** the 15-minute (never past 00:00 UTC) refusal of all AI calls after SoCLaaS reports its spend budget used up; one probe call then re-tests it.
- **Circuit breaker:** stops AI calls for 30 seconds after five failed attempts from at least three different users, then lets one probe through.
- **LLM gateway:** `LlmGateway.run`, the single backend entry point for every call to the NUS SoCLaaS model ([4.20](#420-ai-gateway-backendai)).
- **PII redaction:** replacing card numbers, NRIC/FIN numbers, Singapore phone numbers and e-mail addresses with `[card]`, `[id-number]`, `[phone]` and `[email]` before text goes to the model.
- **Reasoning effort:** the `reasoning_effort` request field; `none` stops `qwen3.5:9b` and `gemma4:26b` spending `max_tokens` on hidden reasoning before they answer.
- **SoCLaaS:** the NUS School of Computing LLM service, an OpenAI-compatible API reached with a per-environment key; the only LLM provider the app may use (C80).
- **Agent (Support Agent):** the platform-admin role. Agents triage tickets, settle disputes and maintain ticket categories.
- **AGENT_OVERRIDE:** wallet transaction type written on the guest's wallet for a manual adjustment that refunds the guest.
- **Action type:** the kind of change an audit row records, one `AuditAction` constant (for example `TICKET_RESOLVED`, `ESCROW_HOLD`). The Audit Log filters by a set of them.
- **Audit row / audit log:** the `audit_log` table. One row is one change, either a status change (`beforeState` / `afterState`) or a wallet adjustment, never both.
- **Booking chat:** the private, agent-free `BookingConversationService` thread between a booking's guest and host, open from confirmation to check-out + 7 days.
- **BOOKING_PAYOUT:** wallet transaction type for money released to the host. Its `amount` is already net of the 3% fee; when the fee is positive, `LedgerWriter.postPayout` also writes a `PLATFORM_FEE` row crediting the System wallet (W14).
- **Cascade (suspension):** the bookings and listings changed in the same transaction as a suspension: force-cancelled bookings with refunds and a Host's deactivated listings.
- **Conversation row:** one entry in a Messages inbox (`ConversationSummary` or `BookingConversationSummary`): counterpart, last message, unread count, and whether it is still writable.
- **Dispute window:** the 7 days after checkout during which escrow stays held and a ticket can be opened (C17). Filing is open through check-out + 7 days; auto-completion starts at check-out + 8 days (C48, `DisputeWindowPolicy`).
- **Dual-write (historical):** until W14, a wallet movement wrote both a `wallet_transactions` row and an `audit_log` money row in one transaction (D13). W14 folded them into the one `audit_log` row and dropped `wallet_transactions` ([4.16](#416-unified-ledger-ledgerwriter)).
- **Escrow:** the booking's `totalAmount`, held out of the guest's wallet from booking until it is refunded or paid out. It is "held" while a booking has an `ESCROW_HOLD` row and no releasing row.
- **ESCROW_HOLD:** wallet transaction type that records the escrow being taken from the guest.
- **FORCE_CANCELLED:** booking status set by an agent-role transition; today only the suspension cascade produces it.
- **Guest refund (`R`):** the part of the escrow returned to the guest; never charged a fee.
- **Host share:** `escrow − R`, the gross amount owed to the host before the 3% fee.
- **Id fragment:** four or more hex characters (and dashes) typed in the audit search, matched anywhere in an entity, booking, ticket, actor or subject id.
- **IN_REVIEW:** ticket status after an agent has taken it (renamed from `UNDER_REVIEW`, C27). An agent can return it to `OPEN` by unassigning.
- **Ledger:** the money rows of `audit_log` — a row with a `walletAdjustment` and a `balanceAfter` — written through `LedgerWriter` (W14, [4.16](#416-unified-ledger-ledgerwriter)). Each wallet's `balance` is a cache of its owner's newest `balanceAfter`; before W14 the ledger was a separate `wallet_transactions` table (see Dual-write).
- **LedgerWriter:** the single class that changes a wallet balance; every money-moving service calls `post` or `postPayout` on it, inside its own transaction ([4.16](#416-unified-ledger-ledgerwriter)).
- **Manual adjustment:** an agent-chosen split of the held escrow (Full refund, Full payout or Custom), independent of the remedy the ticket asked for.
- **Mock DB:** the committed reference database `desktop/db/snoozeshare-mock.db`, built from `desktop/db/schema.sql` and `desktop/db/seed-mock-data.sql`.
- **PLATFORM_FEE:** wallet transaction type crediting the System wallet with the platform's 3% cut of a host payout; written by `LedgerWriter.postPayout` only when the fee is positive (W14).
- **Post-commit events:** domain events queued by `PostCommitEvents` during a transaction and published only after it commits (discarded on rollback), so a listener never sees a change that was rolled back ([3.1](#31-shared-foundation)).
- **Reactivation:** returning a SUSPENDED Guest or Host to ACTIVE and clearing the reason. It restores no bookings or listings.
- **Remedy:** what the ticket raiser asks for: `FULL_REFUND`, `PARTIAL_REFUND`, `HOST_PAYOUT` or `OTHER`.
- **Scrim:** the light-grey 50% overlay laid over the agent window while a modal dialog is open (C26).
- **Settlement:** dividing a booking's whole held escrow between guest and host in one atomic transaction when an agent resolves a ticket.
- **Subject user:** the user an audited change happened to (for example the guest whose wallet moved), as opposed to the actor who caused it.
- **Suspension reason:** the required text an agent enters when suspending or reactivating; stored in `users.suspensionReason` while suspended and in the audit row.
- **System user:** the seeded, non-loginable account (`AuditService.SYSTEM_ACTOR_ID`), role `SYSTEM` and status `ACTIVE` since W14, that owns system-initiated audit rows and a real wallet receiving the platform's `PLATFORM_FEE` cut. Hidden from the Accounts screen; cannot sign in, be suspended, or be governed.
- **TARGET:** the Audit Log column naming the row's one direct entity — a user's name, or `Property/Booking/Ticket/Category #last4` — dropping any related record (a ticket resolution names only the ticket, not its booking). Replaced the last-four-character `REF` column (W14 follow-up).
- **Ticket:** a dispute filed against a booking by its guest or host. It moves `OPEN → IN_REVIEW → RESOLVED_APPROVED` or `RESOLVED_REJECTED`.
- **Ticket category:** an admin-managed label (`ticket_categories`) offered to guests when they file a ticket. A ticket stores the label text.
- **Ticket chat window:** a ticket's `GUEST`/`HOST` `MessageService` threads are writable while it is `OPEN` or `IN_REVIEW`, and read-only once resolved.
- **TICKET_REMEDY:** wallet transaction type written on the guest's wallet when an agent accepts a ticket and refunds the guest.
- **W14 (unified ledger):** folded `wallet_transactions` into `audit_log` (a money row now carries `balanceAfter`) and gave the System user a real role and wallet that receives a `PLATFORM_FEE` row per payout. Built and operator-confirmed 2026-09-29 (C31, C39–C45; [4.16](#416-unified-ledger-ledgerwriter)).
- **Wallet:** a user's balance holder (`wallets`, one per user, `balance` a cache); each change is one `audit_log` money row, written by `LedgerWriter` (W14).

---

## 7. Testing

### Automated

| Command | What it does |
|---|---|
| `cd desktop; .\gradlew test` | Runs every desktop suite below (JUnit 5, TestFX on the classpath). |
| `cd desktop; .\gradlew build` | Compiles, runs checkstyle (`config/checkstyle/checkstyle.xml`), then all desktop tests. |
| `cd backend; .\gradlew test` | Runs the backend suites (JUnit 5, Spring Boot Test, Testcontainers); needs Docker. |
| `cd backend; .\gradlew build` | Compiles, runs Checkstyle (the same ruleset), then the backend tests. |
| `cd frontend; npm test` | Runs the frontend unit tests (Vitest and Testing Library). |
| `cd frontend; npm run lint` and `npm run build` | ESLint; a production build (needs `NEXT_PUBLIC_API_BASE_URL`). |
| `.\gradlew test --tests "com.snoozeshare.service.DisputeFlowEndToEndTest"` | Runs one class. A pattern such as `--tests "*DisputeSettlement*"` also works. |

The same commands run in CI (`.github/workflows/ci.yml`, see [Setting Up](#2-setting-up)). The desktop and frontend suites need no credentials or external services; the backend suites need Docker. Database-backed suites copy `desktop/db/snoozeshare-mock.db`
to a temporary directory, so the committed file is never modified. Test run times were not
measured for this guide.

| Suite | Covers |
|---|---|
| Foundation and architecture: `W1FoundationIntegrationTest`, `DatabaseBootstrapTest`, `LayerDependencyTest`, `UiDependencyTest` | SQLite bootstrap/migrations, repository and service wiring, wallet provisioning, role routing and the enforced package boundaries. |
| Unit: `SettlementCalculatorTest`, `EscrowPolicyTest`, `StateMachineTest`, `AgentCompletionTest`, `ResolutionPreviewTest`, `HeightGripTest` | Split maths and fee rounding, the escrow-held rule, every legal and illegal transition per role, live preview text, drag-height bounds. |
| Service, fakes: `TicketServiceTest`, `TicketServiceCategoryTest` | Queue order and filters, assign / unassign / notes rules, category rules including delete-when-used. |
| Messaging, mock-DB: `MessageServiceTest`, `BookingConversationServiceTest`, `MessagingMigrationTest` | Ticket and booking chat party checks, resolved/closed-window rejection, ordering, unread counts and `markRead`, `MessagePostedEvent`/`BookingMessagePostedEvent` published once per post; the `messages`/`message_reads`/`booking_messages`/`booking_message_reads` migrations against a fresh and an adopted database. |
| Mock-DB integration: `DisputeSettlementServiceTest`, `TicketServiceIntegrationTest`, `DisputeQueryServiceTest`, `JdbcTicketRepositoryTest`, `JdbcTicketCategoryRepositoryTest` | Exact ledger rows, wallet balances, ticket and booking end states, audit content, precondition rejections that leave the database unchanged, repository round trips, read models. Built on [`MockDbFixture`](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/desktop/src/test/java/com/snoozeshare/testsupport/MockDbFixture.java) with `MockIds` and `SettlementFixtures`. |
| Audit trail, unit and service: `AuditActionTest`, `AuditRecordTest`, `AuditServiceTest`, `WalletLedgerAuditTest`, `AuditLogFormattingTest` | The enum and wallet-type mapping, the one-row-one-change rule and builder, status-only rows with name snapshots, search by name (surviving a rename), id fragment, action set, inclusive dates and ordering, top-up and withdrawal money rows and rollback when the audit write fails, and the screen's status, amount and TARGET text. |
| Audit trail, database: `JdbcAuditLogRepositoryTest`, `AuditTrailMigrationTest`, `MockAuditSeedTest`, `CommittedMockDbTest` | Repository round trips and search clauses; V002 adds the columns and the System user and is idempotent; the seeded audit rows (one money row per ledger row, the four rows of ticket `#0004`, governance rows in the shape W11 will write). `CommittedMockDbTest` opens the committed `desktop/db/snoozeshare-mock.db` **directly and read-only** (not a copy, and without `MigrationRunner`) and checks it already has the audit columns, the System user and a migration version of at least 2 (now v8 after W14 — see the next row), so a stale committed file cannot hide behind a migrated copy (D15). |
| Date rules and post-commit events (MP3-0): `DisputeWindowPolicyTest`, `StayDatePolicyTest`, `DisputeWindowBoundaryTest`, `PostCommitEventsTest`, `GuestVisualsDatePickerTest`, `TicketFilingControllerTest` | The day-7 filing and day-8 completion boundary, joined across the policy, the booking service and the sweep (including a second sweep settling nothing more); past and inverted ranges refused, today allowed; events published only after commit, in order, and discarded on rollback; a typed past date reported and kept while today and future dates pass; "Other" always last in the dispute categories. |
| Unified ledger (W14): `LedgerWriterTest`, `JdbcLedgerRepositoryTest`, `LedgerConservationTest`, `MigrationForeignKeyTest`, `SystemRoleMigrationTest`, `SystemUserGuardTest`, `UnifiedLedgerMigrationTest`, `DropWalletTransactionsMigrationTest` | `LedgerWriter.post`/`postPayout` money rows, balances and the System `PLATFORM_FEE` credit; `LedgerRepository` queries; the conservation invariant (a wallet's `balance` equals its newest row, and a refund + host net + fee nets to the original escrow) against the mock DB and a real settlement; V006 rebuilding `users` with foreign keys off and restoring them; the System role and wallet; the System user guard (cannot authenticate, be suspended, or appear governable); V007's `balanceAfter` backfill and legacy `PLATFORM_FEE` rows; V008 dropping `wallet_transactions` ([4.16](#416-unified-ledger-ledgerwriter)). |
| Audit screen layout (FX toolkit): `MultiSelectMenuTest`, `AdminTableLayoutTest` | The multi-select control's behaviour; fixed table headers, shared control heights, hover without a hand cursor and the date-picker styling on the agent tables. They skip when the toolkit cannot start. |
| Account governance: `AccountGovernanceServiceTest`, `AccountGovernanceCascadeTest`, `AccountGovernanceAtomicityTest`, `SuspensionReasonMigrationTest`, `BookingServiceTest` | Validation, audit rows and events; the cascade scope (PENDING and not-yet-started CONFIRMED), refunds and listing deactivation; rollback on failure; V003; the suspended-guest booking guard |
| Accounts UI: `AccountTextTest`, `AccountSearchTest`, `SuspensionDialogFlowTest`, `AccountGovernanceUiTest` | Labels and dates, live-search matching, the modal's required reason, the screen's rows, actions and refresh |
| Atomicity: `DisputeSettlementAtomicityTest` | A forced failure after the guest row rolls back everything and publishes nothing; the connection stays usable. |
| Schema and migration: `SchemaParityTest`, `MockDbFixtureTest`, `MigrationRunnerReferenceDbTest` | `V001__foundation.sql` matches `desktop/db/schema.sql`; the mock DB opens and passes the ledger invariant; baseline adoption. |
| End to end: `DisputeFlowEndToEndTest` | `AppContext` on a mock DB copy: agent works an open ticket from queue to resolution through the real services. |
| Backend (`backend/src/test/java/com/snoozeshare`): `HealthEndpointTest`, `HealthOnKubernetesTest`, `FlywayBaselineTest`, `StartupFailureTest`, `ProfileDefaultsTest`, `ServerPortDefaultTest`, `ServerPortOverrideTest`, `ProdProfileTest`, `CorsPropertiesTest`, `CorsIntegrationTest` | Health body and status, no other actuator paths, DOWN and recovery when the database login is blocked, the baseline migration, fail-fast on an unreachable or missing database, the default profile and port, `prod` naming a missing variable, exact-origin CORS on the actuator and application endpoints. |
| Backend authentication: `AuthMigrationTest`, `FirebaseConfigTest`, `FirebaseTokenVerifierTest`, `AuthSecurityIntegrationTest`, `AuthRegistrationIntegrationTest`, `AuthContractTest`, `RegistrationCodeValidationTest`, `RegistrationRateLimiterTest`, `PublicRegistrationRateLimiterTest`, `ClientIpResolverTest` | The `users` and `wallets` constraints, blank Firebase settings refused, token and project checks, `401` and `403` per route and role, registration (one wallet for a Guest and none for an Agent, same-role retry, inactive profile, concurrent registration), stable error shapes that leak no secret, the public pre-check and its limits (a correct code cannot beat an exhausted limit), and the client address (a forged `X-Forwarded-For` is ignored). A token-verifier double replaces Firebase, so no network is needed. |
| Backend domain unit: `BookingStateMachineTest`, `TicketStateMachineTest`, `StayDatePolicyTest`, `DisputeWindowPolicyTest`, `SettlementCalculatorTest`, `DomainValidationTest` | The web-side copies of the desktop domain logic: every legal and illegal booking/ticket transition, past-date and inverted-range rejection, the day-7/day-8 boundary, escrow split maths and fee rounding, and field validation (postal codes, rates, required text). Pure unit tests, no Spring context. |
| Backend domain repository integration: `UserRepositoryTest`, `BookingRepositoryTest`, `WalletRepositoryTest`, `TicketRepositoryTest`, `AuditLogRepositoryTest` | `@DataJpaTest` with Testcontainers Postgres 17: entity round trips, custom query methods (`findByFirebaseUid`, `findByIdForUpdate`, `findByGuestIdAndStatus`), the V003 schema constraints, and the audit-log search and indexing. Each test class starts its own application context with only JPA and Flyway. |
| Backend AI gateway (`backend/src/test/java/com/snoozeshare/ai`, plus `DatabaseBeansTest`): `LlmGatewayTest`, `LlmGatewayIntegrationTest`, `AiFeatureRegistryTest`, `LlmCallTest`, `LlmPropertiesTest`, `ProdEnvironmentGuardAiTest`, `ModelCheckTest`, `InputSanitizerTest`, `PiiRedactorTest`, `InjectionDetectorTest`, `PromptBuilderTest`, `OutputValidatorTest`, `SchedulerCoreTest`, `LlmSchedulerTest`, `UserThrottleTest`, `CircuitBreakerTest`, `BudgetBlockTest`, `BackoffTest`, `SoclaasClientTest`, `SoclaasClientStallTest`, `AiAuditWriterTest`, `AiBoundaryTest`, `SecretScanTest`, `AiHealthTest`, `AiDatasourceFreeContextTest`, `AiLogLeakTest` | The 14-step pipeline with a stub SoCLaaS server, a fake clock and a recording sleeper; real audit rows and cap counts on Postgres (including a parallel burst); sanitizing, output validation, scheduling, breaker and budget logic as pure classes; configuration ranges and the production guard; the package boundary, no committed key, no key or prompt in logs, a healthy backend with an unreachable service, and `DatabaseBeansTest` for the bean registration. No test contacts SoCLaaS. |
| Frontend (`frontend/src`): `env.test.ts`, `BackendStatus.test.tsx` | Environment validation (including the six Firebase variables), and the status page for UP, each failure, the 5-second timeout, the disabled `Re-check` button and aborting on unmount. |
| Frontend authentication: `firebase.test.ts`, `api.test.ts`, `AuthProvider.test.tsx`, `route-guards.test.tsx`, the `login`, `register` and `forgot-password` `page.test.tsx` files, `AuthHeader.test.tsx`, `RoleNavbar.test.tsx`, `RoleShell.test.tsx` | Firebase initialisation once, the bearer header and stable error mapping, the six provider states (a stale profile response after sign-out is ignored, a failed registration stays on the registration step, missing Firebase configuration shows an error instead of crashing), guard redirects, the register page's validate-before-signup order, safe `returnTo` handling, and the role shell. The Firebase client is mocked. |
| Architecture: `LayerDependencyTest`, `UiDependencyTest` | `domain` has no JavaFX or `java.sql` imports; `ui` has no `com.snoozeshare.repository` or `java.sql` imports. |
| Layout: `AdminFxmlLayoutTest`, `AdminShellInitialsTest`, `ShellLayoutTest`, `AuthLayoutTest` | FXML and CSS content checks; these need no display. |
| FX smoke and flow: `AdminUiSmokeTest`, `AgentModalTest`, `ResolutionDialogFlowTest`, `CategoryDialogFlowTest` | Real JavaFX toolkit: screens render, dialogs validate and return results, the scrim is added and removed. They skip (`assumeTrue`) when the toolkit cannot start, for example on a headless machine. |
| Host UI (FX toolkit): `HostShellControllerTest`, `HostListingsControllerTest`, `HostBookingsControllerTest`, `HostCalendarControllerTest`, `HostBreadcrumbNavigationTest`, `HostPageLayoutTest`, `HostThemeOwnershipTest`, `HostCalendarVisualSnapshotTest` | Host tab routing and cleanup, listing/request/calendar rendering, breadcrumb ownership, Host stylesheet isolation, inclusive block history and the calendar snapshot. Skip when the toolkit cannot start. |
| Host Messages (FX toolkit): `ChatBubblesTest`, `HostMessagesControllerTest`, `HostMessagesLayoutTest` | Shared bubble rendering for both message types; the merged inbox orders rows, loads and sends on the correct service by row kind, disables the composer on a read-only thread, and refreshes on `MessagePostedEvent`/`BookingMessagePostedEvent`; sidebar width and ellipsis-overrun layout. Skip when the toolkit cannot start. |
| Snapshots: `AdminUiSnapshotTest`, `AdminDetailSnapshotTest`, `AdminModalSnapshotTest` | Render the agent screens at 1280x800 and write PNGs to `build/ui-snapshots/`. They never assert on pixels and skip when the toolkit cannot start. Open the images to review layout by eye. |

**Mock DB conventions to follow when writing tests or editing the seed**

- Ids are hex UUIDs (prefixes: users `a`/`b`/`c`, tickets `d`, listings `1`, bookings `2`, wallets `3`, transactions `4`, availability `5`, audit `6`, categories `7`, reviews `8`). A non-hex prefix makes `UUID.fromString` throw.
- Timestamps are UTC ISO-8601 ending in `Z`. `JdbcCodecs.instant` rejects zone-less values.
- SQLite returns money columns as `REAL`, so a `BigDecimal` can come back at scale 1 (`150.0`). Assert money with `compareTo`, never `equals`.
- Tests that match multi-line source text (CSS or FXML snippets) must read it with `testsupport/SourceText.read`, which normalises line endings; on Windows with `core.autocrlf=true` the working copy is CRLF and a raw `\n` match never succeeds.
- Do not hard-code future calendar dates in booking or search tests: past dates are now rejected, so use dates relative to today (or a fixed `Clock`, as `AvailabilityServiceTest` does).
- Rebuild `desktop/db/snoozeshare-mock.db` from `desktop/db/schema.sql` and `desktop/db/seed-mock-data.sql` after any seed edit, and keep the ledger invariant (NFR6) true.

### Manual

**Agent screens visual check.** Layout and palette are cheaper to judge by eye than to assert.

- **Prerequisites:** a copy of the mock DB and `SNOOZESHARE_DB_URL` set as in [Setting Up](#2-setting-up); a display.
- **Steps:**
  1. Run `.\gradlew run` and log in as `amy.tanaka@snoozeshare.test`.
  2. On Disputes, check the queue is oldest first with an "N unassigned" badge, and that the All / Unassigned / Mine chips and status dropdown change the rows.
  3. Open ticket `#0002`. Check the booking summary shows the held escrow and "Stay ended — escrow held", that Accept, Reject dispute and Manual adjustment are disabled, and that no Force control exists.
  4. Press Assign to me (the button becomes Unassign and the actions enable). Type in the notes box, press Save, and drag the grips to resize the chat panes and notes box.
  5. Press Accept and enter a refund of `175`: the preview should read `Guest refund SGD 175.00 | Host payout SGD 679.00 (fee SGD 21.00)`. Leave the reason empty and confirm the dialog will not submit, then enter a reason and confirm.
  6. Open the Accounts tab and type part of a name, email, role or status: the list filters live, and a short result list shows in full with no scroll bar. Suspend a Guest with upcoming bookings (a reason is required): the banner shows the display name, email and role, the row turns SUSPENDED with its reason, and Audit Log shows `ACCOUNT_SUSPENDED` with the cancellation and refund rows. Reactivate the account: the status returns to ACTIVE and no booking or listing is restored. Agent rows show a dash.
  7. Open the Categories tab: add, rename and deactivate a category, try a duplicate label, and delete one that no ticket uses.
- **Expected:** the agent screens use the canvas tab strip and "Fall Light" palette; every modal dims the window behind it with a light-grey scrim; the resolved ticket shows escrow as "Settled" and its actions are disabled; a duplicate category label shows an error; a category used by a ticket cannot be deleted; the Accounts modals show a red (Suspend) or green (Reactivate) banner and refuse an empty reason.

**Web authentication check.** A real Firebase sign-up is cheaper to judge by hand than to automate.

- **Prerequisites:** a Firebase project with Email/Password enabled; the backend running with auth on and a local Postgres, and the frontend running with the Firebase values, both as in [Setting Up](#2-setting-up); the Host and Agent codes you chose.
- **Steps:**
  1. Signed out, open `/guest`. You should land on `/login?returnTo=%2Fguest`.
  2. Open `/register`, choose GUEST and create an account. You should land on the Guest shell.
  3. Sign out. Register a HOST with a wrong code: the page says "Registration code is invalid" and the Firebase console shows no new user.
  4. Register the HOST with the right code. You should land on the Host shell.
  5. While signed in as the Host, open `/guest`. You should be sent to `/host`.
  6. Try to register an AGENT with the Host code. It should be refused.
  7. Enter wrong codes six times in a row. The page says "Too many attempts. Try again later".
  8. On `/forgot-password`, request a reset for a registered email and check the mail arrives.
- **Expected:** each role reaches only its own shell, a wrong code never creates a Firebase user, and the limiter stops repeated guesses.

**Host Portal check.**

- **Prerequisites:** as above; log in as `diego.fernandez@snoozeshare.test`.
- **Steps:**
  1. On Listings, confirm the listing cards show booking/rating metrics and that detail, edit and
     calendar navigation use the Host breadcrumbs and Host-owned Fall Light theme.
  2. Open Requests, inspect a pending request's gross/net earnings and guest rating, then exercise
     the approve and reject confirmation modals. Confirm rejection can preserve its message and
     refunds held escrow; approval leaves escrow held.
  3. Open Booking Calendar for a listing. Create a single-date or range block with a reason, confirm
     the blocked history shows the inclusive dates and reason, and remove the manual block. Confirm
     booking blocks are visible but cannot be removed.
  4. Open Wallet and confirm the shared statement, dynamic held-escrow hint, top-up presets and
     full-balance withdrawal action render inside the Host shell.
- **Expected:** switching tabs replaces the center page and cleans up the previous page's event
  subscriptions; Host pages use `host-theme.css`, not Agent styling; booking completion skips open
  or in-review tickets.

**Host Messages check.**

- **Prerequisites:** as above.
- **Steps:**
  1. Log in as `diego.fernandez@snoozeshare.test` and open the Messages tab.
  2. Confirm the inbox lists both a ticket conversation (booking #0003) and booking conversations, newest activity first, with unread rows marked.
  3. Open the ticket row, send a reply, and confirm it appears in the thread and on the Agent's dispute page for the same ticket without a reload.
  4. Open a booking row inside its chat window (bookings 4 or 14) and send a reply; open one past its window (booking 11) and confirm the composer is disabled.
- **Expected:** rows merge from both services into one list; sending updates unread state and persists across a restart; a closed booking chat and a resolved ticket both show history with no way to send; there is no control to create a new ticket.

**Audit Log real-app check (D14).** The operator drove this in the real app on 2026-09-26; repeat it after any change to the Audit Log screen.

- **Prerequisites:** as above (a copy of the mock DB, `SNOOZESHARE_DB_URL` set, a display).
- **Steps:**
  1. Run `.\gradlew run` and log in as `amy.tanaka@snoozeshare.test`; open the Audit Log tab.
  2. Type a user's name, then a booking or ticket id fragment of four or more characters, and press Apply filters: only matching rows remain.
  3. Choose two or more action types in the multi-select, and set From and To dates (the same day is allowed); Apply.
  4. Press Clear: every filter resets and all rows return.
  5. Resolve a ticket on the Disputes tab, return to Audit Log and Apply: the resolution's rows appear together, in order.
  6. Scroll the table: the header stays fixed; check the same on the Disputes and Categories tabs, and that hovering a row shows a grey background without a hand cursor.
- **Expected:** filters combine as described in [4.14](#414-audit-trail); the red Clear button resets them; the ticket resolution appears as its ticket, booking and wallet rows; table headers stay put while rows scroll.


**AI gateway live check.** Whether SoCLaaS behaves as the gateway assumes can only be judged against the real service, so repeat this after changing a model name, the base URL or a limit, and before a role feature relies on a model.

- **Prerequisites:** your environment's own SoCLaaS key in `$env:SOCLAAS_API_KEY` in the shell you use (never in a file, and never printed); `curl.exe`.
- **Steps:**
  1. List the models: `curl.exe -sS https://soclaas-api.comp.nus.edu.sg/v1/models -H "Authorization: Bearer $env:SOCLAAS_API_KEY"`. Check that the three configured names appear and are chat models.
  2. Post one chat request per model to `/v1/chat/completions` with a small `max_tokens`, then again with `"reasoning_effort":"none"` and with `"response_format":{"type":"json_object"}`. Note the status, `finish_reason` and whether `content` is empty.
  3. Read your key's limits from `https://soclaas-portal.comp.nus.edu.sg/api-key/budget` and check that `max-rpm` is below `requests_per_minute_limit`.
  4. On the Render dev service, set `SNOOZESHARE_AI_ENABLED=true`, `SOCLAAS_BASE_URL` and `SOCLAAS_API_KEY`, redeploy, and read the startup log.
- **Expected:** the models are listed; `reasoning_effort` `none` returns a short non-empty `content` with `finish_reason` `stop`; the log shows `SoCLaaS reachable: N models listed, all configured models present`, or one warning naming a missing model or the failing status. The results of the first run are in [4.20](#420-ai-gateway-backendai).

---

## 8. Acknowledgements

* [OpenJFX](https://openjfx.io/) provides the JavaFX 25 GUI modules (`javafx.controls`, `javafx.fxml`) used by the application.
* [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) (`org.xerial:sqlite-jdbc`) provides the SQLite driver.
* [JUnit 5](https://junit.org/junit5/) provides the test framework, and [TestFX](https://github.com/TestFX/TestFX) drives the JavaFX smoke tests.
* [Gradle](https://gradle.org/) and the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) provide the build and dependency-management workflow. The project also uses the [Shadow](https://gradleup.com/shadow/) plugin to create a runnable fat JAR.
* [Checkstyle](https://checkstyle.org/) enforces the source style checks run by `.\gradlew build`.
* [Spring Boot](https://spring.io/projects/spring-boot), [Spring Data JPA](https://spring.io/projects/spring-data-jpa) with [Hibernate](https://hibernate.org/), [Flyway](https://flywaydb.org/) and [Testcontainers](https://testcontainers.com/) provide the backend; [Next.js](https://nextjs.org/) and [Vitest](https://vitest.dev/) the frontend; [Render](https://render.com/), [Vercel](https://vercel.com/) and [Neon](https://neon.tech/) host the dev environment.
* [Jekyll](https://jekyllrb.com/) and [GitHub Pages](https://pages.github.com/) render the User Guide and this guide.
* [Superpowers](https://github.com/obra/superpowers) supplied the agent skills for brainstorming, planning and subagent-driven development. The copies the project uses are committed in `.claude/skills/`.
* Anthropic Claude (through Claude Code) and OpenAI Codex were the AI tools used during development. The project-specific conventions and constraints they worked under are recorded in `AGENTS.md`, and the session history is summarised in [Reflections](reflections/).
