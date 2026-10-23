# AGENTS.md

Instructions for any AI agent working in this repository. Read this before touching anything.

## 1. `PROJECT_STATE.md` is the source of truth

The operator may switch between models, agents, and context windows. None of that history survives; the state files do.

**Branches.** Work flows feature branch → `development` → `main`. `development` is the integration (default)
branch: it accepts direct pushes and pull requests. `main` is the release branch and only changes by pull
request from `development`. `checkstyle` and `build` are required status checks on both. The skills
in `.claude/skills/` call the integration branch `INT` (see `maintaining-project-state` § Branch Model);
in this project `INT` is **`development`**: the branch a branch state is merged into and folded on,
and where `PROJECT_STATE.md` and the guide are written.

| | `PROJECT_STATE.md` | Branch state |
|---|---|---|
| **Path** | repo root | `workflow/project-state/branch-states/<slug>.md` (`<slug>` = branch name, `/` becomes `-`) |
| **Describes** | `development`: what is merged there, decided and handed over (`main` receives it by release PR) | what one branch has added or changed |
| **Written by** | the `sync-project-state` skill (the owner may hotfix it by hand) | every agent working on that branch |

**Anything not written in them did not happen.** On a branch, `PROJECT_STATE.md` is read-only.

- **Before any work** — including answering questions or reading code — read
  `PROJECT_STATE.md` in full, and on a branch read its branch state in full (create it from
  `.claude/skills/maintaining-project-state/branch-state-template.md` if it is missing). Then run
  `git log --oneline -20` and `git status` to confirm they still match reality. Correct any drift you find before acting on the files.
- **Never reconstruct project state from source.** Code shows what is; it never shows what was decided, what was rejected, or what is half-built.
- **When the document and the repo disagree:** the repo wins on what the code does; the document wins on what was decided and why. Never silently edit a decision to match a stray commit — that erases the only record the decision existed.
- **If the file does not exist**, create it from
  `.claude/skills/maintaining-project-state/project-state-template.md`, populating it from `workflow/specs/`, `workflow/plans/`, git history, and the source tree. Then tell the operator what you inferred and what you guessed.
- **It is an index, not a library** — with one exception. Status, decisions and pointers live there; depth lives in the specs and plans it links to. But a fact that contradicts or post-dates every spec and plan — a deviation, a discovered trap, an accepted limitation — has nowhere else to live and belongs in the document in full.

## 2. Update it as you go, not at the end

On a branch, write to **the branch state** immediately when any of these happen. On `development`, the
owner's hotfixes aside, `PROJECT_STATE.md` changes only when `sync-project-state` folds a branch
in.

1. A workstream starts, blocks, passes review, finishes, or is abandoned — and whenever a task
   starts, finishes, or blocks, so the Progress cell stays current.
2. A session starts, changes what it's doing, spawns or retires an agent, or ends — update its
   row in the branch state's § How to Resume immediately, including its **Next action**; delete
   the row once its work is reflected in § Workstreams and § Changes. This is how multiple
   sessions or agents on the same branch hand off without duplicating work.
3. The shape of the system changes — a new part, a moved responsibility, a changed boundary or
   data model — write the area's proposed **Now** in the branch state's § Architecture in the
   same write.
4. The operator makes or changes a decision affecting design, scope, or priorities — record
   *who asked and why*, and what was rejected, not just the outcome. Use a provisional ID (`C~1`).
5. A spec or plan is created or materially revised — link it from its workstream.
6. Work completes, or reality diverges from a plan — a Changes line (copied to the Done ledger
   at fold), status, and Deviations.
7. The session is ending or context is running low — reconcile your session row and § Remaining
   so a different session can take over cold. Mark anything that should outlive the branch
   **Handover**.
8. You merge `development` into the branch — diff `PROJECT_STATE.md` since the branch state's `Base`,
   re-check every area and entry the branch edits, then move `Base` to the new `development` sha.
9. The branch is finished — empty the session table, set `Status: In review`, and tell the
   project owner.

Batching these until the end of a session loses them, because sessions end unpredictably. **A
task is not complete until the branch state reflects it.**

**Approval gate.** Branch state `Status` runs `Draft` → `In review` → `Approved` → `Folded`. Only
the project owner sets `Approved`, with their name and the date; an agent never does on its own initiative, only by writing down the owner's yes (§ 8 Workflow gates). Do not merge
a branch into `development` until it is `Approved` and its session table is empty. After the merge the owner runs
`sync-project-state` (§ 8: the agent asks first); a file still in `branch-states/` on `development` means a fold is pending.

**IDs.** Permanent IDs (`C14`) are allocated only by `sync-project-state`. A branch mints
provisional `~` IDs and never cites one outside its own branch state — not in a commit message or a
code comment.

**The acid test, before you stop:** could a fresh agent, given only `PROJECT_STATE.md` and this
branch's state, take over any session in § How to Resume and take its Next action correctly,
without asking a question? If not, fix the file.

Status vocabulary, used verbatim so it stays greppable:
`Not started` · `Spec'd` · `Planned` · `Building` · `Blocked — needs human` · `In review` ·
`Done` · `Abandoned`

**Two rules agents get wrong.** § Architecture is structured by the system, not by history: one
subsection per real part — data, a boundary such as auth or tenancy, a core engine, the
interface — each opening with what that part *is today*, followed by a table of the decisions
that shaped it. Decisions that changed how the work is done, not the system, stay in
§ Decisions; each decision appears once and keeps its ID. And the Workstream **Progress** cell
is a position, not a diary: at most ~15 words — where in the plan, what is in flight, and the
ID of whatever blocks or explains it (`Task 6/9 in flight: conflict engine`, `Task 3/8 stopped:
Q2`). What was built belongs in the plan and the Done ledger; why it changed belongs in
§ Decisions or § Deviations.

Cite sections by name — `§ Known Gaps` — not by number, so the file can be reordered.

## 3. Big changes need a spec and a plan. Every change gets recorded.

| Size | Examples | Before coding | After |
|---|---|---|---|
| **Big** — feature level and above | New user-facing capability; new module, service, or page; schema or data-model change; new dependency or architectural shift; cross-cutting refactor; anything spanning more than one session | A design spec at `workflow/specs/YYYY-MM-DD-<topic>-design.md`, an implementation plan at `workflow/plans/YYYY-MM-DD-<topic>.md`, and a Workstream row in the branch state linking both | Workstream status + a Changes line, both in the branch state |
| **Small** | Bugfix within an existing design; copy or style tweak; single-function refactor; added tests; config change | Nothing — just do it | A Changes line in the branch state |

**Ceremony scales with size; the written record does not.** A one-line fix needs no spec and no
plan, but it still gets a Changes line — and if it diverges from a plan, contradicts a spec, or
leaves a trap for the next agent, it also gets a Deviations entry. Only true no-ops
(formatting, a typo in a comment) pass unwritten.

Ambiguous size? Treat it as big.

**Default skills.** Write the spec with `writing-specs-lite`, the plan with `writing-plans-lite` and implement it
with `executing-plans-lite` (`.claude/skills/`). Do not use `brainstorming`, `writing-plans`,
`reviewed-plan`, `executing-plans` or `subagent-driven-development` unless the operator names them.
Specs and plans are reviewed by the read-only `lite-reviewer` agent and checked with
`.claude/skills/writing-specs-lite/scripts/lint.mjs`. The spec is an index file at the path above plus a
sibling folder of component files; the plan mirrors the spec's implementation order task for task.

If the operator asks to skip the spec and plan on a big change, say so once, then do as asked —
it is their call — and record the skip as a decision in the branch state, so the missing document is visible to
whoever inherits the work.

## 4. Do not "fix" deliberate omissions

`PROJECT_STATE.md` § Known Gaps lists known gaps and accepted limitations. Those are decisions,
not a TODO list. If one looks wrong, raise it in § Needs a Human; do not quietly implement it.

**If an operator request collides with a recorded decision** — a § Known Gaps entry, the
out-of-scope list, or a decision in § Architecture or § Decisions — stop and confirm before
doing anything. Name the entry and ask which:
**keep the decision** (do not build it), or **reverse it** (build it, record a decision
reversing the entry, update or remove the entry, and open a new workstream if it falls outside
the current one). Act on neither until they answer, even if they said "just do it" — they may
not know the entry exists. This overrides the skip-the-spec rule in § 3 above. If the operator
is unavailable, stop and put the question in § Needs a Human.

Durable technical conventions live in § Conventions, and the current shape of each part of the
system in § Architecture. Read both before writing code, and add to them when you discover
something the next agent would otherwise learn the hard way.

## 5. Developer guide

`docs/DeveloperGuide.md` is the handover brief for developers taking over the project — scope,
goals, requirements, architecture, key decisions, diagrams, how to test. It holds only confirmed
work and changes only at checkpoints, so it may lag. **It never directs coding:** if it disagrees
with `PROJECT_STATE.md`, the state file wins. Do not edit the guide to fix it outside a checkpoint;
add `guide out of date: <what>` to the workstream's Progress instead. **The guide is written only on
`development`** (or on a branch cut from it for review), never on a feature branch: a branch leaves a workstream's Guide at `—` and notes `guide out of date`
in its branch state.

- **Developer guide checkpoint:** `feature`
  (`feature` = each workstream once it is `Done` and the operator confirms it ·
  `phase` = when the operator declares a phase transition · `manual` = only on request)
- **Developer guide optional sections:** `none`
  (`none` · `user stories` · `use cases` · `user stories, use cases`)

**First write:** when the project's first spec is approved and no guide exists, propose seeding
it with only what the spec settles — overview, scope, requirements, glossary. No architecture,
no diagrams.

**Compare the guide to the built app at every checkpoint.** Before proposing the update, check
each claim you are about to keep or add against the code (defaults, filters, tab and screen names,
limits), not against the spec or a past version of the guide. The MP2 guide drifted on exactly
these points.

**When a workstream reaches `Done`** (at fold, or directly on `development`): set its **Guide** column to `Awaiting confirmation` (or `N/A`,
with the reason in Progress) and ask the operator whether it is confirmed; a yes makes it `Pending`.
At each checkpoint, **propose** the update — the sections and diagrams to add or change — and
**write nothing until the operator approves**; then set Guide to `Documented YYYY-MM-DD`. If the
operator holds it, it stays `Pending` with `guide held YYYY-MM-DD: <reason>` in Progress. Guide
values: `—` · `Awaiting confirmation` · `Pending` · `Documented YYYY-MM-DD` · `N/A`.

---

## 6. Keep commits small and contained

Reviewers cannot audit a 96-file commit, and a broken integration is hard to bisect. Past
integration commits touched 55–96 files and needed a follow-up repair. So:

- **One logical change per commit** — a fix, a refactor, a doc update, a test. Do not mix a
  behaviour change with unrelated formatting or cleanup.
- **Keep each commit a reviewable logical unit, not a line count.** Do not pile hundreds of
  changed lines across many files or several unrelated behaviours into one commit, and do not
  chop a cohesive change (a feature with its tests, one rule with its fixtures) into fragments
  that mean nothing alone just to stay small. When a commit grows large, ask whether it holds
  two logical changes; if it does, split there.
- **Each commit builds and passes tests** (the build command of each component you changed, from `PROJECT_STATE.md` § How to Resume). Never commit a known-red tree to
  "fix it in the next commit".
- **Aim for a handful of files per commit.** If a change touches more than about 15 files,
  stop and split it (extract the refactor first, then the behaviour change), unless it is a
  mechanical rename or a generated file.
- **Commit as you go, per plan task** — not once at the end of a session or workstream.
- **Verify in proportion.** After a change, run only the test classes it touches, with the
  warm Gradle daemon (no `--no-daemon`), and batch several related tests per run. Run the full
  suite and Checkstyle once per task, before its final tick, and the full build at each wave
  and at close-out. Keep spy and mock beans in a shared test base class so tests reuse one
  Spring context.
- **Integrate branches in small steps.** Merge or rebase a feature branch before it grows
  large; resolve API drift at the merge, in the merge commit, with the build run first. Do not
  land stale-API repairs as a separate "fix after integration" commit.
- **Write descriptive messages:** `type: what changed and why` (`fix:`, `feat:`, `refactor:`,
  `docs:`, `test:`), naming the area or item ID. Never `update`, `wip`, or "record origin/main
  as merged".
- **Keep the record in step:** update the branch state (and, on `development`, `PROJECT_STATE.md` and the Done ledger) in the same commit
  as the work they describe, or the very next one.

---

*Claude Code users: the full protocols are the skills in `.claude/skills/` —
`maintaining-project-state` (reconciliation, worked examples, failure modes), `sync-project-state`
(folding a merged branch state into `PROJECT_STATE.md`), `refresh-project-state` (closing an
iteration and archiving the record) and `update-documentation` (checkpoints, section rules,
diagrams). This file is the portable subset.*

## 7. Ignore `workflow/project-history/`

Closed iterations of the project state are archived in `workflow/project-history/` by the
`refresh-project-state` skill. They are not the source of truth. The skill is run by hand by the
project owner, on `development`, and only after the Developer Guide holds the context being archived
(guide first, archive second). Agents never start a refresh on their own initiative.

- **Never read `workflow/project-history/` at session start**, and never take direction from it.
  Open it only to answer "why was X decided?".
- **IDs continue across iterations.** `PROJECT_STATE.md` § Conventions records the highest ID
  used; never reuse one.

## 8. Workflow gates: the agent asks, the operator decides

The operator must never have to remember which skill comes next. Work moves through the gates
below in order. At each one the agent **stops, says what is ready, and asks one question**; it
does not start the next stage until the operator answers yes in chat. Silence, "looks fine so far",
a yes to a different question, or a clean `lite-reviewer` / `code-review` report is not a yes. If
the operator is unavailable, stop and put the question in § Needs a Human (the same rule as § 4).

**Recording a yes.** On a yes the agent writes the approval marker itself, in the same write as the
stage change: `Approved (<operator name>, <YYYY-MM-DD>)`, taking the name from the chat (ask if it
is not known). That transcribes the operator's decision; it is not the agent's approval. A
change after approval that is more than a typo (the spec, plan or branch changes in behaviour,
scope or tasks) drops the marker back to `In review` and the agent asks again. A "no" or "change
X" keeps the stage open: fix it, then ask again. Small changes (§ 3) skip gates 1 and 2 only.

| Gate | The agent asks when | The question names | On yes | Not before |
|---|---|---|---|---|
| **1. Spec** | The spec files are written, `lint.mjs spec` passes, the review rounds are done, no `Q` is open | The spec path, risk tier, lenses run, decisions and rejected options | Spec index `Status: Approved (...)`; workstream `Spec'd`; Changes line; then start `writing-plans-lite` | Any plan |
| **2. Plan** | The plan is written, `lint.mjs plan` passes, the reviewer round is done | The plan path, task count and waves, `GATE:` and `Post-merge` tasks | Plan `Status: Approved (...)` (add the line under the plan's title if it is missing); workstream `Planned`; then start `executing-plans-lite` | Any code |
| **3. Review** | Every task is `Done` (or `Post-merge`), the build is green, the code review is run, and every known bug is fixed or recorded as a Deviation or Handover item; a state that is reasonable to hand over counts, but say what is left | What was built, the checks run and not run, open Deviations and Handover items | Branch state `Status: Approved`, `Approved: <name> <date>`; then ask separately whether to push and open the PR | Pushing, opening a PR, merging |
| **4. Sync** | The branch is merged into `development` (the operator says so, or a non-archived branch state is found there at session start) | The branch state and the fold it would run | Run `sync-project-state` for it on `development`, or on a `sync/<slug>` branch if the operator prefers a reviewed fold; commit, never push | Any fold |
| **5. Fold** | The fold commit exists | The ID map, sections touched, Handover items now in Upcoming | The operator's yes confirms the changes; then ask whether each folded `Done` workstream is confirmed for the guide | Any guide work |
| **6. Guide** | The operator confirmed the workstream (Guide `Pending`) | The proposed sections and diagrams, with each claim checked against the code (§ 5) | Write the guide, set Guide `Documented YYYY-MM-DD`, add the Done ledger line; on a hold, keep `Pending` and note `guide held YYYY-MM-DD: <reason>` | Writing the guide |

- **Gate 3 is the review that unblocks the PR.** The agent sets the branch state to `In review`
  with an empty session table and asks the operator to review. The agent never pushes, opens a PR
  or merges before the `Approved` line is written. The CI check (`check-branch-states.sh`) backs this up
  and does not replace it: it fails a PR into `development` whose branch state is not `Approved`, has live session rows, or is missing (a `sync/*` fold PR is exempt), so the review has to happen first.
- **"By hand" in the sync, refresh and update skills means the operator's yes in chat.** The agent
  asks and then runs `sync-project-state` or `update-documentation`; the operator does not have to
  invoke them. `refresh-project-state` is still never started by an agent (§ 7).
- **At the start of every session** the agent finds where the work sits and raises any open gate
  first: a spec or plan awaiting approval, a branch `In review`, a merged branch still in
  `branch-states/`, a Guide cell of `Awaiting confirmation` or `Pending`. A held gate is mentioned
  once per session, not repeated.
