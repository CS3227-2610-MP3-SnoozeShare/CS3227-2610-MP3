# Branch State — W-AI-GUEST

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AI-GUEST (from `development` at `f50f437`)
- **Base:** f50f437
- **Owner:** Nathan
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-23 by Claude Opus 4.6 — plan approved

---

**Scope.** AI Search Assistant for the Guest role: a new endpoint that uses the LLM gateway (FAST tier) to extract structured search filters from a natural language query and run the existing search. Frontend AI search bar on the Guest search page. Serves workstream W-AI-GUEST.

---

## 1. Orientation

No change.

---

## 2. How to Resume

**Setup** — same as `PROJECT_STATE.md` § How to Resume.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| S1 | 2026-10-23 16:00 | Claude Opus 4.6 (lead) | W-AI-GUEST | Active | T5 done; close-out next | Close-out: lint, build, code review | 2026-10-23 |

**Remaining**

- [x] Write the implementation plan (`writing-plans-lite`)
- [x] Implement T1-T5 (`executing-plans-lite`)
- [ ] Close-out and review

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AI-GUEST | AI Search Assistant — Guest natural language search via LLM | Building | [spec](docs/superpowers/specs/2026-10-23-w-ai-guest-design.md) | [plan](docs/superpowers/plans/2026-10-23-w-ai-guest.md) | T5 done; close-out next; guide out of date: AI search assistant | — |

---

## 4. Architecture

No change: this branch adds a new endpoint and an `AiFeature` bean on top of the existing AI gateway (§ 4.11) and Guest controllers (§ 4.14). The architecture area will be written when the code is built.

---

## 5. Conventions

No change: the spec applies existing conventions (gateway calling rules, `@ConditionalOnDatabase`, `ResolvedUser`, DTO placement) and introduces only feature-local rules documented in the spec's conventions.md.

---

## 6. Known Gaps

No change: no new gaps introduced. The spec explicitly cites and respects the existing Known Gaps for steered prose, reasoning per feature, and 429 behaviour.

---

## 7. Needs a Human

No change.

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-23 | Single-phase AI search: LLM extracts filters only, no second call to rank or explain individual results. No listing descriptions in LLM context | Avoids indirect injection surface from host-authored content, halves token cost. Rejected: two-phase search+rank | Operator chose Option A |
| C~2 | 2026-10-23 | FAST model tier for the guest search feature | Operator confirmed; model name changeable via `SNOOZESHARE_AI_MODEL_FAST` env var | Operator |
| C~3 | 2026-10-23 | Risk tier Medium with Implementer, Tester, and Attacker lenses | Untrusted user input to LLM but no money, permissions, or schema changes. Rejected: High | Spec author |
| C~4 | 2026-10-23 | AI search controller catches `IllegalArgumentException` from search layer (LLM-extracted invalid dates) and returns degraded 200 with `INVALID_SEARCH_DATES`, not a 400 | The AI made the date mistake, not the user. Rejected: propagating the 400 | Tester review |
| C~5 | 2026-10-23 | `OutputValidator` uses `FAIL_ON_UNKNOWN_PROPERTIES`; extra JSON fields from LLM trigger fallback | Verified in code; strict validation. E12 corrected from draft | Attacker review |
| C~6 | 2026-10-23 | Flagged status not exposed to the client; WARN-logging is the full "acts on flagged" for this feature | No oracle for injection probing. Rejected: adding a `flagged` response field | Attacker review |
| C~7 | 2026-10-23 | Error codes from `LlmErrorCode` exposed in degraded response | Minor info disclosure accepted for debugging value. Rejected: collapsing to generic code | Attacker review |

---

## 9. Deviations

| ID | What | Consequence | Action |
|---|---|---|---|
| D~1 | `GuestWalletControllerTest` fails (10 tests) in the full suite but passes in isolation — pre-existing Testcontainers context-cache issue, present on the base commit too | No impact on this branch's code; full `./gradlew build` shows 10 failures not caused by W-AI-GUEST | Recorded; not fixed here — it affects the base branch |

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-23 | W-AI-GUEST design spec written and approved — 7 functional requirements, 3 non-functional, 36 edge cases, 5 tasks across 4 waves. Three review lenses (Implementer, Tester, Attacker) run and findings merged | W-AI-GUEST | `086588b` |
| 2026-10-23 | W-AI-GUEST implementation plan written and approved — 5 tasks, 4 waves, 21 edge-case tests in T3, reviewed by lite-reviewer | W-AI-GUEST | plan commit |
| 2026-10-23 | T1: AiSearchFilters record, GuestAiConfig bean (guest.search), unit test — @ConditionalOnDatabase added to avoid datasource-free context failure | W-AI-GUEST | T1 commit |
| 2026-10-23 | T2: POST /api/guest/ai/search — DTOs, GuestAiSearchService, GuestAiSearchController, 10 integration tests (happy path, validation, degraded, auth, role) | W-AI-GUEST | T2 commit |
| 2026-10-23 | T3: 21 edge-case tests — vague query, flagged input, null filters, invalid output, partial dates, Unicode, blocked/refused/auth, throttling, concurrency | W-AI-GUEST | T3 commit |
| 2026-10-23 | T4: AI search bar on Guest search page — types, aiSearch function, AI form, explanation card, filter chips, clear action | W-AI-GUEST | T4 commit |
| 2026-10-23 | T5: 7 Vitest tests for AI search bar (renders, disabled states, explanation, degraded alert, clear, manual fallback) + checkstyle import order fix in GuestAiSearchController | W-AI-GUEST | T5 commit |
