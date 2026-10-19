---
name: writing-plans-lite
description: Use when an approved spec (from writing-specs-lite) needs an implementation plan before coding. A lightweight alternative to superpowers writing-plans - one section per task from the spec's implementation order, each with exact files to create/modify, what changes, and a checklist the implementing agent ticks off as it works.
---

# Writing Plans (lite)

A plan turns an **approved spec** into **tasks an agent can execute and tick off**. The spec decides *what*; the plan decides *which files, in which order, with which checks*. It is a working document: the implementing agent updates its checkboxes as it goes.

**Announce at start:** "I'm using writing-plans-lite to write the plan."

## Preconditions

- The spec's index status is `Approved` and has no open `Q`. If not, stop and finish the spec first.
- Read `PROJECT_STATE.md` (§ Conventions, § Known Gaps) and the branch state. A task must not implement a Known Gap or contradict a decision; if one would, raise it (`AGENTS.md` § 4) before writing the plan.
- Read the spec index and every component file, especially `implementation-order.md`.

## Rules

1. **Mirror the spec.** Sections are the tasks `T1..Tn` from `implementation-order.md`, same IDs, same order, same dependency table. Do not invent, merge or drop tasks; if the order is wrong, fix the spec first and note it.
2. **Exact files.** Every path is real: verify each "Modify" file exists (and the line range or symbol you refer to), each "Create" path does not and its directory is the right layer. Include test files.
3. **Say what changes, not the code.** Describe each modification precisely (method added, column added, branch changed, rule enforced) citing the spec section that defines it. Include code only for a contract that cannot be said in words (a signature, a SQL column list, a message string).
4. **Checklist is the work.** Steps are small, ordered, verifiable, and each ends in an observable result. They default to TDD: failing test -> implement -> green -> commit. Name the tests and what each asserts.
5. **No placeholders.** No "TBD", "add error handling", "handle edge cases", "similar to Task N". Edge cases come from `edge-cases.md`: list the rows each task must cover by scenario name.
6. **Small, green commits** (`AGENTS.md` § 6): one commit per task (or per logical slice), builds with `.\gradlew build`, handful of files. If a task touches more than ~15 files, split it and tell the operator the spec's order needs the same split.

## Layout

`docs/superpowers/plans/YYYY-MM-DD-<topic>.md`, one file. If it passes ~600 lines, the spec's feature is probably two workstreams.

```markdown
# <Topic> Implementation Plan

> **For agents:** tick `- [ ]` -> `- [x]` as you finish each step, in this file, in the same commit as the work. Keep the branch state's Progress cell current (`AGENTS.md` § 2). Spec: [<topic> design](../specs/YYYY-MM-DD-<topic>-design.md).

## Scope
**Goal:** <one sentence, from the spec>
**In scope:** <bullet list, from spec Goals F/N IDs>
**Non-goals:** <bullet list, from spec Non-goals; includes any Known Gap this must not touch, by ID>
**Risk tier:** <from spec> - <what that means for verification below>

## Task order
<copy the spec's dependency table; add a "Status" column: Not started / In progress / Done / Blocked>

## Verification gates
- Per task: `.\gradlew test` for the touched area, plus `.\gradlew checkstyleMain checkstyleTest` if Java changed.
- Before finishing: `.\gradlew build` green; each spec F/N acceptance criterion has a passing test or a recorded manual check.

---

### T1: <name from spec>
**Delivers:** <outcome from spec> | **Satisfies:** F1, N2 | **Depends on:** - | **Wave:** 1

**Files**
| Action | Path | What changes |
|---|---|---|
| Create | `src/main/java/.../Foo.java` | <responsibility; spec ref `data-model.md` § Foo> |
| Modify | `src/main/java/.../Bar.java` (`method()`) | <exact change> |
| Test | `src/test/java/.../FooTest.java` | <cases: scenario names from edge-cases.md> |

**Checklist**
- [ ] 1. Write failing `FooTest`: <asserts>. Run `.\gradlew test --tests <class>`; expect FAIL because <reason>.
- [ ] 2. Implement <change> in `Foo`/`Bar`.
- [ ] 3. Run the same test; expect PASS.
- [ ] 4. Cover edge cases: <scenario A>, <scenario B> (each a test).
- [ ] 5. `.\gradlew build` green.
- [ ] 6. Update branch state: Progress, Changes line.
- [ ] 7. Commit: `feat: <what and why> (<workstream>)`.

### T2: ...
```

## Process

1. Check preconditions.
2. **Verify against the repo**: open each file you will list; confirm names, signatures, package layering, and whether a file already exists. Fix the plan, not the guess.
3. Write the plan file task by task. Add steps for anything the spec's *Conventions* require (audit rows, `LedgerWriter`, `SourceText.read` in tests, no hard-coded future dates, rebuilding `db/snoozeshare-mock.db` after seed edits).
4. **Self-review** (below), fix inline.
5. Record in the branch state (workstream -> `Planned`, plan link, Changes line) and commit the plan alone (`docs: plan for <topic>`).
6. Hand off: offer execution (inline task-by-task, or one subagent per task within a wave - only for tasks the dependency table marks concurrent and that share no files). The executor ticks the checklist and updates Status.

## During execution (for whoever implements)

- Work in task order, respecting the table; do a task's steps in order.
- Tick each box as soon as it is done; mark the Task order Status; update branch state Progress (position, ~15 words).
- If reality diverges (file missing, spec wrong, new edge case): stop, write a Deviation in the branch state, amend the spec/plan with the operator, then continue. Never silently change scope.
- A task is done only when its checklist is all ticked, the build is green, and the branch state reflects it.

## Self-review

- [ ] Scope/goals/non-goals match the spec; every F/N is satisfied by a named task.
- [ ] Task IDs, order and dependencies identical to the spec.
- [ ] Every path verified; every Modify names the symbol or lines.
- [ ] Every edge-case row from the spec is assigned to a task's tests.
- [ ] Each checklist ends in a commit and a green build; no step is vague.
- [ ] Names used in later tasks match names defined in earlier ones.
- [ ] No task implements a Known Gap or violates a Convention.
