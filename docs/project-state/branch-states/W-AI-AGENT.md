# Branch State — W-AI-AGENT

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AI-AGENT (from `development` at `4f87cfa`)
- **Base:** `42128e6` (development tip after the W-AGENT fold and guide update; merged into this branch 2026-10-23, diff of `PROJECT_STATE.md` checked: only the Guide cell and the guide rows changed, nothing this branch edits)
- **Owner:** Xiao-Congchen
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-23 by Codex — plan written, lint clean and reviewed; gate 2 next

---

**Scope.** The Support Agent's AI feature on the web: after a Guest files a ticket, an assistant asks the Guest clarifying questions in the ticket's GUEST thread and keeps a structured draft report that the Agent reads and can insert into the ticket's notes. Serves the W-AI-AGENT workstream, on top of the W-AI-GATEWAY gateway (§ 4.11).

---

## 1. Orientation

Row added to `PROJECT_STATE.md` § Orientation:

| Path | What lives there |
|---|---|
| `tools/prompt-tuning/ticket-clarify/` | Prompt tuning kit for the ticket clarification assistant (W-AI-AGENT): `run.mjs` (Node 22, offline `--selftest`), 23 synthetic tickets, prompt variants, `CHANGELOG.md` of runs (results are git-ignored). Run by hand with a SoCLaaS key held in the terminal; synthetic data only. It is outside `docs/`, so the Pages site does not publish it |

---

## 2. How to Resume

**Setup** — same as `PROJECT_STATE.md`. This branch is checked out in the main checkout (`S:\CS3227\CS3227-2610-MP3`). It was briefly kept in a sibling git worktree while the W-AGENT fold ran on `development`; the operator ended that on 2026-10-23 (C~9). A leftover empty folder `S:\CS3227\CS3227-2610-MP3-w-ai-agent` may remain on disk until a process releases it; it is not tracked and can be deleted.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|

**Remaining**

- [x] Gate 1: spec reviewed and Approved (Xiao-Congchen, 2026-10-23)
- [ ] The operator tunes the prompt with `tools/prompt-tuning/ticket-clarify/` in parallel with the build; the committed prompt needs a `KEEP` row in that kit's `CHANGELOG.md` before merge (spec conventions R15)
- [ ] Gate 2: plan written and reviewed, awaiting operator approval. Ask approval of `docs/superpowers/plans/2026-10-23-w-ai-agent.md` (10 tasks, five waves). On yes write Approved (Xiao-Congchen, date), lint plan --approved, mark workstream Planned, then start executing-plans-lite. No implementation before that yes.
- [ ] Execute the plan (`executing-plans-lite`)
- [ ] **Handover:** `docs/DeveloperGuide.md` needs the assistant (data, flow, configuration) at its guide checkpoint
- [ ] **Handover:** the live SoCLaaS check of the assistant (a real ticket, a real reply) is a post-merge step

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AI-AGENT | Ticket clarification assistant and draft report for the Support Agent | Spec'd | [spec](docs/superpowers/specs/2026-10-23-w-ai-agent-design.md) | [plan](docs/superpowers/plans/2026-10-23-w-ai-agent.md) | Plan reviewed without blockers; gate 2 approval next | — |

---

## 4. Architecture

No change: nothing is built yet. The spec proposes a new area, the ticket assistant, which the plan's execution will describe here.

---

## 5. Conventions

No change: nothing is built yet.

---

## 6. Known Gaps

No change: nothing is built yet.

---

## 7. Needs a Human

No change: Q~1 was raised and resolved on this branch (C~7).

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~10 | 2026-10-23 | Approved (Xiao-Congchen, 2026-10-23): retain High risk and approve the spec at gate 1; proceed to plan writing | Operator: "ok fine approve, proceed", after the High-risk rationale and explicit spec approval question. Medium rejected because personal data triggers C67 and feature-specific output and concurrency risks remain | Operator conversation |
| C~1 | 2026-10-23 | The assistant's report is stored in a separate AI draft field, never written into `tickets.agent_notes` by the system; the Agent inserts it into notes on request | Operator: "most flexibility". Rejected: writing straight into `agent_notes`, which the Agent also edits | Operator conversation |
| C~2 | 2026-10-23 | The assistant posts in the ticket's GUEST thread as the SYSTEM user, labelled as an automated assistant | Operator: "thats kinda like how apps do it anyway". The role value is Q~1 | Operator conversation |
| C~3 | 2026-10-23 | Guest-raised tickets only. Host-raised tickets cannot exist (`TicketServiceImpl.fileTicket` rejects non-guests, C40) | Operator chose guest-only over also covering hosts | Operator conversation |
| C~4 | 2026-10-23 | The assistant asks at submission, then answers each Guest reply, up to a fixed round cap. Rejected: submission only; Agent-requested follow-ups | Operator | Operator conversation |
| C~5 | 2026-10-23 | The shared LLM gateway is a separate workstream (W-AI-GATEWAY, done, C101); this feature registers one `AiFeature` on it | Operator | Operator conversation |
| C~6 | 2026-10-23 | This branch is cut in a separate git worktree and the main checkout stays on `development` | Operator: switching the shared checkout would complicate the W-AGENT fold that another session is running there | Operator conversation |
| C~7 | 2026-10-23 | Reverse the recorded "new `AI` author role": the assistant uses the existing `SYSTEM` role and user, and the frontend labels every `SYSTEM` message in a ticket thread "Snoozy · Automated assistant". Convention to fold: a SYSTEM-authored ticket-thread message means the automated assistant. Supersedes the Upcoming row "W-AI-AGENT scoping decisions" (its `AI` role part; amends C~2) | Operator, after `AGENTS.md` § 4 confirmation: the frontend can label the messages, and a new `Role` value would ripple through every switch on `Role`. Rejected: an `AI` value (second seeded user, widened `users.role` CHECK, desktop enum copy) | Operator conversation |
| C~8 | 2026-10-23 | The assistant has a light persona: UI label "Snoozy · Automated assistant"; the prompt asks for a calm, plain, friendly voice for the questions only (no self-introduction, emojis, markdown, apologies or comfort promises) and neutral third-person wording in every other field; English only (the screens are English patterns). The prompt is tuned by the operator with a kit in `tools/prompt-tuning/ticket-clarify/` | Operator chose "named, light touch". Rejected: no persona; a full personality (a playful voice reads as dismissive in a dispute and widens roleplay jailbreaks; safety rests on the schema and the screens) | Operator conversation |
| C~9 | 2026-10-23 | Work returns to the main checkout: the sibling worktree is retired and the main checkout switches from `development` to this branch. Ends C~6 | Operator: the W-AGENT fold on `development` is done, so the shared checkout is free. Rejected: keeping the worktree | Operator conversation |

---

## 9. Deviations

- **D~1 (planning, 2026-10-23):** Existing notes save/textarea limits are 4,000 characters; approved drafts allow 6,000 and insertion appends existing notes. Plan preserves full local insertion and existing server validation, so an Agent may need to edit down before saving. No silent truncation or limit increase. Carry this UX limitation to review/Handover.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-23 | Finish plan: 10 tasks/five waves, all 85 E rows mapped; read-only lite-reviewer Implementer lens found no blockers/questions. Clarify inherited fixture ordering, broaden legacy-suite opt-out to PostgresIntegrationTest, and link spec to plan. Spec/plan lint clean; source/build tests not run because only planning documents changed. Session and reviewer retired; gate 2 next | W-AI-AGENT | This session |
| 2026-10-23 | Draft T1–T10 implementation plan across five waves with all 85 edge cases traced; verify repository seams and run clean plan lint; begin required read-only lite-reviewer round | W-AI-AGENT | This session |
| 2026-10-23 | Record gate 1 spec approval by Xiao-Congchen and start plan writing; implementation remains gated on plan approval | W-AI-AGENT | This session |
| 2026-10-23 | Assess operator's proposed Medium tier: recommend retaining High because C67 covers personal data and the feature adds draft visibility, automated messages and transactional state; gateway controls leave steered-prose residual risk. Spec lint: zero errors/warnings. Gate 1 remains open; no plan written | W-AI-AGENT | This session |
| 2026-10-23 | Open the branch state with the operator's scoping decisions (C~1 to C~6) | W-AI-AGENT | `ec5a98e` |
| 2026-10-23 | Draft the ticket clarification assistant spec (index plus seven component files, High risk) | W-AI-AGENT | `78a517b` |
| 2026-10-23 | Apply the persona (SD21, C~8) to the spec: label, prompt rules P8 to P12, conventions R15; contractions need their apostrophe in list A (`well` and `ill` are ordinary words) | W-AI-AGENT | `92f4163` |
| 2026-10-23 | Add the prompt tuning kit: runner that mirrors the gateway layout and the screens, 23 synthetic tickets, three prompt variants, README and changelog; offline self test (35 checks) and a mock-server end-to-end run pass | W-AI-AGENT | `c861943` |
| 2026-10-23 | Record the reversal to the SYSTEM author role and close Q1 (SD20, C~7) | W-AI-AGENT | `15e2a7d` |
| 2026-10-23 | Fix review round 2 findings: Host and Agent text removed from the prompt, NFKC-cleaned screens with wider lists, a job never runs on the submitting thread, stuck-row stop path, exact audit rows, test-harness rules, closing text made non-committal | W-AI-AGENT | `585cb15` |
| 2026-10-23 | Revise the spec after review round 1: row inserted at submission, `REQUIRES_NEW` listener writes, Agent and Host text moved to the untrusted slot, draft and question screens, closing message on failure, test harness rules | W-AI-AGENT | `9921059` |
