# Branch State — W-AGENT

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AGENT (from `development` at `7eef36a`)
- **Base:** `e185dc0` (development tip merged into the branch on 2026-10-22; the branch was cut at `7eef36a`)
- **Owner:** Xiao-Congchen
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-22 by Codex — T1 complete, paused at operator request

---

**Scope.** The Support Agent role's feature set on the web: REST controllers under `/api/agent/**` (disputes with notes and threads, accounts, audit log, ticket categories) over the ported W-DOMAIN services, the shared REST foundation Guest and Host will reuse, and the Next.js pages under `frontend/src/app/agent`. Serves the W-AGENT workstream. The agent AI feature is W-AI-AGENT and is out of scope here.

---

## 1. Orientation

No change.

---

## 2. How to Resume

**Setup** — same as `PROJECT_STATE.md`, no additional steps.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| S2 | 2026-10-22 | Claude Sonnet 5.5 | W-AGENT | Active | T7 done; T2 next | T2 step 1 in `backend/src/test/java/com/snoozeshare/api/controller/agent/AgentDisputeConcurrencyTest.java`: write the failing E20 and E10 tests; commits stay under 150 changed lines | 2026-10-22 |

**Remaining**

- [x] Confirm scope with the operator (done 2026-10-22)
- [x] Gate 1: spec reviewed in two rounds, approved by Xiao-Congchen 2026-10-22 (Q~1 answered: Asia/Singapore)
- [x] Gate 2: plan approved by Xiao-Congchen 2026-10-22
- [x] Execute the plan (`executing-plans-lite`); T1 complete
- [ ] **Handover:** the W-AI-GATEWAY branch state is still in `branch-states/` on `development`: its fold (gate 4) is pending and is not this branch's work
- [ ] **Handover:** audit reasons, notes and messages typed by agents will become LLM input in W-AI-AGENT; treat them as untrusted (prompt injection) in that spec

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AGENT | Agent features: disputes, accounts, audit, categories — REST controllers and Next.js pages | Building | [spec](docs/superpowers/specs/2026-10-22-w-agent-design.md) | [plan](docs/superpowers/plans/2026-10-22-w-agent.md) | T1, T7 Done; T2 step 1 next | — |

---

## 4. Architecture

### Shared agent REST foundation

**Now:** `com.snoozeshare.api` provides the shared `{code, message, requestId}` error body and safe exception mapping; unmatched `/api/agent/**` requests use a servlet exception resolver; `@CurrentUserId` reads the token-derived actor attribute set by `RoleAuthorizationFilter`; decoded-path filters enforce role, no-store and request-size rules; `RowLocks` applies PostgreSQL's transaction-local five-second lock timeout. `ApiLayerTest` pins controller boundaries and proves each rule with deliberate test fixtures.

| ID | Decision |
|---|---|
| C~3 | The agent REST layer owns the shared API foundation because Guest and Host routes do not yet exist |

---

## 5. Conventions

| Rule | Current convention |
|---|---|
| API controllers | Live under `com.snoozeshare.api.controller.<role>`; records stay in `api.request`/`api.response`, conversions in `api.mapper` |
| Layer boundary | Controllers do not depend on repositories, domain models, persistence types, money services or the audit write service; audit reads use `AuditQueryService` |
| Actor identity | Controller actor ids use `@CurrentUserId`, resolved from the authenticated request attribute |
| Locking | Ticket and governed-user mutations take `RowLocks.timeout()` and then lock the target row before reading it |
| Commit size | Fewer than 150 changed lines per commit, counting additions plus deletions including tests and docs |

---

## 6. Known Gaps

| Gap | Accepted limitation |
|---|---|
| Queue and account lists | Unpaginated |
| Audit paging | Offset paging can repeat or skip rows when new entries arrive between pages |
| Agent API | No per-agent rate limit |
| Missing related records | Missing Booking, Property or Guest wallet still map to 400 |

---

## 7. Needs a Human

No change: Q~1 (audit zone) was answered on 2026-10-22 (C~7); nothing is open.

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-22 | W-AGENT covers all four agent areas (disputes with threads and notes, accounts, audit, categories), API and pages in one workstream, no AI | Operator confirmed the proposed scope | Operator conversation |
| C~11 | 2026-10-22 | Skip repeated per-commit build and test runs for this history-only split; verify changed-line budgets and final implementation parity | Operator: "skip the per commit checks since we are just splitting them"; the completed T1 backend suite was already green | Operator conversation |
| C~10 | 2026-10-22 | Keep every commit under 150 changed lines; split the local T1 history and retain the final implementation | Operator found the T1 commits too large to review | Operator conversation |
| C~2 | 2026-10-22 | Where the desktop screens and the design artifact differ, the desktop behaviour wins: keep C22 (no force actions), Manual adjustment is one guest refund, audit filters are Search / Action / From / To / Load more, Accounts lists Guests and Hosts only, categories keep Delete, amounts in SGD | Operator answered each difference, 2026-10-22 | Spec SD2 to SD5 |
| C~4 | 2026-10-22 | Tickets are locked pessimistically in assign, notes, unassign and settlement so an escrow cannot be settled twice. Rejected: `@Version`, SERIALIZABLE, leaving the race | Review (Tester, Attacker, Maintainer) found no lock in the ported services | Spec SD10 |
| C~6 | 2026-10-22 | Round 2 review: the ticket lock is the first read of resolve; suspend and reactivate lock the target user; audit reads use `AuditQueryService`; the error handler passes on messages only from service and domain code; request size is checked after authentication | Review (Attacker, Implementer, Tester) found stale-read, cascade-race, layering and message-leak gaps | Spec SD10, SD19, SD20, SD21 |
| C~7 | 2026-10-22 | The audit-log date range uses a fixed `Asia/Singapore` zone by default (`snoozeshare.audit.zone`), not the server zone. Rejected: server default (UTC on Render) | Operator: "Use Singapore timezone as default"; resolves Q~1 | Spec SD16 |
| C~8 | 2026-10-22 | Spec amended while writing the plan: T5 also edits `AuditLogSearchRepositoryImpl` so `%` and `_` in the audit text search are literal (E82 needs it). No behaviour or task boundary changes | Plan writing found the LIKE patterns unescaped | Spec api.md, implementation-order.md |
| C~9 | 2026-10-22 | Plan-level refinements recorded as spec amendments: filters use `UrlPathHelper.getPathWithinApplication` (works under MockMvc, unlike `getServletPath`); the 5 s lock timeout is `SET LOCAL lock_timeout` in a `RowLocks` helper (the PostgreSQL dialect ignores the JPA hint); the audit `LIKE` escape character is `!` (a backslash is risky in HQL); `AuditServiceImpl` keeps its three-argument constructor | Plan review and code reading; no behaviour or task boundary changes | Spec SD10, SD11, conventions.md |
| C~5 | 2026-10-22 | `agentNotes` moves from the shared `TicketResponse` to the agent-only `DisputeDetailResponse`; request size is capped by a filter on `/api/agent/**` rather than a global Jackson limit | Review (Implementer, Attacker) | Spec SD13, SD14 |

---

## 9. Deviations

| ID | What | Consequence |
|---|---|---|
| D~1 | The spec edits W-DOMAIN services (`TicketServiceImpl`, `DisputeSettlementServiceImpl`, `DisputeQueryService`, `AccountGovernanceService`, `AuditServiceImpl`, not-found throws) and the shared `TicketResponse` | Recorded now so the plan and review expect changes outside the new API packages; the repo's earlier tests must still pass |
| D~2 | The plan assumed ticket categories were seeded, but migrations V001-V005 contain no category seed | Test reset removes all categories; scenario fixtures create them explicitly so assertions remain isolated |
| D~3 | `UrlPathHelper.getPathWithinApplication` drops the `/api` segment on a request URI beginning `//api/...` | `ProtectedRequestPath` collapses repeated slashes in the raw URI first, then uses UrlPathHelper for decoding and matrix-parameter removal; E62 covers it |
| D~4 | The E69 missing-actor unit test was added after the resolver types because they did not yet exist; E4 reuses the existing Jackson unknown-property setting | The actor route did run red before filter/resolver changes; E69 now directly verifies fail-closed resolution |
| D~5 | Spring's standard MVC exception advice did not render JSON for unmatched paths (the `NoHandlerFoundException` was left with an empty 404 in MockMvc) | `ApiNoRouteExceptionResolver` handles only unmatched `/api/agent/**` paths before default resolution; other namespaces keep their prior behavior |
| D~6 | Original T1 commits exceeded the operator's later commit-size cap | Rewritten as 23 commits, at most 133 changed lines each; original history is retained in backup refs and concurrent T7 commits are preserved |

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-22 | Branch W-AGENT cut from `development` and its branch state created | W-AGENT | `7823c4b` |
| 2026-10-22 | Spec for the agent REST API and pages drafted, linted, reviewed in two rounds and approved | W-AGENT | `ecfc451` |
| 2026-10-22 | Plan for the agent REST API and pages written, linted and reviewed once; spec text amended for plan refinements (C~8, C~9) | W-AGENT | next commit |
| 2026-10-22 | Plan approved by Xiao-Congchen; T1 started | W-AGENT | this commit |
| 2026-10-22 | T1 Slice A adds shared API fixtures and ArchUnit dependency; baseline and auth regression pass | W-AGENT | T1 Slice A |
| 2026-10-22 | T1 Slice B adds typed not-found failures for tickets, threads, categories and governed users | W-AGENT | T1 Slice B |
| 2026-10-22 | T1 Slice C guards decoded agent paths, applies no-store and caps request bodies; full backend tests pass | W-AGENT | T1 Slice C |
| 2026-10-22 | T1 Slice D adds token-derived actor resolution and fail-closed missing-actor handling; backend suite passes | W-AGENT | T1 Slice D |
| 2026-10-22 | T1 Slice E adds consistent API errors and safe logging; `RowLocks` and ArchUnit boundary rules; full backend Checkstyle and tests pass | W-AGENT | T1 complete |
| 2026-10-22 | Recorded the operator's commit cap in AGENTS.md; T1 history split finished; per-commit checks waived by the operator; line budgets and implementation parity verified; concurrent T7 commits preserved | W-AGENT | commit organization |
| 2026-10-22 | `development` merged into the branch (W-AI-GATEWAY folded; no conflict with the spec) | W-AGENT | merge commit |
| 2026-10-22 | T7 frontend foundation: `apiJson` and `isSessionFailure`, `usePolling`, `SessionNotice`, `AgentDialog`, agent navbar links without the wallet pill, `/agent` redirect, API types and formatting; 101 frontend tests, lint and build pass | W-AGENT | T7 commits |
| 2026-10-22 | Plan-only fix: the frontend build command needs the CI placeholder Firebase variables | W-AGENT | T7 close |
