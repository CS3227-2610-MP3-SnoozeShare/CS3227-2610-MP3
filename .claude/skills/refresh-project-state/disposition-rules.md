# Disposition Rules

For each entry in the old `PROJECT_STATE.md`, choose **Carry**, **Archive** or **Guide**. The test
is: *does a new owner, working only from the new file and the guide, need this to avoid a wrong
build?* The archive keeps every original either way, so when in doubt about a **binding** fact,
Carry; when in doubt about a **historical** one, Archive.

**Guide** is not a fourth place the entry lives in the state file. It marks that the guide already
holds it, so the entry is Archive in the state file and the guide section is cited in `REFRESH.md`.

## Section by section

| Old section | Default | Rule |
|---|---|---|
| Header | Carry | Rewrite: phase, stack, branch, last updated, iteration, history link, guide line |
| 1. Orientation | Carry | Keep the purpose paragraph and repo map; add `workflow/project-history/` |
| 2. How to Resume | Carry | Commands as-is. Upcoming rows carry. Session rows must already be gone |
| 3. Workstreams | Split | `Done` and `Abandoned` → Archive. Open ones (`Spec'd`, `Planned`, `Building`, `In review`, `Blocked — needs human`, `Not started`) → Carry as rows with spec/plan links |
| 4. Architecture | Carry | Each area's **Now** and Paths table carry (tighten wording; delete progress narration like "in progress" notes that are now stale). Per-area **decision tables**: see Decisions below. The map diagram carries |
| 5. Conventions | Carry | All; drop ones that refer to deleted code. Add the ID high-water line |
| 6. Known Gaps | Carry | All, plus the out-of-scope list. They are decisions, not history (`AGENTS.md` § 4) |
| 7. Needs a Human | Split | Open → Carry. Resolved → Archive |
| 8. Decisions | Split | See below |
| 9. Deviations | Split | Live → Carry (short). Resolved or obsolete → Archive |
| 10. Record | Replace | New empty ledger; pointer to the archived one |

## Decisions: standing or historical

A decision is **standing** if any of these is true. Carry it as one row: ID, date, the decision in
one sentence, why in one sentence, and `full entry: iteration-<n>/PROJECT_STATE.md`.

- It shapes a current § Architecture area (the area's *Now* depends on it)
- A § Convention or § Known Gap depends on it
- Reversing it would need the operator's confirmation (`AGENTS.md` § 4): scope, tenancy, hosting, auth providers
- It rejects something a future agent is likely to re-propose

Otherwise it is **historical** and goes to the archive: process decisions about how a past
workstream was run (commit size for W1, which branch W4 lived on), decisions **superseded** by a
later one (keep only the superseder, and say it reverses the earlier ID), plan-acceptance records
(`P3-1 to P3-9 accepted`), and one-off operator calls whose effect is fully in the code and guide.

Where one entry supersedes another, the carried row names the old ID in *why* so the chain survives
in one line.

## Deviations: live or resolved

**Live** if it is still true of the code or the environment and could cost a future agent time:
a harness quirk that still occurs, a naming difference from a plan that still exists, a data trap
(old category colours), an operational recovery step. Carry it in **three lines or fewer**: what,
the consequence, the ID. **Resolved** if the thing it describes was deleted, fixed or superseded
(for example traps in the old Next harness after the Next app was deleted): Archive.

Long multi-round narratives (a "second update … eighth update" thread) are never carried; the
live outcome, if any, becomes a Convention or a one-line live deviation.

## Open work is not history

Anything `Building`, `In review`, `Blocked`, or with an unresolved Question is carried whole enough
that a stranger can resume it: status, spec and plan links, a position-only Progress cell (≤ ~15 words).
If its detail lived in a long Progress cell or a Deviation, move the detail into the plan or the
workstream's row and cite it.

## Output: the `REFRESH.md` table

One row per source entry, grouped by section:

```markdown
| Source | Entry | Disposition | Where / why |
|---|---|---|---|
| § Decisions | C20 commit size | Archive | Process, W1 only; the convention it created is carried in § Conventions |
| § Decisions | C32 reverse to SPA | Carry | Standing: shapes § Architecture 4.6; supersedes W5's choice |
| § Deviations | D13 Next E2E scope | Archive | Resolved: harness deleted in D18 |
| § Deviations | D17 Playwright teardown | Carry | Live on the Windows harness |
| § Workstreams | W3 UI redesign | Guide | Documented 2026-09-19 (guide § Design) |
```

Every entry in the old file appears exactly once. Count the rows against the source IDs before
showing it to the owner.
