# Branch State — W-AGENT

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AGENT (from `development` at `7eef36a`)
- **Base:** `e185dc0` (development tip merged into the branch on 2026-10-22; the branch was cut at `7eef36a`)
- **Owner:** Xiao-Congchen
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-22 by Claude Sonnet 5.5 — spec approved, development merged

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
| S1 | 2026-10-22 | Codex | W-AGENT | Active | Rebuild T1 commit history under the operator cap | Verify each small snapshot; pause again before T7 | 2026-10-22 |

**Remaining**

- [x] Confirm scope with the operator (done 2026-10-22)
- [x] Gate 1: spec reviewed in two rounds, approved by Xiao-Congchen 2026-10-22 (Q~1 answered: Asia/Singapore)
- [x] Gate 2: plan approved by Xiao-Congchen 2026-10-22
- [ ] Execute the plan (`executing-plans-lite`); T1 in progress
- [ ] **Handover:** the W-AI-GATEWAY branch state is still in `branch-states/` on `development`: its fold (gate 4) is pending and is not this branch's work
- [ ] **Handover:** audit reasons, notes and messages typed by agents will become LLM input in W-AI-AGENT; treat them as untrusted (prompt injection) in that spec

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AGENT | Agent features: disputes, accounts, audit, categories — REST controllers and Next.js pages | Building | [spec](docs/superpowers/specs/2026-10-22-w-agent-design.md) | [plan](docs/superpowers/plans/2026-10-22-w-agent.md) | T1 history split in flight; final implementation preserved in backup | — |

---

## 4. Architecture

No change: nothing built yet.

---

## 5. Conventions

No change: nothing built yet. (The spec proposes web layering rules, ArchUnit `ApiLayerTest`, `@CurrentUserId` and ticket row locks; they land here when built.)

---

## 6. Known Gaps

No change: nothing built yet. (To add when built: queue and accounts lists are unpaginated; offset paging of the audit log can repeat or skip rows while new entries arrive; no per-agent rate limit; `Booking`/`Property`/`Guest wallet does not exist` map to 400.)

---

## 7. Needs a Human

No change: Q~1 (audit zone) was answered on 2026-10-22 (C~7); nothing is open.

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~10 | 2026-10-22 | Keep every commit under 150 changed lines; split the local T1 history and retain the final implementation | Operator found the T1 commits too large to review | Operator conversation |
| C~11 | 2026-10-22 | Skip repeated per-commit build and test runs for this history-only split; verify changed-line budgets and final implementation parity | Operator: "skip the per commit checks since we are just splitting them"; the completed T1 backend suite was already green | Operator conversation |
| C~1 | 2026-10-22 | W-AGENT covers all four agent areas (disputes with threads and notes, accounts, audit, categories), API and pages in one workstream, no AI | Operator confirmed the proposed scope | Operator conversation |
| C~2 | 2026-10-22 | Where the desktop screens and the design artifact differ, the desktop behaviour wins: keep C22 (no force actions), Manual adjustment is one guest refund, audit filters are Search / Action / From / To / Load more, Accounts lists Guests and Hosts only, categories keep Delete, amounts in SGD | Operator answered each difference, 2026-10-22 | Spec SD2 to SD5 |
| C~3 | 2026-10-22 | W-AGENT builds the shared REST foundation (global error handler, `@CurrentUserId`, ArchUnit layering test, typed frontend helpers) because no controllers or backend exist elsewhere | `origin/W-HOST` is fixture-only frontend and has no open pull request | Spec SD1, SD12 |
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

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-22 | Branch W-AGENT cut from `development` and its branch state created | W-AGENT | `7823c4b` |
| 2026-10-22 | Spec for the agent REST API and pages drafted, linted, reviewed in two rounds and approved | W-AGENT | `ecfc451` |
| 2026-10-22 | Plan for the agent REST API and pages written, linted and reviewed once; spec text amended for plan refinements (C~8, C~9) | W-AGENT | next commit |
| 2026-10-22 | Plan approved by Xiao-Congchen; T1 started | W-AGENT | this commit |
| 2026-10-22 | `development` merged into the branch (W-AI-GATEWAY folded; no conflict with the spec) | W-AGENT | merge commit |
| 2026-10-22 | Rebuilt T1 part 1/20: distinguish missing agent resources from invalid requests | W-AGENT | small-commit reconstruction |
| 2026-10-22 | Rebuilt T1 part 2/20: normalize protected API paths and disable agent response caching | W-AGENT | small-commit reconstruction |
| 2026-10-22 | Rebuilt T1 part 3/20: resolve actor UUIDs from authenticated request attributes | W-AGENT | small-commit reconstruction |
