# Branch State — W-INFRA

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-INFRA (from `development` at `e64e16c`)
- **Base:** `e64e16c`
- **Owner:** Xiao-Congchen
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-20 by Claude Sonnet 5.5 — spec drafted, reviewed round 1

---

## 1. Scope

W-INFRA: Spring Boot and Next.js skeletons in a mono-repo (`backend/`, `frontend/`), Docker Compose for
local Postgres, CI, Vercel and Railway setup (Webapp design § 9). This branch is also the trial of the
lite skills (`writing-specs-lite`, `writing-plans-lite`, `executing-plans-lite`, `lint.mjs`,
`lite-reviewer`), which is the open Upcoming todo "Trial the lite skills on the first web-app spec and
plan".

---

## 2. How to Resume

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| S1 | 2026-10-20 | Claude Sonnet 5.5 | W-INFRA | Active | Wave 1 done (T1, T2, T5); Wave 2 next | T3 step 1: backend skeleton (plan T3) | 2026-10-20 |

**Remaining**

- [x] Operator reviews the plan
- [x] Before T1: record per-class desktop test counts from `desktop/build/test-results` (F1 baseline) here
- [ ] Execute T1-T8 with `executing-plans-lite`
- [ ] **Handover:** T9, the live dev deployment, runs on `development` after the merge (needs a human; see SD10)
- [ ] **Handover:** at fold, update PROJECT_STATE § How to Resume commands, § Orientation paths, remove Q5, and note C~2 against C77 and Webapp design § 6.2
- [ ] **Handover:** lite-skills trial findings (below, § Lite-skills trial) to revise `lint.mjs`, templates and `lite-reviewer`

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-INFRA (proposed name) | Mono-repo skeletons, local Postgres, CI, deploy setup | Building | [spec](../../superpowers/specs/2026-10-20-w-infra-design.md) | [plan](../../superpowers/plans/2026-10-20-w-infra.md) | Task 3/8 next: backend skeleton | — |

---

## 7. Needs a Human

- Plan review by the operator (spec approved 2026-10-20 by Xiao-Congchen, including SD10 and the SD13 extras).
- T9 (after merge): create Railway, Vercel and Neon projects/branch, set variables (runbook order).

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-20 | Q5 resolved: the desktop code and root Gradle build move under `desktop/`; `backend/` and `frontend/` sit beside it; nothing is deleted. Independent Gradle builds (no root build); Checkstyle ruleset stays at root `config/checkstyle/` | Operator in chat; rejected: leave in place, delete now, root multi-project | [spec SD1-SD3](../../superpowers/specs/2026-10-20-w-infra-design.md) |
| C~2 | 2026-10-20 | Dev config: `application-dev.yml` is committed with non-secret local defaults; secrets go in a gitignored `application-local.yml`. Refines C77 and Webapp design § 6.2 ("secrets in gitignored dev config") | Agent; a committed no-secret file lets a fresh clone run | spec SD8 |
| C~3 | 2026-10-20 | Required checks keep names `checkstyle` and `build`, each covering all three components; backend tests use Testcontainers Postgres 17; W-INFRA includes the live dev deployment (Railway `dev`, Vercel with Production Branch `development`, Neon branch `dev`), done after merge | Operator chose these | spec SD4-SD6, SD9, SD10 |

| C~4 | 2026-10-20 | Spec amended after plan review: Checkstyle needs `configDirectory = file('../config/checkstyle')` as well as `configFile` (else `${config_loc}/suppressions.xml` resolves under the project and fails); E7 uses `ALTER ROLE ... NOLOGIN` plus terminating backends instead of pausing the container (a paused container hangs the health query); the `.gitignore` wrapper-jar negation becomes `!**/gradle/wrapper/gradle-wrapper.jar` | Plan-review findings; no behaviour or scope change | spec conventions.md, edge-cases.md; plan T1, T3 |

---

## F1 baseline

Desktop `.\gradlew cleanTest test` at the repo root before the move, run twice with identical output: 115 test classes, 581 tests, 0 failures, 0 skipped. After the move `desktop/.\gradlew build` (with Checkstyle) produced the same per-class counts. Format: `class tests failures skipped`.

```text
com.snoozeshare.app.MainDatabaseSelectionTest 3 0 0
com.snoozeshare.app.SceneRouterTest 4 0 0
com.snoozeshare.architecture.LayerDependencyTest 1 0 0
com.snoozeshare.domain.AuditActionTest 1 0 0
com.snoozeshare.domain.DomainModelTest 6 0 0
com.snoozeshare.domain.policy.DisputeWindowPolicyTest 6 0 0
com.snoozeshare.domain.policy.StayDatePolicyTest 6 0 0
com.snoozeshare.domain.settlement.EscrowPolicyTest 3 0 0
com.snoozeshare.domain.settlement.SettlementCalculatorTest 12 0 0
com.snoozeshare.domain.statemachine.AgentCompletionTest 4 0 0
com.snoozeshare.domain.statemachine.StateMachineTest 9 0 0
com.snoozeshare.foundation.W1FoundationIntegrationTest 1 0 0
com.snoozeshare.infra.db.AuditTrailMigrationTest 2 0 0
com.snoozeshare.infra.db.CommittedMockDbTest 1 0 0
com.snoozeshare.infra.db.DatabaseBootstrapTest 4 0 0
com.snoozeshare.infra.db.DropWalletTransactionsMigrationTest 1 0 0
com.snoozeshare.infra.db.MessagingMigrationTest 4 0 0
com.snoozeshare.infra.db.MigrationForeignKeyTest 1 0 0
com.snoozeshare.infra.db.MigrationRunnerReferenceDbTest 3 0 0
com.snoozeshare.infra.db.MockDbFixtureTest 2 0 0
com.snoozeshare.infra.db.SchemaParityTest 1 0 0
com.snoozeshare.infra.db.SuspensionReasonMigrationTest 1 0 0
com.snoozeshare.infra.db.SystemRoleMigrationTest 3 0 0
com.snoozeshare.infra.db.TransactionManagerTest 6 0 0
com.snoozeshare.infra.db.UnifiedLedgerMigrationTest 9 0 0
com.snoozeshare.infra.events.InProcessEventBusTest 2 0 0
com.snoozeshare.infra.events.PostCommitEventsTest 6 0 0
com.snoozeshare.repository.jdbc.JdbcAvailabilityBlockRepositoryTest 4 0 0
com.snoozeshare.repository.jdbc.JdbcBookingRepositoryFindByListingTest 1 0 0
com.snoozeshare.repository.jdbc.JdbcBookingRepositoryTest 3 0 0
com.snoozeshare.repository.jdbc.JdbcLedgerRepositoryTest 1 0 0
com.snoozeshare.repository.jdbc.JdbcPropertyRepositoryTest 9 0 0
com.snoozeshare.repository.jdbc.JdbcRepositoryIntegrationTest 2 0 0
com.snoozeshare.repository.jdbc.JdbcReviewRepositoryTest 1 0 0
com.snoozeshare.repository.jdbc.JdbcTicketCategoryRepositoryTest 5 0 0
com.snoozeshare.repository.jdbc.JdbcTicketRepositoryTest 7 0 0
com.snoozeshare.repository.jdbc.NumericPropertySchemaTest 3 0 0
com.snoozeshare.repository.JdbcAuditLogRepositoryTest 8 0 0
com.snoozeshare.repository.JdbcUserRepositoryTest 2 0 0
com.snoozeshare.service.AccountGovernanceAtomicityTest 2 0 0
com.snoozeshare.service.AccountGovernanceCascadeTest 4 0 0
com.snoozeshare.service.AccountGovernanceServiceTest 6 0 0
com.snoozeshare.service.AuditAtomicityTest 12 0 0
com.snoozeshare.service.AuditRecordTest 5 0 0
com.snoozeshare.service.AuditServiceTest 5 0 0
com.snoozeshare.service.AvailabilityServiceTest 13 0 0
com.snoozeshare.service.BookingConversationServiceTest 10 0 0
com.snoozeshare.service.BookingServiceTest 32 0 0
com.snoozeshare.service.DisputeFlowEndToEndTest 1 0 0
com.snoozeshare.service.DisputeQueryServiceTest 5 0 0
com.snoozeshare.service.DisputeSettlementAtomicityTest 4 0 0
com.snoozeshare.service.DisputeSettlementServiceTest 15 0 0
com.snoozeshare.service.DisputeWindowBoundaryTest 3 0 0
com.snoozeshare.service.FileTicketTest 14 0 0
com.snoozeshare.service.LedgerConservationTest 2 0 0
com.snoozeshare.service.LedgerWriterTest 4 0 0
com.snoozeshare.service.ListingMetricsServiceTest 4 0 0
com.snoozeshare.service.ListingServiceTest 30 0 0
com.snoozeshare.service.MessageServiceTest 10 0 0
com.snoozeshare.service.ReviewServiceTest 9 0 0
com.snoozeshare.service.SearchCriteriaTest 3 0 0
com.snoozeshare.service.SystemUserGuardTest 3 0 0
com.snoozeshare.service.TicketServiceCategoryTest 8 0 0
com.snoozeshare.service.TicketServiceIntegrationTest 5 0 0
com.snoozeshare.service.TicketServiceTest 14 0 0
com.snoozeshare.service.TransactionServiceTest 8 0 0
com.snoozeshare.service.UserServiceTest 5 0 0
com.snoozeshare.service.WalletEventPublishingTest 3 0 0
com.snoozeshare.service.WalletLedgerAuditTest 3 0 0
com.snoozeshare.service.WalletLedgerTest 4 0 0
com.snoozeshare.service.WalletTransactionAtomicityTest 1 0 0
com.snoozeshare.session.SessionContextTest 1 0 0
com.snoozeshare.testsupport.MockAuditSeedTest 4 0 0
com.snoozeshare.ui.admin.AccountGovernanceUiTest 5 0 0
com.snoozeshare.ui.admin.accounts.AccountSearchTest 4 0 0
com.snoozeshare.ui.admin.accounts.AccountTextTest 2 0 0
com.snoozeshare.ui.admin.AdminDetailSnapshotTest 3 0 0
com.snoozeshare.ui.admin.AdminFxmlLayoutTest 8 0 0
com.snoozeshare.ui.admin.AdminModalSnapshotTest 2 0 0
com.snoozeshare.ui.admin.AdminShellInitialsTest 3 0 0
com.snoozeshare.ui.admin.AdminTableLayoutTest 9 0 0
com.snoozeshare.ui.admin.AdminUiSmokeTest 6 0 0
com.snoozeshare.ui.admin.AdminUiSnapshotTest 4 0 0
com.snoozeshare.ui.admin.AgentModalTest 3 0 0
com.snoozeshare.ui.admin.audit.AuditLogFormattingTest 6 0 0
com.snoozeshare.ui.admin.CategoryDialogFlowTest 5 0 0
com.snoozeshare.ui.admin.DisputeChatPaneTest 4 0 0
com.snoozeshare.ui.admin.HeightGripTest 2 0 0
com.snoozeshare.ui.admin.MultiSelectMenuTest 4 0 0
com.snoozeshare.ui.admin.ResolutionDialogFlowTest 4 0 0
com.snoozeshare.ui.admin.ResolutionPreviewTest 3 0 0
com.snoozeshare.ui.admin.SuspensionDialogFlowTest 5 0 0
com.snoozeshare.ui.AuthLayoutTest 3 0 0
com.snoozeshare.ui.common.messaging.ChatBubblesTest 1 0 0
com.snoozeshare.ui.guest.GuestUiSnapshotTest 11 0 0
com.snoozeshare.ui.guest.GuestVisualsDatePickerTest 2 0 0
com.snoozeshare.ui.guest.tickets.TicketFilingControllerTest 2 0 0
com.snoozeshare.ui.guest.trips.TripDashboardGroupingTest 5 0 0
com.snoozeshare.ui.host.HostCalendarVisualSnapshotTest 1 0 0
com.snoozeshare.ui.host.HostConversationRowTest 1 0 0
com.snoozeshare.ui.host.HostMessagesControllerTest 2 0 0
com.snoozeshare.ui.host.HostMessagesLayoutTest 1 0 0
com.snoozeshare.ui.HostBookingsControllerTest 5 0 0
com.snoozeshare.ui.HostBreadcrumbNavigationTest 6 0 0
com.snoozeshare.ui.HostCalendarControllerTest 4 0 0
com.snoozeshare.ui.HostListingsControllerTest 25 0 0
com.snoozeshare.ui.HostPageLayoutTest 3 0 0
com.snoozeshare.ui.HostShellControllerTest 1 0 0
com.snoozeshare.ui.HostThemeOwnershipTest 2 0 0
com.snoozeshare.ui.ShellLayoutTest 5 0 0
com.snoozeshare.ui.ShellNavigationTest 7 0 0
com.snoozeshare.ui.UiDependencyTest 1 0 0
com.snoozeshare.ui.WalletActionDialogControllerTest 2 0 0
com.snoozeshare.ui.WalletDashboardControllerTest 5 0 0
com.snoozeshare.ui.WalletUiStructureTest 4 0 0
```

---

## Lite-skills trial

Observations for the Handover todo (what to revise):
- Reviewers caught real defects the author missed: actuator CORS is separate from MVC CORS; a pure rename commit cannot build; index F-numbers drifted from `requirements.md`; unscoped secret scan. Keep multi-lens review.
- `lint.mjs` does not check that the index's goal summary matches `requirements.md` IDs, nor test-layer vocabulary; consider both.
- Plan phase: `lint.mjs plan` reports "Modify path does not exist" for a file an earlier task creates (T4 modifying T3 files); it is a false positive, so either let a Modify row cite an earlier task's Create or downgrade it to a warning. It also requires the close-out heading to carry no `T` number, which the skill text (`Tn+1`) does not say.
- Plan review (one Implementer lens) found real defects the author missed: `properties()` is the lowest Spring precedence so an E10 test would connect to a local DB; a copied `gradlew` loses its exec bit; the root `.gitignore` hides the new wrapper jar and `.env.example`; Gradle 9 needs `junit-platform-launcher`. One reviewer was enough at the plan stage.
- Execution: after a task that moves files, `lint.mjs plan` fails its own Modify rows (the old paths no longer exist), so lint is only meaningful before execution or with executed tasks skipped; consider a `--executed T1` flag or ignoring Done tasks.
- Spec and plan tooling friction: very long inline shell heredocs failed twice; write scripts to files instead.
- T5 observations: `npm audit` reports 7 vulnerabilities (eslint-config-next/braces, postcss) with Next 15.5.28 already the latest 15.x; not fixed, revisit in W-SECURITY/W-DEPLOY. Vitest crashed once on Windows (worker exit 3221226505) and passed on two reruns; treat as transient.
- Four parallel reviewers on a Medium spec produced heavily overlapping findings; two lenses (Implementer + Tester) would have caught most.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-20 | Created the W-INFRA branch and its branch state | W-INFRA | `8f80e0a` |
| 2026-10-20 | W-INFRA design spec (index + 7 component files) written, linted, reviewed by four lenses | W-INFRA | `73ab826` |
| 2026-10-20 | Moved the desktop app under `desktop/` (renames at 100% except `build.gradle`: Checkstyle now reads root `config/checkstyle`); `.gitignore` and `ci.yml` adjusted; desktop build green with per-class test counts equal to the baseline; no desktop test reads `docs/` or `config/` | W-INFRA | this commit |
| 2026-10-20 | Added `docker-compose.yml` (Postgres 17, healthcheck, `POSTGRES_PORT`); verified healthy, `select 1`, port override | W-INFRA | this commit |
| 2026-10-20 | Spec approved by Xiao-Congchen; W-INFRA implementation plan (T1-T9 + close-out) written, linted, reviewed; spec amended (C~4) | W-INFRA | — |
| 2026-10-20 | Added `frontend/` (Next.js 15.5.28, React 19.1.0, Tailwind 4, shadcn CLI 4.22.1 Button, Vitest 5, Node 22 pinned via `.nvmrc`, `.npmrc`, `engines`): `env.ts` validation, `BackendStatus` page; 15 tests, lint and build verified, `npm ci` works | W-INFRA | this commit |
