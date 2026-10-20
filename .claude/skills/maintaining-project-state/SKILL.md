---
name: maintaining-project-state
description: Use when doing any work in this project - at session start before reading code or answering questions, when sizing a change as feature-level or larger, when a spec or plan is written, when a task starts/finishes/blocks, when the user makes a decision that shapes the design, and before ending a session or compacting context. Keywords - PROJECT_STATE.md, branch state, project state, source of truth, continuity, handoff, resume, what was I doing, where did we leave off, status, roadmap, context window, switching models or agents.
---

# Maintaining Project State

## Overview

Two documents carry the project's memory:

| | `PROJECT_STATE.md` | Branch state |
|---|---|---|
| **Path** | repo root | `docs/project-state/branch-states/<slug>.md` |
| **Describes** | `main` — what is merged, decided and handed over | what **one branch** has added or changed |
| **Written by** | the `sync-project-state` skill (the owner may edit it directly for a hotfix) | every agent working on that branch |
| **Lives** | permanently | until the branch merges and is folded in, then archived |
| **IDs** | permanent (`C14`) | provisional (`C~1`), numbered at fold |

`<slug>` is the branch name with `/` replaced by `-`.

The operator switches between models, agents, and context windows — sometimes running several
sessions and agents at once, on different branches, with teammates. None of that history survives;
these documents do. **Anything not written there did not happen.**

Branches never edit `PROJECT_STATE.md`, so they cannot conflict on it or collide on its ID
numbers. They read it for context and write to their own branch state. After the merge, the
project owner runs `sync-project-state` by hand to fold the branch state in.

`PROJECT_STATE.md` is an **index, not a library** — with one exception: a fact that contradicts
or post-dates every spec and plan has nowhere else to live, so it lives in full. Everything else is
a summary plus a link. It is ordered by how often each part is needed: **where we are**
(Orientation, How to Resume, Workstreams), **what the system is** (Architecture, Conventions),
**what not to touch and what is stuck** (Known Gaps, Needs a Human), then **the record**
(Decisions, Deviations, Record). Cite sections by name — `§ Known Gaps` — never by number.

## Read Before You Act

At the start of every session, and after any compaction:

1. **Read `PROJECT_STATE.md` in full** before reading code, answering a question, or planning
   anything. Never reconstruct state from source: code shows what is, never what was decided, what
   was rejected, or what is half-built.
2. **On a branch, read its branch state in full.** If `branch-states/<slug>.md` does not exist,
   create it from [branch-state-template.md](branch-state-template.md) before doing anything else.
   Its § How to Resume says what every session on the branch is doing and what to do next.
3. **Verify both still match the repo** — [reconciliation.md](reconciliation.md).

If `PROJECT_STATE.md` does not exist — [bootstrapping.md](bootstrapping.md).

## Every Change Is Recorded — Size Decides the Ceremony

|  | **Big** — feature level and above | **Small** |
|---|---|---|
| **Examples** | New user-facing capability; new module, service, or page; schema or data-model change; new dependency or architectural shift; cross-cutting refactor; anything spanning more than one session | Bugfix inside an existing design; copy or style tweak; single-function refactor; added tests; config change |
| **Before coding** | Spec → `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md`, plan → `docs/superpowers/plans/YYYY-MM-DD-<topic>.md`, and a Workstream row in the branch state linking both | Nothing. Just do it |
| **After** | Workstream status + a Changes line, both in the branch state | A Changes line in the branch state |

A **Changes line** is one line in the branch state's § Changes: what changed and why, with a
commit as the reference. At sync it is copied into the Done ledger,
`docs/project-state/done-ledger.md`, which is where "when did this change?" is answered.

**Small never means unrecorded.** Ceremony scales with size; the written record does not. A
one-line fix still gets a Changes line, and if it diverges from a plan, contradicts a spec, or
leaves a trap, it also gets a Deviations entry — that is where the detail belongs. Only pure
no-ops (formatting, a typo in a comment) can pass unwritten.

**The owner's hotfix exception.** The project owner may edit `main` directly for a hotfix or a
tiny edit, with no branch state. They write `PROJECT_STATE.md` and the ledger by hand, and the
change still gets its Done line.

Ambiguous size? **Treat it as big.** A spec you did not need costs twenty minutes; a feature
built without one costs a rewrite.

Use `writing-specs-lite` for the spec, `writing-plans-lite` for the plan and
`executing-plans-lite` to implement it (the repo defaults). `brainstorming`, `writing-plans` and `reviewed-plan` remain available only when the
operator asks for them by name.

**If the operator asks to skip the spec or the plan on a big change:** say so once, then do as
asked. It is their call. Record the skip as a decision, so the missing document is visible to
whoever inherits the work.

**If a request collides with a recorded decision** — a Known Gap, the out-of-scope list, or a
Decisions entry in `PROJECT_STATE.md` — **stop and confirm before doing anything.** Name the
entry, then ask which:

- **Keep the decision** — do not build it. Nothing changes.
- **Reverse it** — build it, record a decision reversing the entry in the branch state (who
  asked, why), note the entry to update or remove, and give the work its own workstream if it
  falls outside the current one.

Act on neither until the operator answers — even if they said "just do it", since the request
does not show they knew it overturns a recorded decision. This takes precedence over the
skip-the-spec rule above. If the operator is unavailable, stop and put the question in the
branch state's Needs a Human.

## Write Immediately When

On a branch, **write to the branch state**; `PROJECT_STATE.md` is read-only there.

1. **A workstream changes status, or a task starts, finishes, or blocks** — update its row and
   Progress cell. A guide-related note goes in the row too; the guide itself is handled at sync.
2. **A session starts, changes what it is doing, spawns or retires an agent, or ends** — add or
   update its row in § How to Resume immediately, including its **Next action**. Delete the row
   once its work is recorded in § Workstreams and § Changes.
3. **The shape of the system changes** — a new part, a moved responsibility, a changed boundary
   or data model — write the affected area's proposed **Now** in § Architecture, in the same write.
4. **The operator makes or changes a decision** affecting design, scope, or priorities —
   capture *who asked and why*, not just the outcome. Use a provisional ID (`C~1`).
5. **A spec or plan is created or materially revised** — link it from its workstream.
6. **Work completes, or reality diverges from a plan** — a Changes line, status, and Deviations.
7. **The session is ending, context is low, or you are about to compact** — reconcile your
   session row and § Remaining so a different session can take over cold. Anything that should
   outlive the branch is marked **Handover**.
8. **You merge `main` into the branch** — diff `PROJECT_STATE.md` since the branch state's
   `Base`, re-check every area and entry the branch edits, then move `Base` to the new `main` sha.
9. **The branch is finished** — empty the session table, set `Status: In review`, and tell the
   project owner. See below.

Not at the end, not batched: sessions end unpredictably and the update is what gets lost. **A
task is not complete until the branch state reflects it.**

## The Branch Lifecycle and the Approval Gate

`Draft` → `In review` → `Approved` → `Folded`.

- Agents set `Draft` (creating the file) and `In review` (spec, plan, build and review are done and
  the session table is empty).
- **Only the project owner sets `Approved`**, with their name and the date. An agent never
  approves a branch, and never writes the `Approved` field on its own initiative.
- **Do not merge a branch to `main` until it is `Approved` and its session table is empty.** If
  you are asked to merge one that is not, say so and ask.
- After the merge, the owner invokes `sync-project-state` — it refuses a state that is not
  `Approved`, so approval can still be given at that point. A file left in `branch-states/` on
  `main` means a fold is pending; reconciliation reports it.

## Two Things Agents Get Wrong

**§ Architecture is structured by the system, not by history.** One subsection per real part —
data, a boundary such as auth or tenancy, a core engine, the interface, each integration —
each opening with what that part *is today*, then a table of the decisions that shaped it.
A reader learns the design by reading it top to bottom, never by replaying a decision log. A
decision that changed the shape of the system lives with its area; one that changed how the
work is done lives in § Decisions. Each appears exactly once and keeps its ID. In a branch state
you write the area's proposed **Now**, ready to replace the one in `PROJECT_STATE.md`.

**The Progress cell is a position, not a diary.** At most ~15 words: where in the plan, what is
in flight, and the ID of whatever blocks or explains it — `Task 6/9 in flight: conflict engine`,
`Task 3/8 stopped: Q2`. What was built is in the plan and the Changes lines. Why something
changed mid-flight is a § Decisions or § Deviations entry, cited from the cell by ID only.

Both are spelled out in [document-structure.md](document-structure.md).

## The Acid Test

Before you stop, ask: **could a fresh agent, given only `PROJECT_STATE.md` and this branch's
state, take over any session in § How to Resume and take its Next action correctly, without
asking a question?**

If no, the file is wrong — fix the file, not your answer to the question.

## Status Vocabulary

Use exactly these, so status stays greppable.

**Workstream status** (`PROJECT_STATE.md` and branch state): `Not started` · `Spec'd` ·
`Planned` · `Building` · `Blocked — needs human` · `In review` · `Done` · `Abandoned`.
`Blocked — needs human` must always name what you need, in Needs a Human.

**Branch state status:** `Draft` · `In review` · `Approved` · `Folded`.

**Session status** (branch state § How to Resume): `Active` · `Paused` ·
`Blocked — needs human`. A finished session is not a status — its row is deleted once the work
is in § Workstreams and § Changes.

**Guide column** (Workstreams, only when the project keeps a developer guide):
`—` · `Awaiting confirmation` · `Pending` · `Documented YYYY-MM-DD` · `N/A`, owned by the
`update-documentation` skill. It tracks the guide, not the build.

**On a branch the Guide cell stays `—` (or `N/A`, with the reason in Progress), even when the workstream
is `Done` and the operator has confirmed the guide.** Only `sync-project-state` sets
`Awaiting confirmation`, at fold on `development`; the update-documentation checkpoint then runs there.
Record a branch-side confirmation as text in Progress (`guide confirmed by <owner> <date>`) and add
`guide out of date: <what>`, so the fold carries both. Never write the guide itself on a branch.

## Red Flags — Stop and Write

- "I'll update the branch state at the end" → you will run out of context first.
- "The workstream is `Done` and the owner confirmed the guide, so I'll set Guide to `Awaiting confirmation` or `Pending`" → not on a branch. Leave `—`, write `guide confirmed by <owner> <date>` and `guide out of date: <what>` in Progress; the fold sets the value.
- "It's a small change" → small changes are recorded too. Always.
- "I'll just edit `PROJECT_STATE.md`, it's quicker" → on a branch it is read-only. Write the
  branch state; sync does the rest.
- "The spec already says this" → the state files must be readable alone.
- "The operator told me, I'll remember" → you will not. Next session is a different you.
- "I'll put what I did this task in the Progress cell" → it is a position, not a diary. One
  line, then point at the plan, the Changes line, or an ID.
- "The architecture is obvious from the code" → then it is obvious to nobody who has not read
  the code. Write the area's proposed *Now*.
- "I'll add the decision to the decisions table and move on" → if it changed the system's
  shape, the area's *Now* is now wrong too. Rewrite it in the same edit.
- "This gap looks like a bug, I'll fix it" → check Known Gaps first; it may be deliberate.
- "They asked for it, so the out-of-scope entry doesn't apply" → it does. Ask: keep or reverse?
- "Another session is probably on this" → check its row's `Last touched` against `git log`
  first. A stale `Active` row means nobody is.
- "The branch looks done, I'll merge it" → not until the owner has approved it and the session
  table is empty.
- "I'll cite my `C~1` in the commit message / code comment" → provisional IDs are renumbered at
  fold and nothing outside the branch state is rewritten. Cite a permanent ID, or describe it.
- "This is Handover-worthy but I'll remember it" → you will not. Mark it **Handover** in
  § Remaining.

## Where the Rest Lives

Read these on demand, not every session:

| File | Read it when |
|---|---|
| [reconciliation.md](reconciliation.md) | Session start, after reading the files — verifying them against the repo, merging `main` into a branch, and resolving doc-vs-repo conflicts |
| [branch-state-template.md](branch-state-template.md) | Creating a branch state — the skeleton to copy, and how to write How to Resume |
| [document-structure.md](document-structure.md) | Writing a non-obvious entry — what each section is for, the branch-state rules, how to structure § Architecture, how terse the Progress cell must be, size and archive limits |
| [worked-examples.md](worked-examples.md) | Writing a Next action, decision, deviation, session row or Changes line — good and bad versions of each |
| [failure-modes.md](failure-modes.md) | You are about to skip or shortcut an update, or the files have decayed |
| [bootstrapping.md](bootstrapping.md) | `PROJECT_STATE.md` does not exist yet |
| [project-state-template.md](project-state-template.md) | Creating `PROJECT_STATE.md` — the skeleton to copy |
| `sync-project-state` skill | The owner is folding a merged branch state into `PROJECT_STATE.md`, or reconciliation reports a pending fold |
| `update-documentation` skill | The project keeps a developer guide and a workstream was folded as `Done`, a Guide value is `Awaiting confirmation` or `Pending`, or the guide contradicts the state file or code |
