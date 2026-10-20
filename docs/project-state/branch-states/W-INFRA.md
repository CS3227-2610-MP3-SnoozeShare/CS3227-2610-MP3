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
| S1 | 2026-10-20 | Claude Sonnet 5.5 | W-INFRA | Paused | Spec drafted; awaiting operator approval | Operator reads the spec and strikes or accepts SD13; then optionally run review round 2 (Implementer + Maintainer lenses), `lint.mjs spec --approved`, then write the plan with `writing-plans-lite` | 2026-10-20 |

**Remaining**

- [ ] Operator approves the spec (set Status Approved in its index)
- [ ] Write the plan with `writing-plans-lite` at `docs/superpowers/plans/2026-10-20-w-infra.md`, lint, review
- [ ] Before T1: record per-class desktop test counts from `desktop/build/test-results` (F1 baseline) here
- [ ] Execute T1-T8 with `executing-plans-lite`
- [ ] **Handover:** T9, the live dev deployment, runs on `development` after the merge (needs a human; see SD10)
- [ ] **Handover:** at fold, update PROJECT_STATE § How to Resume commands, § Orientation paths, remove Q5, and note C~2 against C77 and Webapp design § 6.2
- [ ] **Handover:** lite-skills trial findings (below, § Lite-skills trial) to revise `lint.mjs`, templates and `lite-reviewer`

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-INFRA (proposed name) | Mono-repo skeletons, local Postgres, CI, deploy setup | Spec'd | [spec](../../superpowers/specs/2026-10-20-w-infra-design.md) | — | Spec drafted, review round 1 applied; awaiting approval | — |

---

## 7. Needs a Human

- Spec approval, and strike-or-accept for the small-scope items in SD13.
- T9 (after merge): create Railway, Vercel and Neon projects/branch, set variables (runbook order).

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-20 | Q5 resolved: the desktop code and root Gradle build move under `desktop/`; `backend/` and `frontend/` sit beside it; nothing is deleted. Independent Gradle builds (no root build); Checkstyle ruleset stays at root `config/checkstyle/` | Operator in chat; rejected: leave in place, delete now, root multi-project | [spec SD1-SD3](../../superpowers/specs/2026-10-20-w-infra-design.md) |
| C~2 | 2026-10-20 | Dev config: `application-dev.yml` is committed with non-secret local defaults; secrets go in a gitignored `application-local.yml`. Refines C77 and Webapp design § 6.2 ("secrets in gitignored dev config") | Agent; a committed no-secret file lets a fresh clone run | spec SD8 |
| C~3 | 2026-10-20 | Required checks keep names `checkstyle` and `build`, each covering all three components; backend tests use Testcontainers Postgres 17; W-INFRA includes the live dev deployment (Railway `dev`, Vercel with Production Branch `development`, Neon branch `dev`), done after merge | Operator chose these | spec SD4-SD6, SD9, SD10 |

---

## Lite-skills trial

Observations for the Handover todo (what to revise):
- Reviewers caught real defects the author missed: actuator CORS is separate from MVC CORS; a pure rename commit cannot build; index F-numbers drifted from `requirements.md`; unscoped secret scan. Keep multi-lens review.
- `lint.mjs` does not check that the index's goal summary matches `requirements.md` IDs, nor test-layer vocabulary; consider both.
- Four parallel reviewers on a Medium spec produced heavily overlapping findings; two lenses (Implementer + Tester) would have caught most.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-20 | Created the W-INFRA branch and its branch state | W-INFRA | `8f80e0a` |
| 2026-10-20 | W-INFRA design spec (index + 7 component files) written, linted, reviewed by four lenses | W-INFRA | — |
