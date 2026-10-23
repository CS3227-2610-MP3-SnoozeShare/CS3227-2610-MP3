---
name: sync-project-state
description: Use when folding a merged, approved branch state into PROJECT_STATE.md - after a branch that has a workflow/project-state/branch-states/<slug>.md file has been merged to the integration branch (INT), when reconciliation reports a pending fold, or when the project owner says sync, fold, land or archive a branch state. Started on the project owner's yes in chat: the agent asks after the merge (AGENTS.md § 8), never runs it unasked. Keywords - sync-project-state, branch state, fold, provisional IDs, archive branch state, PROJECT_STATE.md, handover, Upcoming.
---

# Sync Project State

## Overview

`PROJECT_STATE.md` describes `INT`. Branches never edit it; each records what it changed in a
branch state, `workflow/project-state/branch-states/<slug>.md`
(see `maintaining-project-state`). **This skill folds one merged, approved branch state into
`PROJECT_STATE.md` and the Done ledger, then archives the branch state.** It is the only thing that
writes `PROJECT_STATE.md`, apart from the owner's hand-edited hotfixes.

It is **owner-gated**. After the merge the agent asks the owner (`AGENTS.md` § 8, gate 4) and runs it on their yes, or the owner invokes it, naming a branch state — or asking
for the list of pending ones. Nothing runs it without that yes: no CI, no hook. That makes the fold
serial, so IDs and counters never collide. It also means it can be forgotten, which is why a
branch state left in `branch-states/` on `INT` is the pending-fold signal.

**Where it runs.** On `INT`, or on a short-lived `sync/<slug>` branch cut from the merged
`INT` if the owner wants the fold reviewed as a PR. Keep **one sync PR open at a time** — a sync
branch edits `PROJECT_STATE.md`, so two open at once would conflict.

## Never

- Never **approve** a branch state. `Approved` is the project owner's. They may give it now, in
  this conversation; then write their name and the date. An agent's own opinion is not approval.
- Never **push**. Commit locally and report. On a sync branch, open the PR only if asked.
- Never fold a branch that is **not merged**.
- Never **overwrite** an entry that changed on `INT` since the branch's `Base`. Stop and ask.
- Never **rewrite the substance** of the branch state's text. You renumber IDs and fit the text to
  the target section's format; you do not reword decisions, deviations or the proposed *Now*.
- Never edit `docs/DeveloperGuide.md`. The guide is handled afterwards by `update-documentation`.

## Procedure

**0. Pick the state.** If the owner named a branch, use it. Otherwise list the non-archived files
in `workflow/project-state/branch-states/` and ask which. Fold one at a time, oldest merge first.

**1. Preconditions.** Stop and report if any fails.

- On `INT` (or a fresh `sync/<slug>` from it) with a clean tree.
- The branch is merged: `git branch --merged INT` lists it, or `git log INT` shows its merge.
- `Status` is `Approved` with the `Approved` field filled. If not, tell the owner; if they approve
  now, write `Approved: <name> YYYY-MM-DD`.
- The **session table is empty.** Rows left behind are stale or unfinished work: ask the owner
  whether to discard them (note it in the archive) or whether the branch is not really finished.
- **Remaining** has no unmarked open items. Ask the owner to drop each or mark it **Handover**.

**2. Read** `PROJECT_STATE.md`, the head of `workflow/project-state/done-ledger.md`, and the branch
state, in full.

**2a. Check the `No change` claims.** Every heading should be present. A missing heading is a gap: ask
the owner or the author to add it before folding. For each Architecture, Conventions or Known Gaps
section that says `No change`, skim the branch diff (`git diff <Base> <merge sha> --stat`) for new
directories, dependencies, profiles, scripts or boundaries it contradicts; if it does, stop and ask.
A true `No change` is skipped in step 5.

**3. Check for stale bases — before editing anything.** A pure addition never conflicts. An
**edit to an existing entry** (an area's *Now*, a Workstream row, a Convention, a Known Gap) does
if another fold or hotfix changed it since the branch's `Base`:

```bash
git diff <Base> INT -- PROJECT_STATE.md
```

For each entry the branch state replaces, compare it with the diff. If `INT` changed it, **stop
and ask the owner.** Show both versions. Reconcile the text with them; if the result changes what
the area *is*, record the resolution as a Decision. Never keep both versions side by side.

**4. Allocate permanent IDs.** For each namespace `W`, `C`, `D`, `Q`, find the highest number
already used in `PROJECT_STATE.md`, `done-ledger.md` **and** the ID maps in
`branch-states/archive/` — resolved Questions disappear from the file but their numbers are never
reused. Give the branch state's provisional IDs the next numbers, in the order they first appear
top to bottom. Rewrite every citation inside the branch state text in **one regex pass**
(`([WCDQ])~(\d+)`), so `C~1` can never match inside `C~12`. Keep the map; it goes in the archive.

**5. Apply, section by section.** Newest first wherever the target is newest-first. A section reading `No change` adds nothing.

| Branch state | Goes to |
|---|---|
| 3. Workstreams | New rows added; a changed existing row replaces the old one in place |
| 4. Architecture | Each area: replace its **Now**, merge Paths rows, add its decisions to the area's table. A new area becomes a new `### 4.n` subsection; update the map at the top if the structure changed |
| 5. Conventions, 6. Known Gaps | Add, replace or remove the named entries |
| 7. Needs a Human | Add new rows; delete resolved ones |
| 8. Decisions, 9. Deviations | Add rows / sections, Deviations in full |
| 10. Changes | Into `done-ledger.md` at the right dates, plus one line recording this fold |
| 2. Handover items | Into `PROJECT_STATE.md` § How to Resume → **Upcoming**, after the existing rows unless the owner says otherwise, each with `Source: handed over from <branch>` and today's date. A Handover item that came from a `Post-merge` plan task has Kind `Post-merge` |

Then update § Record (latest entry date, entry count) and the header — `Last updated: YYYY-MM-DD
by <model> — folded <slug>`.

**6. Reconcile.** Run the `maintaining-project-state` reconciliation check against the result: no
dangling provisional IDs (`grep -n "[WCDQ]~[0-9]" PROJECT_STATE.md` is empty), links resolve,
areas read as one coherent description. Stamp `Last verified against repo`.

**7. Archive.** Move the file with `git mv` to
`workflow/project-state/branch-states/archive/<YYYY-MM-DD>-<slug>.md`. In it set `Status: Folded`, and
append the fold record:

```markdown
## Fold record
Folded YYYY-MM-DD by <model> on <INT | sync/<slug>> · merge commit `<sha>` · base `<sha>`

| Provisional | Permanent |
|---|---|
| C~1 | C29 |

Sessions discarded at fold: <none | list, with the owner's yes>
```

**8. Commit** with `docs: sync <slug> branch state into PROJECT_STATE`. Do not push.

**9. Developer guide.** Ask this after the owner confirms the fold (step 10). Only if the project keeps one (`docs/DeveloperGuide.md`, or `AGENTS.md`
§ 5): for each folded workstream that is now `Done`, set Guide to `Awaiting confirmation` and ask
the owner **"Is W<n> confirmed for the developer guide?"** A yes becomes `Pending`; the
`update-documentation` skill takes it from there — on `INT`, on this sync branch, or on its own
branch and PR, at the owner's choice.

**10. Report and ask** (`AGENTS.md` § 8, gate 5): ask the owner to confirm the fold, then ask the step 9 guide question. Report briefly: the ID map, the sections touched, any stale-base decision, the Handover
items now in Upcoming, the Guide questions asked, and any further pending folds.

## Red Flags — Stop

- "The branch state says `Approved`, I'll skip the checks" → check the preconditions anyway; the
  field can be wrong.
- "It's only a typo in their text" → renumber and reformat only. If it is wrong, tell the owner.
- "Both versions are fine, I'll keep both" → an area has one *Now*. Reconcile with the owner.
- "I'll fold three branches at once" → one at a time, so each stale-base check sees the last fold.
- "The numbers don't matter, I'll skip the archive maps" → they are why a resolved Q's number is
  never reused.
- "I'll push so it's done" → the owner decides when it leaves the machine.
- "The guide needs updating, I'll do it now" → ask the confirmation question; do not edit the guide.
