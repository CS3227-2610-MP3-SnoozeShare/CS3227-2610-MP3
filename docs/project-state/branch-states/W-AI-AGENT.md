# Branch State — W-AI-AGENT

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AI-AGENT (from `development` at `4f87cfa`)
- **Base:** `42128e6` (development tip after the W-AGENT fold and guide update; merged into this branch 2026-10-23, diff of `PROJECT_STATE.md` checked: only the Guide cell and the guide rows changed, nothing this branch edits)
- **Owner:** Xiao-Congchen
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-23 by Claude Sonnet 5.5 — spec revised after review round 1

---

**Scope.** The Support Agent's AI feature on the web: after a Guest files a ticket, an assistant asks the Guest clarifying questions in the ticket's GUEST thread and keeps a structured draft report that the Agent reads and can insert into the ticket's notes. Serves the W-AI-AGENT workstream, on top of the W-AI-GATEWAY gateway (§ 4.11).

---

## 1. Orientation

No change.

---

## 2. How to Resume

**Setup** — same as `PROJECT_STATE.md`, plus one local detail: this branch lives in a separate git worktree, `S:\CS3227\CS3227-2610-MP3-w-ai-agent` (sibling of the main checkout), because the main checkout stays on `development` while the W-AGENT fold and guide update run there. Do not switch the main checkout's branch.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| S1 | 2026-10-23 | Claude Sonnet 5.5 (lead) + four `lite-reviewer` subagents | W-AI-AGENT | Active | Spec review round 1 fixed; round 2 next | Run review round 2 (Implementer, Tester, Maintainer, Attacker) on `docs/superpowers/specs/2026-10-23-w-ai-agent-design.md`; done when no blocking finding remains and `lint.mjs spec` is clean; then ask the operator gate 1 (Q~1 first) | 2026-10-23 |

**Remaining**

- [ ] Gate 1: spec reviewed and approved (Q~1 open)
- [ ] Gate 2: plan written, reviewed and approved
- [ ] Execute the plan (`executing-plans-lite`)
- [ ] **Handover:** `docs/DeveloperGuide.md` needs the assistant (data, flow, configuration) at its guide checkpoint
- [ ] **Handover:** the live SoCLaaS check of the assistant (a real ticket, a real reply) is a post-merge step

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AI-AGENT | Ticket clarification assistant and draft report for the Support Agent | Not started | [spec](docs/superpowers/specs/2026-10-23-w-ai-agent-design.md) | — | Spec drafted, round 1 fixes in; round 2 next; Q~1 open | — |

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

| ID | What is needed | What it blocks | Raised |
|---|---|---|---|
| Q~1 | Keep or reverse a recorded decision (`AGENTS.md` § 4): `PROJECT_STATE.md` § Upcoming records "a new `AI` author role"; the spec uses the existing `SYSTEM` role (recommended: `Role.SYSTEM` exists and a new `Role` value would ripple through every switch on `Role`). Keep = add `AI`; reverse = use `SYSTEM` | Spec approval (gate 1) | 2026-10-23 |

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-23 | The assistant's report is stored in a separate AI draft field, never written into `tickets.agent_notes` by the system; the Agent inserts it into notes on request | Operator: "most flexibility". Rejected: writing straight into `agent_notes`, which the Agent also edits | Operator conversation |
| C~2 | 2026-10-23 | The assistant posts in the ticket's GUEST thread as the SYSTEM user, labelled as an automated assistant | Operator: "thats kinda like how apps do it anyway". The role value is Q~1 | Operator conversation |
| C~3 | 2026-10-23 | Guest-raised tickets only. Host-raised tickets cannot exist (`TicketServiceImpl.fileTicket` rejects non-guests, C40) | Operator chose guest-only over also covering hosts | Operator conversation |
| C~4 | 2026-10-23 | The assistant asks at submission, then answers each Guest reply, up to a fixed round cap. Rejected: submission only; Agent-requested follow-ups | Operator | Operator conversation |
| C~5 | 2026-10-23 | The shared LLM gateway is a separate workstream (W-AI-GATEWAY, done, C101); this feature registers one `AiFeature` on it | Operator | Operator conversation |
| C~6 | 2026-10-23 | This branch is cut in a separate git worktree and the main checkout stays on `development` | Operator: switching the shared checkout would complicate the W-AGENT fold that another session is running there | Operator conversation |

---

## 9. Deviations

No change.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-23 | Open the branch state with the operator's scoping decisions (C~1 to C~6) | W-AI-AGENT | `ec5a98e` |
| 2026-10-23 | Draft the ticket clarification assistant spec (index plus seven component files, High risk) | W-AI-AGENT | `78a517b` |
| 2026-10-23 | Revise the spec after review round 1: row inserted at submission, `REQUIRES_NEW` listener writes, Agent and Host text moved to the untrusted slot, draft and question screens, closing message on failure, test harness rules | W-AI-AGENT | `9921059` |
