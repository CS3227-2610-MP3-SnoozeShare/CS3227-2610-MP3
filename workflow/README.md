# workflow/ — agentic software-engineering records

Everything the AI-assisted development process produced or consumes, apart from the files that
tools force to live elsewhere. The product code is in `desktop/`, `backend/` and `frontend/`;
the published documentation is in `docs/`. The overview with owners and lifecycle is
[Developer Guide § 0](../docs/DeveloperGuide.md#0-agentic-workflow-map).

## How the workflow works

The process has one rule: **anything not written in a state file did not happen.** Agents are
swapped, context windows end, so continuity lives in files, not memory. The rules are in
[`AGENTS.md`](../AGENTS.md); the step-by-step procedure, with a flow diagram and who acts at each
step, is [`workstream-workflow.md`](workstream-workflow.md).

1. **Two state files.** [`PROJECT_STATE.md`](../PROJECT_STATE.md) describes `development`: what is
   merged, decided and handed over. Each branch has its own *branch state* in
   [`project-state/branch-states/`](project-state/branch-states) recording only what that branch
   adds or changes. Every session reads them first and updates them as it goes (a task starts or
   ends, a decision is made, a plan is revised), never in a batch at the end.
2. **Size decides ceremony.** A small change (bug fix, tweak, tests) gets a *Changes* line in the branch
   state. A big one (new capability, module, schema change, anything over one session) gets a
   **spec**, then a **plan**, before any code.
3. **Spec → plan → execute, with gates.** The agent writes the spec ([`specs/`](specs)) and it is
   checked by a linter and by read-only reviewer agents, one per lens (implementer, tester,
   maintainer, plus an attacker lens when auth, money, personal data or untrusted input is
   involved). The **owner approves** it. The plan ([`plans/`](plans)) mirrors the spec task for
   task and is approved the same way. Execution goes task by task, tests first, with a
   checklist ticked as it goes and a commit per task. Nothing is built before the spec and plan are approved.
4. **Review gate, then merge.** When every task is done and the build is green, the branch state goes to
   `In review`. The owner sets `Approved`; CI refuses a pull request into `development` otherwise
   (`.github/scripts/check-branch-states.sh`). Agents never approve, merge or enter credentials.
5. **Fold and document.** After the merge the owner runs `sync-project-state`: the branch state is
   folded into `PROJECT_STATE.md` and the [Done ledger](project-state/done-ledger.md), provisional
   IDs become permanent, and the branch state is archived. Then the Developer Guide is updated at
   the workstream's checkpoint, and `development` is released to `main` by pull request.
6. **Iterations.** When the state file grows too large or a phase ends, the owner runs
   `refresh-project-state`; the closed record moves to [`project-history/`](project-history/README.md).
7. **Session logs.** At the end of a session the agent writes a log of the material interactions to
   [`logs/`](logs), kept as evidence of how decisions were reached.

## The project's skills and agents

These are the skills and the agent written for this project. They live in `.claude/` because Claude
Code only loads them from there. Each link opens the skill's instructions.

| Skill | What it does | Used when |
|---|---|---|
| [`maintaining-project-state`](../.claude/skills/maintaining-project-state/SKILL.md) | The protocol for the state files: session start checks, branch state rules, reconciliation against git, IDs, failure modes; holds the [`PROJECT_STATE.md`](../.claude/skills/maintaining-project-state/project-state-template.md) and [branch state](../.claude/skills/maintaining-project-state/branch-state-template.md) templates | At the start and end of every session, and whenever a task or decision changes |
| [`writing-specs-lite`](../.claude/skills/writing-specs-lite/SKILL.md) | Produces a design spec (an index plus focused component files) sized to the change's risk; includes [`lint.mjs`](../.claude/skills/writing-specs-lite/scripts/lint.mjs) and the templates | Before any big change |
| [`writing-plans-lite`](../.claude/skills/writing-plans-lite/SKILL.md) | Turns an approved spec into a plan: one section per task with exact files and a checklist; [templates](../.claude/skills/writing-plans-lite/templates) | After the spec is approved |
| [`executing-plans-lite`](../.claude/skills/executing-plans-lite/SKILL.md) | Implements a plan task by task in dependency order, ticks the checklist, keeps the branch state current, lints, and closes out for owner review | After the plan is approved |
| [`sync-project-state`](../.claude/skills/sync-project-state/SKILL.md) | Folds a merged, approved branch state into `PROJECT_STATE.md` and the Done ledger, allocates permanent IDs, archives the branch state | After a branch merges into `development` |
| [`update-documentation`](../.claude/skills/update-documentation/SKILL.md) | Maintains [`docs/DeveloperGuide.md`](../docs/DeveloperGuide.md) at checkpoints: what to write, section rules, diagrams, the first write | When a workstream reaches `Done` or the owner asks |
| [`refresh-project-state`](../.claude/skills/refresh-project-state/SKILL.md) | Closes an iteration: archives the state, ledger and branch states into `project-history/` and starts a fresh `PROJECT_STATE.md` | Run by the owner only, after the guide holds the context |
| [`logging-agent-interactions`](../.claude/skills/logging-agent-interactions/SKILL.md) | Records the material user/agent interactions of a session into `workflow/logs/` | At the end of a session, when invoked |
| [`lite-reviewer`](../.claude/agents/lite-reviewer.md) (agent) | Read-only reviewer for specs and plans, dispatched once per review lens; it never edits files | During spec and plan review |

[`brainstorming`](../.claude/skills/brainstorming/SKILL.md) and
[`writing-plans`](../.claude/skills/writing-plans/SKILL.md) are copies of the general-purpose
superpowers skills, committed so the workflow is complete. They are not the default here: the
`-lite` skills above are used unless the operator names them.

## What is in this folder

| Path | What it is |
|---|---|
| [`specs/`](specs) | Design specs, one per feature-level workstream (an index file plus a sibling folder of component files). MP2-era specs are in `mp2 archive/` |
| [`plans/`](plans) | Implementation plans mirroring the specs task for task. MP2-era plans are in `mp2 archive/` |
| [`project-state/`](project-state) | [`done-ledger.md`](project-state/done-ledger.md) (every change, newest first) and `branch-states/` (one file per in-flight branch; `archive/` holds folded ones) |
| [`project-history/`](project-history/README.md) | Closed iterations of `PROJECT_STATE.md`. Not the source of truth |
| [`workstream-workflow.md`](workstream-workflow.md) | How a workstream goes from the Upcoming list to `Done`: who acts, the gates, the skills |
| [`logs/`](logs) | Agent session logs `YYYY-MM-DD_HH-mm-ss_<branch>.md` (SGT) |
| [`tools/prompt-tuning/`](tools/prompt-tuning/ticket-clarify/README.md) | Prompt-tuning kit for the ticket clarification assistant: runner, synthetic tickets, prompt variants, run changelog |
| [`MP2-feedback-triage.md`](MP2-feedback-triage.md) | MP2 grade-report triage that produced the MP3-0 workstream |

## Files that stay outside `workflow/`

They are discovered by path, so moving them would break the tool.

| Path | Why it stays |
|---|---|
| `AGENTS.md`, `CLAUDE.md` | Agents read them from the repository root |
| `PROJECT_STATE.md` | The source of truth, read first in every session; kept at the root where `AGENTS.md` points |
| `.claude/skills/`, `.claude/agents/` | Claude Code loads skills and agents from `.claude/` |
| `.github/workflows/`, `.github/scripts/check-branch-states.sh` | GitHub runs workflows from `.github/`; the script is the approval-gate check that `ci.yml` calls |
| `docs/` | The Jekyll site published to GitHub Pages |
| `.superpowers/sdd/` | One progress ledger from a subagent-driven execution (Host messages UI, 2026-09-27); a one-off, so it is not part of the workflow records |

Paths inside the older specs, plans, logs and archived branch states are history: where a record
names an old location (for example `docs/superpowers/`), read it as the matching folder here.
