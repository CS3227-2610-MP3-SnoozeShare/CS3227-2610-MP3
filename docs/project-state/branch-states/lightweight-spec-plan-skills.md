# Branch State — lightweight-spec-plan-skills

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** lightweight-spec-plan-skills (from `development` at `33ee5b1`)
- **Base:** `33ee5b1`
- **Owner:** Xiao-Congchen
- **Status:** Approved
- **Approved:** Xiao-Congchen
- **Last updated:** 2026-10-19 by Claude Sonnet 5.5 — reviewed

---

## 1. Scope

Add the project-level lightweight spec/plan workflow and make it the repo default: skills `writing-specs-lite`, `writing-plans-lite`, `executing-plans-lite`, the read-only `lite-reviewer` agent and the `lint.mjs` checker. Process tooling only; no application code. Small change (AGENTS.md § 3): no spec or plan.

---

## 2. How to Resume

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|

**Remaining**

- [ ] **Handover:** on `development`, update `PROJECT_STATE.md` § How to Resume → Upcoming row 1, which still names `brainstorming`/`writing-plans`, and add the lite skills to § Conventions (C~3).
- [ ] **Handover:** on `development`, `PROJECT_STATE.md` must record the web pivot (C~7): Stack, Orientation, Phase, How to Resume commands, and the Known Gaps / out-of-scope entries it reverses (Q~1). Until then the lite skills tell agents to read those sections fresh, so the first web-app spec will hit the `AGENTS.md` § 4 keep-or-reverse stop for each entry.
- [ ] **Handover:** trial the lite skills on the first web-app spec and plan; revise `lint.mjs`, templates and `lite-reviewer` from what it shows.

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-19 | Lite skills are separate files (`writing-specs-lite`, `writing-plans-lite`) alongside, not replacing, the superpowers-derived `brainstorming` / `writing-plans` | Operator wants a lightweight alternative; replacing would break the existing workflow in AGENTS.md | Operator request |
| C~2 | 2026-10-19 | A spec is an index file at the AGENTS.md § 3 path plus a sibling folder of component files (requirements, data model, API, conventions, edge cases, implementation order) | Operator: no single mega-document; keeps the AGENTS.md path valid | Operator spec |
| C~3 | 2026-10-19 | The lite skills are the repo defaults for spec and plan; `brainstorming`, `writing-plans`, `reviewed-plan` only when the operator names them. Amends C~1 (alongside, not replacing) | Operator: set as defaults moving forward | `AGENTS.md` § 3 |
| C~4 | 2026-10-19 | Spec and plan reviews run in dedicated read-only `lite-reviewer` subagents (`.claude/agents/lite-reviewer.md`, tools Read/Grep/Glob), one per lens, reviewing logic first, then seams and integration. A separate agent configuration, not inline prompts. Rejected: self-review by the author agent | Operator | `.claude/agents/lite-reviewer.md` |
| C~5 | 2026-10-19 | Plans carry code snippets for tricky logic, schemas and any context-heavy component, not only contracts | Operator | `writing-plans-lite` rule 3 |
| C~6 | 2026-10-19 | Spec/plan amendments are recorded in the branch state as Decisions (no `## Amendments` heading in the spec or plan; the index keeps a one-line pointer) | Operator: amendments come from decision changes | `writing-specs-lite` change control |
| C~7 | 2026-10-19 | This iteration pivots SnoozeShare from a single-user Java desktop app to a multi-user web app (design: `docs/Webapp-architecture-design.md`). Skills and the reviewer agent are therefore stack-agnostic: they read invariants and commands from `PROJECT_STATE.md`; multi-user networked work has a Medium floor and High for authN/authZ, tenancy, money or personal data | Operator, 2026-10-19 | operator conversation |
| C~8 | 2026-10-19 | Added: stable edge-case IDs (`E`), spec/plan templates, `lint.mjs` (deterministic spec and plan checks), a two-round review cap, and `executing-plans-lite` (default for implementing plans) | Operator approved "proceed with all 7" | `.claude/skills/` |

## 7. Needs a Human

| ID | What is needed | What it blocks | Raised |
|---|---|---|---|
| Q~1 | Owner decides, per entry, which `PROJECT_STATE.md` § Known Gaps and "Explicitly out of scope" items the web pivot reverses (no multi-instance deployment, fully mocked auth, microservices/brokers/horizontal scaling, JPA, DTO layer, single-process events) and records them on `development` (`AGENTS.md` § 4) | The first web-app spec | 2026-10-19 |

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-19 | Added `writing-specs-lite` and `writing-plans-lite` project skills | — | this branch |
| 2026-10-19 | Made the lite skills the defaults (`AGENTS.md` § 3, `maintaining-project-state`; old skills marked non-default); added gap fixes: size gate, approaches, `ui.md`, migration/rollback, reviewer brief, change control, traceability, close-out, parallel/resume rules | — | this branch |
| 2026-10-19 | Added the `lite-reviewer` read-only subagent; specs and plans are reviewed by it (logic, then integration); plans may carry snippets; amendments live in the branch state | — | this branch |
| 2026-10-19 | Smoke-tested `lite-reviewer` on the MP3-0 spec and plan: it returned substantive logic and integration findings; revised the agent (stack-agnostic, `retrospective` mode, `UNVERIFIED` and `AGENT-NOTES` slots, chunked reads) | — | this branch |
| 2026-10-19 | Added `.claude/skills/writing-specs-lite/scripts/lint.mjs`, spec and plan templates, edge-case IDs, review round cap and `executing-plans-lite`; made the skills stack-agnostic for the web pivot; wired `executing-plans-lite` into `AGENTS.md` § 3 | — | this branch |
