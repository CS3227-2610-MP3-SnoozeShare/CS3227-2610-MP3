# Branch State — W-AI-AGENT

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AI-AGENT (from `development` at `4f87cfa`)
- **Base:** `42128e6` (development tip after the W-AGENT fold and guide update; merged into this branch 2026-10-23, diff of `PROJECT_STATE.md` checked: only the Guide cell and the guide rows changed, nothing this branch edits)
- **Owner:** Xiao-Congchen
- **Status:** Draft
- **Approved:** —
- **Last updated:** 2026-10-23 by Codex — SD22 spec approved; revised plan lint clean/reviewed; gate 2 next

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

- [x] Original gate 1: Approved (Xiao-Congchen, 2026-10-23), before SD22.
- [x] SD22 gate 1: Approved (Xiao-Congchen, 2026-10-23), operator: "approve and continue building". Revised-plan gate 2 remains separate.
- [ ] The operator tunes the prompt with `tools/prompt-tuning/ticket-clarify/` in parallel with the build; the committed prompt needs a `KEEP` row in that kit's `CHANGELOG.md` before merge (spec conventions R15)
- [ ] Tuning-kit gross-gap review completed (D~4); resolve Q~2, address evaluator parity/scoring gaps, then run matched repeated variants. No live eval yet; `SOCLAAS_API_KEY` is absent from the review shell.
- [x] Gate 2: Approved (Xiao-Congchen, 2026-10-23); operator said "approve" to the plan approval question. Start executing-plans-lite without another implementation approval.
- [ ] Revised-plan gate 2: ask approval of docs/superpowers/plans/2026-10-23-w-ai-agent.md (10 tasks/five waves; E89–E92, UUID builder, tuning parity). On yes write Approved (Xiao-Congchen, current date), run plan lint --approved and start T3 step 7; T8 step 5 can run concurrently. T1 remains Done; continue Wave 2 after reopened Wave 1 verifies.
- [ ] **Handover:** `docs/DeveloperGuide.md` needs the assistant (data, flow, configuration) at its guide checkpoint
- [ ] **Handover:** the live SoCLaaS check of the assistant (a real ticket, a real reply) is a post-merge step

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AI-AGENT | Ticket clarification assistant and draft report for the Support Agent | Building | [spec](docs/superpowers/specs/2026-10-23-w-ai-agent-design.md) | [plan](docs/superpowers/plans/2026-10-23-w-ai-agent.md) | SD22 plan reviewed; gate 2 next, then T3 step 7 | — |

---

## 4. Architecture

### Repository and persistence

**Now:** migration V006 adds `ticket_assistants` keyed by ticket id with bounded state, rounds and draft, and widens ticket-message authorship to SYSTEM. `TicketAssistant` maps the row; `TicketAssistantRepository` supplies a conflict-safe PENDING insert, WAITING_GUEST compare-and-set and pessimistic locked read. Reset helpers delete assistant rows before messages. No runtime assistant orchestration is built yet.

### Guest message presentation

**Now:** Guest ticket messages share an author-role label helper including `Snoozy · Automated assistant`; ticket conversation entries show the stored last-message body as plain text. Authentication roles and booking conversation presentation are unchanged.

### Ticket assistant feature definition

**Now:** `service.assistant` defines validated `ClarifyOutput`, bounded configuration, the conditional Guest-only `ticket.clarify` feature, deterministic R4/R4b screens and fixed plain-text report/message rendering. The provisional prompt follows the committed tuning variant plus approved delimiter/personhood rules; Q~2 holds the concurrent email variant. Runtime orchestration and listeners are not built yet.

---

## 5. Conventions

- `TicketAssistant` is explicitly keyed by `ticket_id` (approved data model), an exception to the ordinary entity `id` column convention; it is a one-to-one extension of the existing ticket.

---

## 6. Known Gaps

No new accepted limitations beyond the provisional prompt and existing screen/prose residuals recorded in the spec; runtime orchestration is not built.

---

## 7. Needs a Human

| ID | What is needed | What it blocks | Raised |
|---|---|---|---|
| Q~2 | SD22 spec approved and revised plan reviewed without blockers; obtain gate 2 approval. On yes mark plan Approved, run approved lint, then execute T3 step 7 and T8 step 5 before Wave 2. Apology variants remain unapproved | Resumed implementation | 2026-10-23 |

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~13 | 2026-10-23 | Approved (Xiao-Congchen, 2026-10-23): SD22 amended High-risk spec, fixed support email via backend expansion | Operator: "approve and continue building" in response to amended-spec approval question. Continue through revised-plan gate; this does not approve an unwritten plan revision or push/PR | Operator conversation |
| C~12 | 2026-10-23 | Operator reverses the blanket contact prohibition only for a fixed support-email instruction. SD22 proposes exact model marker expanded by backend to support@snoozeshare.com with stored ticket UUID subject; amendment review/approval pending | Operator selected revise spec for fixed support email. Rejected retaining the blanket prohibition; arbitrary addresses/subjects remain forbidden. No apology-rule reversal authorized | Operator async answer |
| C~11 | 2026-10-23 | Approved (Xiao-Congchen, 2026-10-23): implementation plan at gate 2; proceed to execution | Operator said "approve" to the explicit plan approval question (10 tasks, five waves). No push, PR, merge or live service action is authorized | Operator conversation |
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

- **D~4 (tuning review, 2026-10-23):** The kit is not ready for reliable prompt ranking. Its validator accepts unknown fields, duplicate keys and escaped URLs that the backend rejects; its injection detection runs before control-character removal and misses a split phrase the backend detects. The judge has no draft factuality/coverage check (a fabricated T03 summary passes), mostly checks first-turn questions, and fixed multi-turn replies reveal facts regardless of what was asked. Summaries omit temperature, maxTokens, round cap and dataset/runner fingerprints, and compare does not require matching case sets/settings. Next: resolve Q~2, correct parity and scoring, then compare identical cases/settings with repeated runs; manually review factuality and question relevance before any KEEP decision. No live call made; review shell has no SOCLAAS_API_KEY.

- **D~1 (planning, 2026-10-23):** Existing notes save/textarea limits are 4,000 characters; approved drafts allow 6,000 and insertion appends existing notes. Plan preserves full local insertion and existing server validation, so an Agent may need to edit down before saving. No silent truncation or limit increase. Carry this UX limitation to review/Handover.
- **D~2 (baseline, 2026-10-23):** Restricted Gradle/Docker access required escalated build commands. All component builds passed; frontend npm ci/lint/build passed. Default frontend suite had six timeout failures, two-worker retry had two different timeout failures (308/310 passed); full single-worker suite passed unchanged (49 files, 310/310 tests), without changing timeouts/assertions. Use --maxWorkers=1. Existing docs/Reflections.md changes are operator work and stay untouched.
- **D~3 (concurrent work, 2026-10-23):** Operator tuning-kit variants changed during T3 reads, including an email instruction contrary to approved P3/R4. Variants stay untouched and excluded from agent commits; Q~2 asks keep/reverse. T3's provisional prompt freezes the committed variant plus already approved P4/P12, pending a scoped decision.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-23 | SD22 revised plan ready: 10 tasks/five waves unchanged, all 89 E rows traced; T3/T8 reopened with historical steps retained. Read-only Implementer review has no blockers/questions/contradictions; plan lint zero errors/warnings. No application code changed this turn. Session/reviewer retired; revised-plan gate 2 next | W-AI-AGENT | This session |
| 2026-10-23 | Revise SD22 plan without changing tasks/dependencies/waves: reopen T3/T8 continuation, trace E89–E92, define UUID builder/caller and replay tests, scope tuning marker parity and safe v4 variant. Plan lint zero errors/warnings; required read-only review in flight | W-AI-AGENT | This session |
| 2026-10-23 | Record SD22 spec approval and begin the revised-plan update; original Wave 1 commits preserved | W-AI-AGENT | This session |
| 2026-10-23 | SD22 amendment reviewed: Implementer/Tester/Maintainer round 1 and Attacker rounds 1–2; forged-subject finding fixed with subject/header/UUID screening and explicit R12 residual, normalized-marker and expanded-replay cases clarified. Spec lint: zero errors, one size warning. Plan intentionally awaits SD22 traceability/revision after spec approval. All implementation/reviewer sessions retired; gate 1 next | W-AI-AGENT | This session |
| 2026-10-23 | T3 original scope done: six production/four test files; focused 22/22, full backend 604/604, Checkstyle and packaging pass; offline kit 35/35. Fix E6 test expectation for shifted newline, not renderer behavior. Provisional prompt excludes unapproved concurrent email/apology changes. SD22 unimplemented; agent retired | W-AI-AGENT | This session |
| 2026-10-23 | T1 done: V006 schema, entity/repository, audit enums and cleanup/migration coverage. Red missing types recorded; focused persistence tests pass; full backend 604 tests/99 classes pass, corrected Checkstyle and packaging pass. Shared full gate also covers provisional T3 | W-AI-AGENT | This session |
| 2026-10-23 | T8 done: seven files, focused 15/15, full frontend 325/325 across 52 files, lint and production build pass. Shared helper ready for T7; auth and booking presentation unchanged; agent retired | W-AI-AGENT | This session |
| 2026-10-23 | Write SD22 support-email amendment per operator choice (C~12), four new edge rows; new behavior held for spec and revised-plan gates. Preserve concurrent tuning-review records D~4 and operator variants | W-AI-AGENT | This session |
| 2026-10-23 | Review tuning workflow for gross gaps: 35/35 offline selftest checks and T01 dry run pass. In-memory probes reproduce acceptance of unknown fields, duplicate JSON keys and escaped links; a zero-width-split injection is missed; a fabricated T03 summary receives no issue. Email instruction is dropped by question screen. No runner/prompt changes or live calls; Q~2 remains pending; D~4 records follow-up. Review session retired; concurrent implementation sessions preserved | W-AI-AGENT | This tuning review |
| 2026-10-23 | Baseline green: all three component builds, frontend npm ci/lint/build and 310/310 tests with one worker. Wave 1 red runs recorded for missing persistence/feature types and missing Guest labels/preview. T1 schema/entity/repository and reset changes implemented; T8 focused tests pass 15/15, full frontend gate in flight | W-AI-AGENT | This session |
| 2026-10-23 | Record plan approval at gate 2 and start baseline verification before implementation. Existing operator changes to docs/Reflections.md are preserved and excluded from all task commits; clean-tree precondition is interpreted as no unrelated agent changes | W-AI-AGENT | This session |
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
