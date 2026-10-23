# Branch State — workflow-folder-consolidation

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `INT`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** workflow-folder-consolidation (from `INT` at `2f83c4e`)
- **Base:** 2f83c4e
- **Owner:** Xiao-Congchen
- **Status:** Approved
- **Approved:** Xiao-Congchen 2026-10-23
- **Last updated:** 2026-10-23 by Claude Sonnet 5.5 — owner approval recorded

---

**Scope.** The project instructions ask that all files of the agentic-SE workflow sit in one dedicated folder. This branch moves the workflow records into `workflow/`, repoints every reference, adds an overview section at the start of the Developer Guide, and fixes the broken GitHub Pages links (the User Guide was published at `/UserGuide.html` while the home page and navigation linked `/user-guide/`).

---

## 1. Orientation

Replace these rows of `PROJECT_STATE.md` § Orientation (the paths are the only change except the new `workflow/` row), and apply the same path rewrite to every other mention in the file (`docs/superpowers/` → `workflow/specs/` and `workflow/plans/`, `docs/project-state/` → `workflow/project-state/`, `docs/project-history/` → `workflow/project-history/`, `docs/MP2-feedback-triage.md` → `workflow/MP2-feedback-triage.md`, `tools/prompt-tuning/` → `workflow/tools/prompt-tuning/`, `logs/` → `workflow/logs/`):

| Path | What lives there |
|---|---|
| `workflow/` | The agentic-SE records, described in [workflow/README.md](../../README.md) and Developer Guide § 0: `specs/`, `plans/` (MP2-era ones in `mp2 archive/`), `project-state/` (Done ledger, `branch-states/`, `archive/`), `project-history/`, `workstream-workflow.md`, `logs/` (session logs `YYYY-MM-DD_HH-mm-ss_<branch>.md`, SGT), `tools/prompt-tuning/` (the ticket clarification prompt kit; run by hand, synthetic data only), `MP2-feedback-triage.md` |
| `docs/DeveloperGuide.md`, `docs/UserGuide.md` | Handover brief (starts with § 0, the workflow map) and end-user walkthroughs (Jekyll site, published by `jekyll-gh-pages.yml`; User Guide at `/user-guide/`) |

The rows for `docs/superpowers/…`, `docs/project-state/`, `docs/project-history/`, `docs/MP2-feedback-triage.md`, `tools/prompt-tuning/` and `logs/` are removed; their content is the `workflow/` row. Files that stay by path: `AGENTS.md`, `CLAUDE.md`, `PROJECT_STATE.md`, `.claude/`, `.github/`, `docs/`.

---

## 2. How to Resume

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|

**Remaining**

- [ ] **Handover:** at fold, rewrite the old workflow paths in `PROJECT_STATE.md` (see § Orientation above); this branch left that file unedited because it is read-only on a branch, so its links to specs, plans, branch states and `docs/project-history/` are stale until then.
- [ ] **Handover:** after the release PR from `development` to `main`, open the published site and confirm `/user-guide/` loads with its images, the home-page links and the navigation work (Pages deploys only on pushes to `main`).

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W~1 | Consolidate the agentic-workflow files into `workflow/`, add Developer Guide § 0, fix Pages links | Done | — (skipped, C~1) | — (skipped, C~1) | Moves, references, guide § 0 and Pages fix done; awaiting owner review | N/A: § 0 was written on this branch at the operator's request (C~2) |

---

## 4. Architecture

No change: this branch moves documentation and process files and edits docs and CI path filters; no product code changes.

---

## 5. Conventions

Add to `PROJECT_STATE.md` § Conventions:

- Agentic-workflow records live under `workflow/` (specs, plans, project state and history, session logs, the prompt-tuning kit). Files that tools discover by path stay outside it: `AGENTS.md`, `CLAUDE.md`, `PROJECT_STATE.md`, `.claude/`, `.github/` and the published `docs/`. A new workflow artefact goes in `workflow/` unless a tool forces a path. Developer Guide § 0 is the index and is updated when a location is added or moved.
- Pages-published documents that link to each other use their `permalink` URLs (`../developer-guide/`, `../user-guide/`) or `.html` paths, never `.md` paths, and images from a page served at a directory permalink use `../images/…`. Links to repository files outside `docs/` use GitHub URLs.

---

## 6. Known Gaps

No change: nothing deliberate is added or reversed. Historical specs, plans, logs and archived branch states still name the old locations in prose (for example `docs/superpowers/`); `workflow/README.md` says to read those as the matching `workflow/` folder.

---

## 7. Needs a Human

No change.

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-23 | Skip the spec and plan for this change (`AGENTS.md` § 3 says ambiguous size is big) | The operator asked for the consolidation to be done directly and to record the skip as a decision here. The change is mechanical renames plus reference rewrites, with no behaviour change | Operator conversation |
| C~2 | 2026-10-23 | Write Developer Guide § 0 on this feature branch, although `AGENTS.md` § 5 says the guide is written only on `development` | The operator asked for the section in the same change as the move so the overview ships with the folder. The Guide column stays `N/A`; the section describes the layout and was checked against the repository, not against a built feature | Operator conversation |
| C~3 | 2026-10-23 | Workflow records move to `workflow/`: `docs/superpowers/{specs,plans}` → `workflow/{specs,plans}`; `docs/project-state`, `docs/project-history`, `logs/`, `docs/runbooks/workstream-workflow.md`, `docs/MP2-feedback-triage.md`, `tools/prompt-tuning/` (→ `workflow/tools/prompt-tuning/`). Excluded at the operator's request, left in place as a one-off with no consistent record: `.superpowers/sdd/`. Stay in place: `AGENTS.md`, `CLAUDE.md`, `PROJECT_STATE.md`, `.claude/`, `.github/`, `docs/Reflections.md` and the other published docs, `docs/runbooks/dev-environment.md` | Operator asked for a dedicated folder for everything related to the development workflow, "unless it is reasonable / natural for them to be elsewhere"; tooling discovers `.claude/`, `.github/` and the root instruction files by path. `docs/Reflections.md` is a published graded document, and the dev-environment runbook is deployment, not workflow. The operator confirmed the `tools/` and `logs/` choices | Operator conversation |
| C~4 | 2026-10-23 | The User Guide gets `permalink: /user-guide/` (replacing an unused `nav_order`) instead of changing the links to `/UserGuide.html` | The home page and navigation already used `/user-guide/`, matching the other pages' permalinks | Operator conversation |

---

## 9. Deviations

| ID | Deviation / discovery |
|---|---|
| D~1 | `PROJECT_STATE.md` was not edited (read-only on a branch). Its links and path mentions are stale until the fold applies § Orientation above. A first automated rewrite touched it and was reverted |
| D~2 | The link repair for moved files also corrected links that were already broken before the move (branch-state links written relative to the wrong folder). A few remain: `archive/2026-10-20-W-INFRA.md` points at a plan path that does not resolve, and `edge-cases.md` in the AI gateway spec contains two `b` placeholders; both were broken before |
| D~3 | `lint.mjs plan` reports four errors on the completed W-AI-AGENT plan (a Create path that now exists, a Modify path whose branch state is archived). Those are expected for a finished plan and are not new |
| D~4 | The Pages site was not rebuilt locally (no Jekyll here); the fix is checked by reading the links against the permalinks and the live 404s found before the change |

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-23 | Moved the workflow records (specs, plans, project state and history, logs, workstream runbook, MP2 triage, prompt-tuning kit, SDD ledger) into `workflow/` with `git mv` | W~1 | `refactor: move workflow records into workflow/` |
| 2026-10-23 | Repointed path references in skills, `AGENTS.md`, README, CI (branch-state check path and `workflow/**` in push `paths-ignore`), Jekyll excludes and the moved records; repaired relative links between moved files | W~1 | this branch |
| 2026-10-23 | Added `workflow/README.md` and Developer Guide § 0 (workflow map) | W~1 | this branch |
| 2026-10-23 | Fixed GitHub Pages: User Guide permalink `/user-guide/`, its image paths, links to the Developer Guide, runbook and README | W~1 | this branch |
| 2026-10-23 | Left the one-off SDD ledger out of `workflow/` (restored to `.superpowers/sdd/`) at the operator's request; rewrote `workflow/README.md` with an overview of how the workflow runs and linked, described entries for each project skill and the reviewer agent | W~1 | this branch |
