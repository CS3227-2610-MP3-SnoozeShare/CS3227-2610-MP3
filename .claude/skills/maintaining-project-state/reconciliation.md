# Reconciliation — Verifying State Against the Repo

Status drift is what kills these documents. One stale `Building` row and the next agent stops
trusting the whole file — at which point it has negative value, because it is now a
confident-sounding lie.

Run this **once per session, right after reading `PROJECT_STATE.md`** (and, on a branch, the
branch state), before acting on either.

## The Check

1. **Git first.** `git log --oneline -20` and `git status`. Does recent history match the Done
   ledger (and, on a branch, the branch state's Changes)? Uncommitted work in the tree that nothing
   mentions is the loudest possible signal that the last session ended without writing.
2. **Every `Building` workstream** — in the branch state, and in `PROJECT_STATE.md` if any appear
   there. Open its plan. Does the first unchecked task match what the repo actually contains? Does
   the referenced code exist?
3. **Every `Blocked — needs human` row**, workstream or session. Is it still blocked, or was the
   answer given in a session that never cleared the flag?
4. **On a branch: every session row** in the branch state's § How to Resume. `Active` with a
   `Last touched` that predates recent git activity on the branch is a session that died without
   saying so — mark it `Paused` (or delete it if its work already landed) rather than leaving it
   looking live. Check that each **Next action** is still the right step.
5. **On `main`: pending folds.** List the non-archived files in
   `docs/project-state/branch-states/`. Each is a branch that merged without being folded in, or one
   still in flight. If its branch is merged, tell the project owner a `sync-project-state` run is
   due; do not fold it yourself unless asked.
6. **Links.** Every spec and plan path resolves. A dead link is worse than no link.
7. **Architecture.** For any workstream that finished or changed shape since the last
   verification, does its area in `PROJECT_STATE.md` § Architecture still describe the code? On a
   branch, does each proposed **Now** in the branch state describe what the branch's code does? A
   "Now" that describes last month's design is the same failure as a stale `Building` row.
8. **On a branch: `Base`.** Compare the branch state's `Base` with the tip of `main`. If `main` has
   moved, follow [After Merging `main` into a Branch](#after-merging-main-into-a-branch) when you
   next merge it; note it now if the areas you edit are among those that changed.
9. **Guide column** — only if the project keeps a developer guide (`docs/DeveloperGuide.md`). List
   `PROJECT_STATE.md` rows whose Guide is `Awaiting confirmation` or `Pending`, or whose Progress
   says `guide out of date`, and read the `Developer guide` header line. Raise them with the
   operator — this is how guide updates survive a session ending. See the `update-documentation`
   skill.
10. **On a branch: your own session row.** If it is not there yet, add it before doing anything
    else — a different session may otherwise duplicate your work.
11. **What to do next.** On `main`, the first row of § How to Resume → Upcoming. On a branch, the
    `Active` and `Paused` session rows and § Remaining. Still right, given 1–10?

Correct what you find, then stamp `Last verified against repo` with today's date — in
`PROJECT_STATE.md` on `main` only; on a branch, correct the branch state and note the date in its
`Last updated` line.

Cheap version — the whole check is usually under a minute:

```bash
git log --oneline -20
git status --short
ls docs/superpowers/specs docs/superpowers/plans
ls docs/project-state/branch-states
git rev-parse origin/main        # compare with the branch state's Base
grep -E "Awaiting confirmation|Pending|guide out of date" PROJECT_STATE.md
```

## When the Document and the Repo Disagree

| Kind of claim | Winner | What to do |
|---|---|---|
| What the code does, what exists, what is committed | **The repo** | Correct the document. Note the correction in Deviations if it is surprising |
| What was decided, what is in scope, what was rejected, why | **The document** | The code is wrong or incomplete. Do not silently "fix" the document to match the code — that erases the decision |
| Both plausible, genuinely unclear | **Neither** | Do not guess. Raise it in Needs a Human and keep working on something else |

The asymmetry matters: code drifts from intent all the time, and the document is the only
record that intent ever existed. A decision quietly overwritten to match a stray commit is
unrecoverable.

## Do Not Skip This Because the File Looks Fine

A well-written stale file looks exactly like a well-written current one. The check is cheap
precisely so that it can be unconditional.

## After Merging `main` into a Branch

Merging `main` brings in other branches' folded work, and nothing in `PROJECT_STATE.md` is yours,
so there is nothing to resolve there. The risk is that your branch state was written against an
older `main`.

1. `git diff <Base> main -- PROJECT_STATE.md` — what changed on `main` since your `Base`.
2. For every area, Workstream row, Convention or Known Gap your branch state **edits** (not merely
   adds), check whether that entry changed. If it did, redraft your proposed text on top of the new
   one, keeping what `main` added; if the two cannot both be true, put the question in Needs a
   Human rather than guessing.
3. Check your provisional IDs still make sense — an ID you cite as permanent (`C14`) may have been
   superseded.
4. Move `Base` to the new `main` sha and update `Last updated`.

If two people share the branch and collide inside the branch state itself, keep both session rows
and both entries; the append-only sections never need one side dropped.
