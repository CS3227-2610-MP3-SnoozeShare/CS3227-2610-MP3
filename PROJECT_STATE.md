# Project State — SnoozeShare

**Living document — the source of truth for this project.** Read it in full before doing
anything, then run `git log --oneline -20` to confirm it still matches reality. On `main` it
changes only through `sync-project-state` (or an owner hotfix); on a branch, read it and write
the branch state instead.

- **Iteration:** 2 — iteration 1 (W1–W16 and the MP3-0 fixes) is closed
- **History:** [docs/project-history/](docs/project-history/README.md) — archived iteration 1; not read at session start, open it only to answer "why was X decided?"
- **Phase:** MP3-0 finishing (CI protection); no new feature workstream has started
- **Stack:** Java 25, JavaFX 25 (javafx.controls, javafx.fxml), Gradle (application + shadow + checkstyle plugins), SQLite (embedded, `org.xerial:sqlite-jdbc`) via plain JDBC, JUnit 5 + TestFX
- **Branch:** `main` — this file describes `main` only; branches record their changes in `docs/project-state/branch-states/` and `sync-project-state` folds them in
- **Method:** TDD-first vertical slices; spec then plan for big changes (`AGENTS.md` § 3); small commits (`AGENTS.md` § 6)
- **Last updated:** 2026-10-19 by Claude Sonnet 5.5 — refreshed into iteration 2 (archive: iteration 1)
- **Last verified against repo:** 2026-10-19
- **Developer guide:** `docs/DeveloperGuide.md` documents W1–W16 and the MP3-0 fixes (last full update 2026-10-19). Residual limitations stay in the guide's Known Limitations and § Known Gaps.

Sections are ordered by how often they are needed: **1–3 say where we are, 4–5 say what the
system is, 6–7 say what not to touch and what is stuck, 8–10 are the record.** Cite sections by
name (`§ Known Gaps`), not by number.

---

## 1. Orientation

SnoozeShare is a Java 25 / JavaFX desktop application for short-term property rentals (CS3227
Mini Project 3, continuing MP2). Three roles — **Guest**, **Host**, **Support Agent** — each have a
role-isolated UI over one domain model, one wallet/ledger, mocked authentication and shared
booking/ticket state machines. It runs as a single process on an embedded SQLite database; there is
no server.

| Path | What lives there |
|---|---|
| `docs/superpowers/specs/`, `docs/superpowers/plans/` | Design specs and plans, one pair per feature-level workstream |
| `docs/project-state/done-ledger.md` | The Done ledger for this iteration — every change, newest first |
| `docs/project-state/branch-states/` | One lean state file per in-flight branch; `archive/` holds folded ones |
| `docs/project-history/` | Closed iterations (state, ledger, branch states, `REFRESH.md`). Not the source of truth |
| `docs/DeveloperGuide.md`, `docs/UserGuide.md` | Handover brief for developers; end-user walkthroughs (Jekyll site, published by `jekyll-gh-pages.yml`) |
| `docs/SnoozeShare-Architecture-Proposal.md`, `docs/ProductBacklog.md` | Original design rationale and the F0–F12 backlog |
| `docs/MP2-feedback-triage.md` | MP2 grade-report triage that produced MP3-0 |
| `src/main/java/com/snoozeshare/` | Source: `domain`, `repository`, `service`, `infra`, `session`, `ui.{common,guest,host,admin}`, `app`, `config` |
| `config/checkstyle/`, `.github/workflows/` | Checkstyle rules; `ci.yml` (Checkstyle every push, build + tests on PRs and `main`) and the Pages workflow |
| `db/schema.sql`, `db/seed-mock-data.sql`, `db/snoozeshare-mock.db` | Shared reference DB, committed. **Rebuild the `.db` from the two SQL files after any seed edit.** Seed timestamps are UTC ISO-8601 ending in `Z`; seed ids are hex UUIDs (prefixes: users `a`/`b`/`c`, tickets `d`, listings `1`, bookings `2`, wallets `3`, transactions `4`, availability `5`, audit `6`, categories `7`, reviews `8`). The `.db` ships migrated (schema_history through v8); `CommittedMockDbTest` guards that. Running the app with no `SNOOZESHARE_DB_URL` opens and **writes** it, so run against a copy |
| `logs/` | Agent session logs `YYYY-MM-DD_HH-mm-ss_<branch>.md` (SGT) |

---

## 2. How to Resume

```bash
.\gradlew build    # compile, Checkstyle, all tests
.\gradlew test
.\gradlew run      # on a copy: set SNOOZESHARE_DB_URL=jdbc:sqlite:build/acceptance.db
```

**Upcoming** — planned workstreams and the todos handed over at each sync, in priority order.
**The first row is the next action.** Remove a row when it is dropped, or when it starts and
becomes a Workstream row. Sessions and in-flight branches are not tracked here: each branch keeps
its own resume point in `docs/project-state/branch-states/`.

| Item | Kind | Context / done when | Source | Added |
|---|---|---|---|---|
| Require the CI checks on `main` (P1) | Todo | `ci.yml` ran green on GitHub (2026-10-19). Done when the `checkstyle` and `build` checks are required by branch protection on `main` (a repository setting); then MP3-0 can be marked `Done` | handed over from MP3-0 | 2026-10-19 |
| New MP3 feature workstream(s) | Workstream | None scoped yet. Done when the operator names one and it has an approved spec and plan (`brainstorming`, `writing-plans`) | iteration 1 close | 2026-10-19 |

---

## 3. Workstreams

W1–W16 (auth through guest UI alignment) are `Done` and documented in `docs/DeveloperGuide.md`; their
rows are in the [iteration 1 archive](docs/project-history/iteration-1/PROJECT_STATE.md). Only open work is listed here.

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| MP3-0 | MP2 feedback fixes before new MP3 features (bugs B1–B7, design D1–D6, CI/skills/docs) | Building | [correctness-cluster design](docs/superpowers/specs/2026-10-18-mp3-correctness-cluster-design.md) | [correctness-cluster plan](docs/superpowers/plans/2026-10-18-mp3-correctness-cluster.md) | Merged (PRs #1–#3); CI and Pages verified; open: required checks (P1, Upcoming) | Documented 2026-10-19 |

Status vocabulary, used verbatim: `Not started` · `Spec'd` · `Planned` · `Building` ·
`Blocked — needs human` · `In review` · `Done` · `Abandoned`. Guide vocabulary (owned by
`update-documentation`): `—` · `Awaiting confirmation` · `Pending` · `Documented YYYY-MM-DD` · `N/A`.
**Progress is a position, not a diary** (~15 words).

---

## 4. Architecture

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

---

## 5. Conventions

Highest IDs used before this iteration: **W16, C59, D38, Q3**. Numbering continues; never reuse. `C11, C28–C31, C37, C39–C41` each have two meanings in the archive; cite the archive section when it matters.

- `ui.*` never imports `repository.*` or `infra.db.*` — only `service.*`. Only `repository.jdbc.*` imports `java.sql.*`.
- `BookingService`/`TicketService` are the only callers of `TransactionService`; UI never calls it, and never calls `WalletService.topUp/withdraw` inside a booking/ticket flow.
- All state transitions go through the shared `*StateMachine`.
- One audit row per change, written by services on the caller's connection through `AuditRecord.builder`.
- Only `LedgerWriter` changes a wallet balance; it never opens a transaction. `wallets.balance` caches the newest `balanceAfter`.
- Wallet events are queued by `LedgerWriter`; never publish `WalletTransactionRecordedEvent` by hand.
- Dispute-window and stay-date rules live only in `domain.policy`.
- Tests that match multi-line source text read it with `testsupport/SourceText.read` (CRLF working copies); tests never hard-code future dates (past dates are rejected).
- Keep commits small and each one green (`AGENTS.md` § 6).
- **IDs:** `W` workstream, `C` decision, `D` deviation, `Q` question. Allocated only by `sync-project-state`; a branch state uses provisional `~` IDs and never cites one outside itself.
- **Branches never edit this file.** Each writes `docs/project-state/branch-states/<slug>.md`; the owner approves it and runs `sync-project-state` after the merge. Do not merge a branch that is not `Approved` or has live session rows.
- **Iterations:** the owner runs `refresh-project-state` by hand, guide first. Agents never start one and never read `docs/project-history/` at session start.
- Agent interaction logs under `logs/` are append-only, one file per session; the logging skill is invoked manually at session end.

---

## 6. Known Gaps & Accepted Limitations

**These are decisions, not a TODO list — do not "helpfully" implement them.**

- **No multi-instance / distributed deployment** — single-process desktop app; `PostCommitEvents` is one shared queue by that assumption (C50).
- **Ticket categories are a lookup table, not a rules engine** — revisit only if workflow branching is needed.
- **Booking overlap-check + escrow hold use a `SERIALIZABLE` transaction**, not optimistic locking or a queue.
- **Top-up/withdraw are mocked** — no payment gateway; one would slot in behind `WalletService`.
- **Auth is fully mocked** — no credential validation; role separation is in service code. A stated project constraint.
- **Audit timestamps are `Instant.toString()` text**, so rows in the same second can misorder under `ORDER BY timestamp DESC`. Do not "fix" without a decision.
- **Audit Log UI polish left open** — the `REF`/`TARGET` column can truncate when a row has both a booking and a ticket.

**Explicitly out of scope:** microservices, message brokers, horizontal scaling, JPMS module boundaries, JPA/Hibernate, a separate DTO layer, agent Force Cancel/Complete (C22).

---

## 7. Needs a Human

| ID | What is needed | What it blocks | Raised |
|---|---|---|---|
| — | No outstanding questions | — | — |

---

## 8. Decisions & Context

Standing process decisions. Architecture-shaping ones sit with their area in § Architecture.

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C54 | 2026-10-19 | Branch-state workflow: this file describes `main` only and changes via `sync-project-state`; branches write `docs/project-state/branch-states/<slug>.md` | Operator adopted the universal skills; the old shared-file workflow collided on IDs | Operator conversation |
| C55 | 2026-10-19 | `refresh-project-state` closes an iteration; run by the owner, guide first, nothing deleted, IDs continue | Operator added the skill | `.claude/skills/refresh-project-state/SKILL.md` |
| C56 | 2026-10-19 | The docs site is published by `.github/workflows/jekyll-gh-pages.yml` with `source: ./docs` (Pages source = GitHub Actions); supersedes the branch/folder deploy of C52 | Operator followed GitHub's recommendation; the template's `./` would have published `AGENTS.md` and this file | Operator conversation |
| C59 | 2026-10-19 | CI: Checkstyle on every push; full build + tests on every PR and on `main`; merging requires both once branch protection is set. Rejected: full build on every branch push; building only on `main` | Operator: Checkstyle for all pushes, build and test for all PRs at least | `.github/workflows/ci.yml` |

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
- **D38** — W16 traps: `.agent-root`/`.host-root` overrides outrank `wallet.css` (the shared card also changes Host Wallet); pending trips sit under Upcoming; empty tables use `EmptyTableRow`; the ticket status pill is a fixed 116×36; Messages keep the 32px inset pinned by `HostPageLayoutTest`.

---

## 10. Record

The Done ledger lives in **[`docs/project-state/done-ledger.md`](docs/project-state/done-ledger.md)** — every change, big or small, newest first.

- **Latest entry:** 2026-10-19
- **Entries:** 1 (iteration 2 started; earlier entries in the archive)
