# Branch State — W-INFRA

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-INFRA (from `development` at `e64e16c`)
- **Base:** `e64e16c`
- **Owner:** Xiao-Congchen
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-20 by Claude Sonnet 5.5 — branch and state created

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
| S1 | 2026-10-20 | Claude Sonnet 5.5 | W~1 | Blocked — needs human | Spec not started: Q5 | Get the Q5 answer from the operator, then write the W-INFRA spec with `writing-specs-lite` at `docs/superpowers/specs/2026-10-20-w-infra-design.md` (+ sibling folder) | 2026-10-20 |

**Remaining**

- [ ] Write the W-INFRA spec, lint it (`lint.mjs`), review it with `lite-reviewer`
- [ ] Write the plan with `writing-plans-lite`, lint, review
- [ ] Execute with `executing-plans-lite`
- [ ] **Handover:** note in the lite-skills trial what to revise in `lint.mjs`, templates and `lite-reviewer`

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-INFRA (proposed name) | Mono-repo skeletons, local Postgres, CI, deploy setup | Not started | — | — | Blocked on Q5 before spec | — |

---

## 7. Needs a Human

Q5 (in `PROJECT_STATE.md` § Needs a Human) blocks the spec: what happens to the JavaFX desktop code and
the root Gradle build — stay in place, move to `desktop/`, or be removed.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-20 | Created the W-INFRA branch and its branch state | W-INFRA | — |
