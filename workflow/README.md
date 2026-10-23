# workflow/ — agentic software-engineering records

Everything the AI-assisted development process produced or consumes, apart from the files that
tools force to live elsewhere. The product code is in `desktop/`, `backend/` and `frontend/`;
the published documentation is in `docs/`. The overview with owners and lifecycle is
[Developer Guide § 0](../docs/DeveloperGuide.md#0-agentic-workflow-map).

| Path | What it is |
|---|---|
| [`specs/`](specs) | Design specs, one per feature-level workstream (an index file plus a sibling folder of component files). MP2-era specs are in `mp2 archive/` |
| [`plans/`](plans) | Implementation plans mirroring the specs task for task. MP2-era plans are in `mp2 archive/` |
| [`project-state/`](project-state) | [`done-ledger.md`](project-state/done-ledger.md) (every change, newest first) and `branch-states/` (one file per in-flight branch; `archive/` holds folded ones) |
| [`project-history/`](project-history/README.md) | Closed iterations of `PROJECT_STATE.md`. Not the source of truth |
| [`workstream-workflow.md`](workstream-workflow.md) | How a workstream goes from the Upcoming list to `Done`: who acts, the gates, the skills |
| [`logs/`](logs) | Agent session logs `YYYY-MM-DD_HH-mm-ss_<branch>.md` (SGT), written with the `logging-agent-interactions` skill |
| [`tools/prompt-tuning/`](tools/prompt-tuning/ticket-clarify/README.md) | Prompt-tuning kit for the ticket clarification assistant: runner, synthetic tickets, prompt variants, run changelog |
| [`sdd/`](sdd) | Progress ledger from a subagent-driven execution (Host messages UI, 2026-09-27) |
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

Paths inside the older specs, plans, logs and archived branch states are history: where a record
names an old location (for example `docs/superpowers/`), read it as the matching folder here.
