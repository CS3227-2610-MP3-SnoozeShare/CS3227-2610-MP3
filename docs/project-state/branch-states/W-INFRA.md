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
| S1 | 2026-10-20 | Claude Sonnet 5.5 | W-INFRA | Paused | Plan written and reviewed (round 1 applied); awaiting operator review | Operator reviews `docs/superpowers/plans/2026-10-20-w-infra.md`; then execute from T1 step 1 (baseline test counts) with `executing-plans-lite`; Wave 1 (T1, T2, T5) may run in parallel | 2026-10-20 |

**Remaining**

- [ ] Operator reviews the plan
- [ ] Before T1: record per-class desktop test counts from `desktop/build/test-results` (F1 baseline) here
- [ ] Execute T1-T8 with `executing-plans-lite`
- [ ] **Handover:** T9, the live dev deployment, runs on `development` after the merge (needs a human; see SD10)
- [ ] **Handover:** at fold, update PROJECT_STATE § How to Resume commands, § Orientation paths, remove Q5, and note C~2 against C77 and Webapp design § 6.2
- [ ] **Handover:** lite-skills trial findings (below, § Lite-skills trial) to revise `lint.mjs`, templates and `lite-reviewer`

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-INFRA (proposed name) | Mono-repo skeletons, local Postgres, CI, deploy setup | Planned | [spec](../../superpowers/specs/2026-10-20-w-infra-design.md) | [plan](../../superpowers/plans/2026-10-20-w-infra.md) | Spec approved 2026-10-20; plan reviewed; T1 not started | — |

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

## Lite-skills trial

Observations for the Handover todo (what to revise):
- Reviewers caught real defects the author missed: actuator CORS is separate from MVC CORS; a pure rename commit cannot build; index F-numbers drifted from `requirements.md`; unscoped secret scan. Keep multi-lens review.
- `lint.mjs` does not check that the index's goal summary matches `requirements.md` IDs, nor test-layer vocabulary; consider both.
- Plan phase: `lint.mjs plan` reports "Modify path does not exist" for a file an earlier task creates (T4 modifying T3 files); it is a false positive, so either let a Modify row cite an earlier task's Create or downgrade it to a warning. It also requires the close-out heading to carry no `T` number, which the skill text (`Tn+1`) does not say.
- Plan review (one Implementer lens) found real defects the author missed: `properties()` is the lowest Spring precedence so an E10 test would connect to a local DB; a copied `gradlew` loses its exec bit; the root `.gitignore` hides the new wrapper jar and `.env.example`; Gradle 9 needs `junit-platform-launcher`. One reviewer was enough at the plan stage.
- Spec and plan tooling friction: very long inline shell heredocs failed twice; write scripts to files instead.
- Four parallel reviewers on a Medium spec produced heavily overlapping findings; two lenses (Implementer + Tester) would have caught most.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-20 | Created the W-INFRA branch and its branch state | W-INFRA | `8f80e0a` |
| 2026-10-20 | W-INFRA design spec (index + 7 component files) written, linted, reviewed by four lenses | W-INFRA | `73ab826` |
| 2026-10-20 | Spec approved by Xiao-Congchen; W-INFRA implementation plan (T1-T9 + close-out) written, linted, reviewed; spec amended (C~4) | W-INFRA | — |
