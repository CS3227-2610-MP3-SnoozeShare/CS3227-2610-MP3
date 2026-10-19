# Branch State — lightweight-spec-plan-skills

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** lightweight-spec-plan-skills (from `development` at `33ee5b1`)
- **Base:** `33ee5b1`
- **Owner:** Xiao-Congchen
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-19 by Claude Sonnet 5.5 — first drafts of both skills

---

## 1. Scope

Add two project-level, lightweight skills, `writing-specs-lite` and `writing-plans-lite`, as an alternative to the heavier superpowers brainstorming / writing-plans flow. Process tooling only; no application code. Small change (AGENTS.md § 3): no spec or plan.

---

## 2. How to Resume

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| S1 | 2026-10-19 | Claude Sonnet 5.5 | — | Paused | Drafts written, awaiting operator review | Operator reviews `.claude/skills/writing-specs-lite/SKILL.md` and `writing-plans-lite/SKILL.md`; apply feedback; then commit | 2026-10-19 |

**Remaining**

- [ ] **Handover:** on `development`, update `PROJECT_STATE.md` § How to Resume → Upcoming row 1, which still names `brainstorming`/`writing-plans`, and add the lite skills to § Conventions (C~3).
- [ ] Operator review of the gap fixes and `lite-reviewer` in both skills.
- [ ] Trial the skills on the next real feature and revise.

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

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-19 | Added `writing-specs-lite` and `writing-plans-lite` project skills | — | pending commit |
| 2026-10-19 | Made the lite skills the defaults (`AGENTS.md` § 3, `maintaining-project-state`; old skills marked non-default); added gap fixes: size gate, approaches, `ui.md`, migration/rollback, reviewer brief, change control, traceability, close-out, parallel/resume rules | — | pending commit |
| 2026-10-19 | Added the `lite-reviewer` read-only subagent; specs and plans are reviewed by it (logic, then integration); plans may carry snippets; amendments live in the branch state | — | pending commit |
