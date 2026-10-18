# Document Structure — What Goes Where

The section list and the rules for keeping `PROJECT_STATE.md` useful as it grows, and for the
branch state that feeds it. Copy the skeletons from
[project-state-template.md](project-state-template.md) and
[branch-state-template.md](branch-state-template.md).

## The Sections

Ordered by how often they are needed, so the top of the file answers "where are we?" and the
bottom holds the record. **Cite sections by name** — `§ Known Gaps` — never by number.

| § | Section | Holds | Keeps you from |
|---|---|---|---|
| 1 | **Orientation** | What the project is, stack, phase, repo map, `Last updated` | Re-deriving the project from the source tree |
| 2 | **How to Resume** | Exact commands to reach a working state, plus **Upcoming**: planned workstreams and todos handed over at syncs, in priority order. (In a branch state: the session table and Remaining work instead) | Guessing how to build and test, or not knowing what to pick up next |
| 3 | **Workstreams** | One row per feature-level unit: status, spec, plan, a one-line position, Guide | Losing track of what is in flight |
| 4 | **Architecture** | How the system is put together, area by area: current shape, then the decisions that produced it | Reading the whole source tree to learn the design, or reversing a structural decision by accident |
| 5 | **Conventions** | Rules every change must honour; the ID namespaces | Rediscovering the same trap every session |
| 6 | **Known Gaps & Accepted Limitations** | Deliberately not built or not fixed, with the reason | "Helpfully" fixing an intentional omission |
| 7 | **Needs a Human** | Anything an agent cannot do: credentials, deploys, accounts, judgement calls | Stalling silently, or pretending a blocker is solvable |
| 8 | **Decisions & Context** | Decisions that do *not* shape the architecture: process, priorities, scope, tooling | Re-litigating settled questions |
| 9 | **Deviations & Discoveries** | Where reality diverged from a spec or plan, and why | Re-deriving why the code differs from the document describing it |
| 10 | **Record** | A pointer to `docs/project-state/done-ledger.md`, plus the latest entry's date and the entry count | Re-doing work, or wondering when something changed — without the ledger crowding out §§ 1–3 |

§ Architecture, § Known Gaps and § Deviations are the ones agents skip and then regret.
§ Known Gaps in particular is load-bearing: without it, every session re-discovers the same
"bug" and wastes effort fixing what was never broken.

## The Architecture Section

A list of decisions in date order records *history*. It does not tell you what the system is —
to learn that you have to replay every entry and work out which ones still hold. So the
structure comes first and the decisions hang off the part they shaped.

**One subsection per area.** Areas mirror the system's real parts, usually drawn from the
spec's architecture section: the data model, a boundary such as auth or tenancy, a core engine,
the interface, each external integration. Three to eight of them. Not one per directory, and
not one per class.

Each subsection holds, in this order:

1. **Now** — what the area is and how it behaves *today*, in a few lines, naming the real files
   and functions. Include what it must never do when that is the point of it ("no database
   access in here"). Mark anything settled but unbuilt *(planned)*; leave out anything merely
   proposed.
2. **Paths** — an optional small table of the files that make up the area and what each owns.
3. **Decisions** — the same table as § Decisions (`ID · Date · Decision · Why / who asked ·
   Source`), holding only the decisions that shaped *this* area.

At the top of the section, a map of under fifteen lines — a directory sketch, a request path,
an arrow diagram — whichever shows the real structure. Prose cannot replace it.

**Which section does a decision go in?** If it changed the shape of the system — a boundary, a
data model, a dependency, where a responsibility lives, how two parts talk — it belongs to an
area in § Architecture. If it changed how the *work* is done — process, priorities, scope,
sequencing, tooling — it belongs in § Decisions. Either way it appears exactly once and keeps
its ID if it moves.

**Keeping it current.** When a workstream changes the shape of the system, update the affected
area in the same write as the status change — not later. When a decision is superseded, rewrite
"Now" to describe what is true and leave both decisions in the table, the later one citing the
earlier. A superseded decision is never deleted: it is why the earlier code looked as it did.

## The Progress Cell

The Workstreams table answers "where are we?" at a glance. A Progress cell that narrates what
was built buries that answer, and duplicates the plan.

**At most ~15 words**, saying at most three things: the position in the plan, what is in
flight, and the ID of whatever blocks or explains it.

| Write | Not |
|---|---|
| `Task 6/9 in flight: conflict engine` | A summary of Tasks 1–5 |
| `Task 3/8 stopped: Q2` | "Waiting for the operator to set up the database, which we need because…" |
| `All 9 tasks done; see D3` | The story of how D3 came about |
| `Spec approved 2026-09-16; plan not started` | "Spec approved after three rounds of review with…" |

Everything cut from the cell has a home: **what** was built is in the plan and the Done ledger;
**why** something changed mid-flight is a § Decisions or § Deviations entry, cited here by ID;
**what is needed** is a § Needs a Human row, cited here by ID. The cell points; it does not
explain.

Update it when a task starts, finishes, or blocks — the same moments that change the status.

The `update-documentation` skill also writes short markers here — `guide held YYYY-MM-DD:
<reason>`, `guide out of date: <what>`. Keep them short and leave them until that skill clears
them.

## The Branch State

`PROJECT_STATE.md` describes `main`. Everything still in flight lives in a branch state,
`docs/project-state/branch-states/<slug>.md`, copied from
[branch-state-template.md](branch-state-template.md). It exists so that branches never edit
`PROJECT_STATE.md` — no shared counters, no shared insertion points, no ID collisions — and so
that an interrupted session on the branch can be resumed by anyone.

**It mirrors the project-state sections but holds only what the branch adds or changes.** A
section with nothing to say is omitted. It is not a second copy of the project; if a reader needs
the whole picture they read `PROJECT_STATE.md` and then the branch state.

| Branch-state section | Holds | At fold it becomes |
|---|---|---|
| Header | Branch, `Base`, owner, `Status`, `Approved`, `Last updated` | Archive only |
| 1. Scope | What the branch is for | A Workstream row |
| 2. How to Resume | Setup, the session table, Remaining work | Sessions discarded; **Handover** items go to § How to Resume → Upcoming |
| 3. Workstreams | Rows created or changed | § Workstreams |
| 4. Architecture | Per area: proposed **Now**, Paths, decisions | § Architecture |
| 5–9. Conventions … Deviations | Entries added, changed or removed | The matching section |
| 10. Changes | Ledger lines | `done-ledger.md` |

### How to Resume — the part that matters most on a branch

Sessions are interrupted by context limits, closed windows and switched models. The branch state's
§ How to Resume is what lets a different agent pick up *any* of them, not only the one it started.

**One row per operator-initiated session**, not per agent. A session that spawns subagents lists
them briefly in one cell — `Claude Sonnet 5 (lead) + Explore, general-purpose` — rather than
getting a row each; the point is "what is this session touching", not a transcript.

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| S2 | 2026-10-19 14:02 | Claude Sonnet 5 (lead) + Explore×2 | W~1 | Active | Task 3/6: gap review | Fix `EventDetails` conflict text in `app/EventDetails.tsx`; done when `npm test -- details` passes | 2026-10-19 15:40 |
| S1 | 2026-10-19 09:10 | Codex | W~1 | Blocked — needs human | Q~1 | Re-run the deploy once Q~1 is answered | 2026-10-19 11:00 |

- **Status:** `Active` · `Paused` · `Blocked — needs human`. `Blocked` names its question.
- **Doing** follows the Progress Cell rule below: a position and an ID, not a diary.
- **Next action** is concrete — the file, the task, and how the agent will know it is done.
- **Prune aggressively.** When a session's work is recorded in § Workstreams and § Changes, delete
  its row: the Changes lines are the record of what it did, and a finished session left in the table
  makes it lie about what is in flight. A session that stops mid-task is not finished — mark it
  `Paused` so the next one knows there is unlanded work. `Active` with no matching git activity on
  the branch is a session that died without saying so.
- **Only touch your own row** while working. If another session's row looks wrong, that is a
  reconciliation finding, not something to silently correct.
- **Several people on one branch** (discouraged) share the one file. Each adds their own session
  rows; when two edits collide, keep both rows.
- **The table must be empty before the branch can be approved.**

**Remaining** lists work no session has claimed, in order. A follow-up that should outlive the
branch is marked **Handover** and written so a stranger can act on it. Anything else still open
at approval has to be finished or removed.

### How to Resume in `PROJECT_STATE.md`

On `main` the section holds the commands that reach a working state, plus an **Upcoming** table:
planned workstreams and the todos handed over at each sync, in priority order. **The first row is
the next action.** It does not track sessions, branches or other people's work — a branch's own
state does that.

| Item | Kind | Context / done when | Source | Added |
|---|---|---|---|---|
| Add the CSV export | Workstream | Spec the export format first; done when a spec is approved | handed over from `w6-item-packages` | 2026-10-19 |

Remove a row when it is dropped, or when it starts and becomes a Workstream row. An item stays
intelligible without opening its source.

### Provisional IDs

IDs minted on a branch carry `~`: `W~1`, `C~2`, `D~1`, `Q~1`. They are unique only within the file
and are replaced with the next free permanent numbers at fold, so two branches can never collide.
Cite them only inside the branch state — commit messages and code comments cannot be rewritten.
Citing an existing permanent ID is always fine. Session IDs (`S1`) are local and never folded.

### Edits versus additions

A **pure addition** (a new decision, deviation, workstream, ledger line) can never conflict with
another branch. An **edit to something already in `PROJECT_STATE.md`** (an area's *Now*, a
Workstream row, a Convention, a Known Gap) can: another branch may have changed the same entry.
Name the entry you replace, keep `Base` current, and expect sync to stop and ask if `main` moved.

## Root Document vs Linked Document

| Belongs in `PROJECT_STATE.md` | Belongs in the spec or plan |
|---|---|
| Status, what to pick up next, who is blocked on what | Requirements, schema, API shape, task breakdown |
| The shape of each area, and the decision behind it in a line or two | The full argument, alternatives, trade-off tables |
| That a deviation exists, and its consequence | The original design the deviation departs from |
| Links to specs, plans, key files | Implementation detail, code, test strategy |

**The routing rule:** if a spec or plan could hold it, summarise here and link there. If the
fact *contradicts or post-dates* every such document — a deviation, a discovered trap, an
accepted limitation, a superseding operator decision — it has no other home and belongs here
**in full**, however long that takes.

That exception is narrow. Use it for things with nowhere else to go, not as licence to move
design detail into the root file.

## Keeping It Readable

- Every entry must be **intelligible without opening its link**. Links are redirection for
  detail, never a substitute for the summary.
- Newest first in the Done ledger, Decisions, and Deviations. Within § Architecture, order areas by how
  early a newcomer needs them — data and boundaries before interface.
- Stable IDs — `W1`, `C3`, `D7`, `Q2` — unique across the file, never reused, kept when an
  entry moves between sections, so other documents and commits can cite them. They are
  allocated only by `sync-project-state`; a branch uses provisional `~` IDs.
- Target ~300 lines for Orientation, How to Resume, Workstreams and Needs a Human taken
  together — the part read every session. § Architecture grows with the system;
  §§ Decisions, Deviations and Done grow with its history. That is expected.
- **The Done ledger is a separate file** — `docs/project-state/done-ledger.md` — from the
  first entry, not once it gets long. One line per change, newest first, written there and
  nowhere else; § Record in the state file carries the link, the latest entry's date and the
  count. Deviations stay in the state file: they are read every session, while the ledger is
  opened only to answer "when did this change, and why?".
- When the ledger passes ~100 entries, or a Deviation stops being live, move it to
  `docs/project-state/archive-<YYYY>-Q<N>.md` and link it from the ledger. Archive, never
  delete — a deviation that is no longer live still explains why the code looks the way it
  does.
- Update `Last updated` (date, model or agent, and five words on what changed) on every write.
- Re-read before editing in a long session; another agent may have written to it.
