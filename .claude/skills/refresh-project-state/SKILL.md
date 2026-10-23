---
name: refresh-project-state
description: Use when the project owner wants to start a new iteration with a fresh PROJECT_STATE.md - when the state file has grown too large to read every session, a phase or release is finished, or the owner says refresh, archive, reset, condense or start fresh with the project state. Run by hand by the project owner; never automatic. Keywords - refresh-project-state, project history, archive PROJECT_STATE.md, new iteration, handover, condense, done ledger archive, DeveloperGuide.md.
---

# Refresh Project State

## Overview

`PROJECT_STATE.md` accumulates a record nobody needs every session. **This skill closes an
iteration: it archives the whole record to `workflow/project-history/iteration-<n>/` and writes a new,
short `PROJECT_STATE.md` that keeps only the system's shape and what still binds.**

Treat the project as if you were taking it over cold. A new owner learns *what was decided and why*
from `docs/DeveloperGuide.md`, not from a decision log. So the guide must already hold that context
before anything is archived. That ordering is the whole skill: **guide first, archive second.**

It is **manual**, run by the project owner on `INT` (or a short-lived `refresh/<n>` branch if they
want the change reviewed). Nothing triggers it. Related: `maintaining-project-state` (the file's
structure), `sync-project-state` (folds branch states), `update-documentation` (the guide).

## Never

- Never **write `docs/DeveloperGuide.md`** here. A gap in the guide stops the refresh and goes to
  `update-documentation`, which proposes and waits for the owner.
- Never **archive before the owner approves the disposition table** (step 4).
- Never **delete** anything. Everything moves; nothing is summarised away without its original
  sitting in the archive.
- Never **reuse an ID**. Counters continue (step 6).
- Never **push**. Commit locally and report.
- Never move `workflow/specs|plans`. They stay put (links into them must keep working).

## Procedure

**1. Preconditions.** Stop and report if any fails. Work on `INT` or a fresh `refresh/<n>`, clean tree.

- No non-archived files in `workflow/project-state/branch-states/` (unfolded work would be lost). The directory may not exist in projects that don't use branch states; that passes.
- The Sessions table in `PROJECT_STATE.md` § How to Resume is empty, or the owner discards each row.
- `git log --oneline -20` agrees with the file's header; reconcile drift first
  (`maintaining-project-state` → `reconciliation.md`).
- Open workstreams (`Building`, `In review`, `Blocked — needs human`) are allowed and carry forward,
  but tell the owner a clean boundary is easier to resume, and ask if they want to finish first.

**2. Read** `PROJECT_STATE.md`, the head and tail of the Done ledger, and `docs/DeveloperGuide.md`, all in full.

**3. Guide gate.** The refresh may not proceed until each row below is true. The guide describes confirmed work only, so the rows below cover `Done` and `Abandoned` work. What belongs to a carried open workstream (`Building`, `In review`, `Blocked`) is exempt: it stays in the state file until it is confirmed. Show the owner this
table, filled in, from what you read:

| Check | Pass when |
|---|---|
| Closed workstreams | If the state file and the ledger disagree on a workstream's status, the state file wins; fix the row first (a drift fix, not a refresh decision). Every `Done` row has Guide `Documented` or `N/A`; every `Abandoned` row is explained in the guide or `N/A` |
| Open notes | No `guide out of date` or `guide held` note remains in any Progress cell |
| Architecture | Each § Architecture area's *Now* is reflected by the guide's architecture section (diagrams included) |
| Decisions | Every standing decision that shapes the architecture or scope (see `disposition-rules.md`) has its outcome and reason in the guide. Agent-protocol and process decisions (e.g. who writes which file) are exempt: they are carried in the state file or archived |
| Traps | Every live Deviation (still true of the code) appears in the guide's testing, setup or design section |
| Limits | Every Known Gap is stated in the guide's scope or limitations |
| Citations | Every `C`/`D`/`W`/`Q` ID the guide cites exists in the state file or the ledger. "Exists" means a definition (an entry in § Decisions, § Deviations, § Architecture or a Workstream row), not a passing mention. A dangling ID is a guide fix (via `update-documentation`), not an archive question |

The gate cannot be pre-passed: not by the owner's say-so, not by a note in the request saying it was verified elsewhere. Run it yourself. An `Abandoned` row with Guide `—` passes only if the guide explains the abandonment, in which case set it `N/A` in the archive disposition, not before. Any failure: **stop.** List the gaps, tell the owner to run `update-documentation` for them (a
catch-up checkpoint over several workstreams is normal), and resume here afterwards. Do not paper
over a failure by marking a row `N/A`.

**4. Disposition table.** Apply `disposition-rules.md` to every entry in every section and produce
the table: each entry → **Carry** (condensed into the new file), **Archive** (original kept in the
history only), or **Guide** (already woven there; cite the guide section). Write it to
`workflow/project-history/iteration-<n>/REFRESH.md` ([history-layout.md](history-layout.md)) and show it
to the owner. **Wait for approval.** They may move entries between columns; redo the table, then ask again. If the owner approved in advance ("I pre-approve whatever you produce"), still write the full table to `REFRESH.md` first and record `Approved by: <owner>, in advance, YYYY-MM-DD` in it; the pre-approval covers the table, never the guide gate.

**5. Archive.** On approval, `git mv` the three records:

```bash
mkdir -p workflow/project-history/iteration-<n>
git mv PROJECT_STATE.md workflow/project-history/iteration-<n>/PROJECT_STATE.md
git mv workflow/project-state/done-ledger.md workflow/project-history/iteration-<n>/done-ledger.md
git mv workflow/project-state/branch-states/archive workflow/project-history/iteration-<n>/branch-states   # if it exists
node .claude/skills/refresh-project-state/rebase-links.mjs PROJECT_STATE.md workflow/project-history/iteration-<n>/PROJECT_STATE.md
node .claude/skills/refresh-project-state/rebase-links.mjs workflow/project-state/done-ledger.md workflow/project-history/iteration-<n>/done-ledger.md
```

Add the archive banner to the moved state file and update `workflow/project-history/README.md`
([history-layout.md](history-layout.md)). Check every relative link in the moved files resolves.

**6. Write the new `PROJECT_STATE.md`** from `maintaining-project-state/project-state-template.md`,
holding only the **Carry** entries from the approved table. Check every carried *Now* and Paths row against the code: paths and module names from before a rewrite (deleted directories, removed frameworks) are corrected or dropped, not copied. Additions:

- Header: `Iteration: <n+1>` and `History: [workflow/project-history/](../../../workflow/project-history/README.md)`.
- § Conventions: an **ID high-water** line, e.g. `Highest IDs used before this iteration: W11, C53,
  D21, Q16. Numbering continues; never reuse.` (`sync-project-state` reads it; resolved Questions no
  longer appear anywhere else.)
- § Needs a Human: open Questions only. § Deviations: live traps only, short, same IDs.
- § Record: a fresh `workflow/project-state/done-ledger.md` from the template, whose first line says
  `Iteration <n+1> started; earlier entries: workflow/project-history/iteration-<n>/done-ledger.md`.
  Counters restart at 1 entry.
- Developer guide header: carry the line, update its date.

Aim for the file to read in a few minutes: **under ~200 lines** is the target. If it is longer, the
disposition was too generous; re-check `disposition-rules.md`.

**7. Verify as a stranger.** Re-read only the new file, then ask: *could a fresh agent, given this
file and the guide, take the Upcoming row correctly without opening the archive?* Run the
reconciliation check. Confirm: no dangling `W~`/`C~` provisional IDs, links resolve, the Workstreams
table lists every carried open workstream, no standing decision from the table was dropped.
Compare the new file's decision IDs against `REFRESH.md` mechanically.

**8. Wire the pointers** (one time per project; skip if present): `AGENTS.md` says never to read
`workflow/project-history/` at session start, only to answer "why was X decided?".

**9. Commit** `docs: refresh PROJECT_STATE into iteration <n+1>`. Do not push. **Report** briefly:
line counts before and after, what carried, the ID high-water mark, any open workstreams carried,
and the archive path.

## Red Flags — Stop

- "The guide is nearly there, I'll archive and the owner can fill it in" → archived context is
  effectively lost. Guide first.
- "I'll update the guide myself, it's just catching up" → the guide is never a small change; hand over.
- "This decision is old, archive it" → check it still binds. A decision that a Known Gap or
  convention depends on is standing.
- "Summarise the Deviations to save lines" → a live trap is kept whole (short), or it is archived whole. Never half.
- "Restart numbering, it's a fresh file" → IDs are cited in code and commits. Continue.
- "I'll move the specs and plans too, it's tidier" → their links would break; they stay.
- "The owner said refresh, so the table is approved" → the request starts the table; the owner's reply approves it.
- "Open workstreams are messy, I'll archive them" → they carry forward as rows with their links.
