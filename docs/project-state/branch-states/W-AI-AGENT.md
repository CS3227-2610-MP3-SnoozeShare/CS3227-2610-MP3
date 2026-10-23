# Branch State — W-AI-AGENT

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `development`;
read it for context and write here instead of editing it. `sync-project-state` folds this file in
after the branch merges.

- **Branch:** W-AI-AGENT (from `development` at `4f87cfa`)
- **Base:** `42128e6` (development tip after the W-AGENT fold and guide update; merged into this branch 2026-10-23, diff of `PROJECT_STATE.md` checked: only the Guide cell and the guide rows changed, nothing this branch edits)
- **Owner:** Xiao-Congchen
- **Status:** In review
- **Approved:** —
- **Last updated:** 2026-10-23 by Claude Sonnet 5.5 — SD23 approved (C~18); plan revised, gate 2 pending

---

**Scope.** The Support Agent's AI feature on the web: after a Guest files a ticket, an assistant asks the Guest clarifying questions in the ticket's GUEST thread and keeps a structured draft report that the Agent reads and can insert into the ticket's notes. Serves the W-AI-AGENT workstream, on top of the W-AI-GATEWAY gateway (§ 4.11).

---

## 1. Orientation

Row added to `PROJECT_STATE.md` § Orientation:

| Path | What lives there |
|---|---|
| `tools/prompt-tuning/ticket-clarify/` | Prompt tuning kit for the ticket clarification assistant (W-AI-AGENT): `run.mjs` (Node 22, offline `--selftest`), 21 synthetic prompt-quality tickets, prompt variants, `CHANGELOG.md` of runs (results are git-ignored). Run by hand with a SoCLaaS key held in the terminal; synthetic data only. It is outside `docs/`, so the Pages site does not publish it |

---

## 2. How to Resume

**Setup** — same as `PROJECT_STATE.md`. This branch is checked out in the main checkout (`S:\CS3227\CS3227-2610-MP3`). It was briefly kept in a sibling git worktree while the W-AGENT fold ran on `development`; the operator ended that on 2026-10-23 (C~9). A leftover empty folder `S:\CS3227\CS3227-2610-MP3-w-ai-agent` may remain on disk until a process releases it; it is not tracked and can be deleted.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|

**Remaining**


- [x] Waves 1–4 complete and committed (T4 finished after SD23, C~18/C~19); T10 steps 1–2 done. Remaining: step 3 operator tuning KEEP (Q~4), code review, gate 3.
- [x] Original gate 1: Approved (Xiao-Congchen, 2026-10-23), before SD22.
- [x] SD22 gate 1: Approved (Xiao-Congchen, 2026-10-23), operator: "approve and continue building". Revised-plan gate 2 remains separate.
- [ ] The operator tunes the prompt with `tools/prompt-tuning/ticket-clarify/` in parallel with the build; the committed prompt needs a `KEEP` row in that kit's `CHANGELOG.md` before merge (spec conventions R15)
- [x] Tuning-kit gross-gap review and offline corrections completed (D~4/C~15); SD22 decision resolved. Live comparison now covers all 21 cases, one run per prompt and case (T01/T10/T14 were run twice); operator transcript vetting and KEEP decision remain.
- [x] Gate 2: Approved (Xiao-Congchen, 2026-10-23); operator said "approve" to the plan approval question. Start executing-plans-lite without another implementation approval.
- [x] Revised-plan gate 2: Approved (Xiao-Congchen, 2026-10-23), operator: "approve and resume building". Includes current T10 tuning-kit adjustment; resume T3 step 7 and T8 step 5, then Wave 2 after verification. No push/PR or live service action authorized.
- [x] Operator pause applied at the current Wave 3 blocker after independent T4 checks and SD23 review; no Wave 4 work started.
- [ ] **Handover:** `docs/DeveloperGuide.md` needs the assistant (data, flow, configuration) at its guide checkpoint
- [ ] **Handover (pre-merge, blocking, plan T10 step 3):** the operator runs `tools/prompt-tuning/ticket-clarify/` on `prompts/v4-snoozy-support-documents.txt` (identical to `ClarifyPrompt`, pinned by `ClarifyFeatureTest`) with a SoCLaaS key held in the terminal, vets the transcripts, and adds a `KEEP` row to its `CHANGELOG.md` (spec R15). Also: the uncommitted tuning-kit edits in `tools/prompt-tuning/ticket-clarify/` (README, run.mjs, tickets.json, v1–v3 prompts, CHANGELOG) predate this session and still need the operator's commit.
- [ ] **Handover:** the live SoCLaaS check of the assistant (a real ticket, a real reply) is a post-merge step

---

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-AI-AGENT | Ticket clarification assistant and draft report for the Support Agent | Building | [spec](docs/superpowers/specs/2026-10-23-w-ai-agent-design.md) | [plan](docs/superpowers/plans/2026-10-23-w-ai-agent.md) | Waves 1–4 done; T10 steps 1–2 done; operator KEEP (Q~4), code review and gate 3 remain | — |

---

## 4. Architecture

### Repository and persistence

**Now:** migration V006 adds `ticket_assistants` keyed by ticket id with bounded state, rounds and draft, and widens ticket-message authorship to SYSTEM. `TicketAssistant` maps the row; `TicketAssistantRepository` supplies a conflict-safe PENDING insert, WAITING_GUEST compare-and-set and pessimistic locked read. Reset helpers delete assistant rows before messages. No runtime assistant orchestration is built yet.

### Guest message presentation

**Now:** Guest ticket messages share an author-role label helper including `Snoozy · Automated assistant`; ticket conversation entries show the stored last-message body as plain text. Authentication roles and booking conversation presentation are unchanged.

### Agent assistant presentation

**Now:** dispute detail shows the nullable assistant panel above internal notes with state, stale and injection warning copy. Insert into notes copies the complete draft into dirty local notes only for the assigned Agent; polling preserves dirty text and explicit Save retains the existing 4,000-character limit. Agent thread messages share role labels and render SYSTEM text left aligned with preserved line breaks.

### Internal assistant message posting

**Now:** `MessageService.postAssistant` trims and bounds the body, requires an open ticket, and writes SYSTEM-authored messages only to the GUEST channel. It joins the caller transaction and publishes only after commit; the applier will own its audit. No HTTP route exposes this method.

### Agent assistant read projection

**Now:** dispute detail reads its assistant row in a read-only transaction and returns only the seven public fields through a nullable service snapshot and response mapper. Defaults/configured round and stale settings are read independently of runtime feature beans; strict stale age applies only to PENDING rows on open tickets. Queue and Guest/Host contracts remain unchanged.

### Ticket assistant feature definition

**Now:** `service.assistant` defines validated `ClarifyOutput`, bounded configuration, the conditional Guest-only `ticket.clarify` feature, deterministic R4/R4b screens and fixed plain-text report/message rendering. The provisional prompt includes approved P13 marker guidance; R4 canonicalizes the document marker and rejects mixed/contact/subject/UUID overrides. R6 requires a server-loaded ticket UUID and expands only the canonical marker to the fixed support instruction. Final live tuning evidence remains T10. The runtime now reads immutable snapshots in a read-only transaction, calls the gateway outside transactions, and applies guarded state/message/audit changes in a REQUIRES_NEW transaction. A private bounded asynchronous runner tracks follow-up handoffs through idle waits. Catch-up calls omit only the newest question when an unseen Guest reply precedes it (SD23). `TicketOpenedListener` (after commit; conflict-safe PENDING insert, then submit) and `MessagePostedListener` (plain listener; a Guest reply claims WAITING_GUEST, an Agent post in the GUEST channel stops the row) start and stop turns, each catching every failure. Settings live under `snoozeshare.assistant` in `application.yml` (`SNOOZESHARE_ASSISTANT_*`); the runbook has the diagnosis and rollback notes.

### 4.17 Ticket assistant (`com.snoozeshare.service.assistant`)

**Now:** after a Guest files a ticket, `TicketOpenedListener` (after commit) inserts a `ticket_assistants` row in `PENDING` and submits an `AssistantJob` to the private bounded `AssistantRunner` (daemon threads, abort-on-full queue, never the request thread). The job loads a read-only snapshot (`AssistantContextBuilder`: eight trusted facts, the ticket text, stored question and Guest turns, no Host, Agent, listing or category text), calls the `ticket.clarify` feature on the shared gateway outside any transaction, then `AssistantApplier` applies the result in one `REQUIRES_NEW` transaction under ticket-then-assistant locks: screened questions are posted as SYSTEM messages in the GUEST thread (the fixed support-email line is expanded server-side from the canonical marker), the draft is replaced, and every state change and message gets an `ASSISTANT_STATE` / `ASSISTANT_MESSAGE` audit row with metadata only. `MessagePostedListener` resumes a `WAITING_GUEST` row on a Guest reply (compare-and-set) and stops it on an Agent post in the GUEST channel. A follow-up call runs when a Guest reply arrived during a call; its replay leaves out only the newest question when an unseen reply precedes it (SD23). Rounds are capped (`max-rounds`, default 3), calls are bounded by `max-rounds + 1`, and every failure ends in `UNAVAILABLE`/`STOPPED` with no retry or sweep. The Agent sees the draft only through dispute detail and inserts it into notes by hand. A SYSTEM-authored ticket-thread message means the automated assistant (C~7).

| ID | Decision |
|---|---|
| C~1 | The draft lives in a separate field and is never written to `tickets.agent_notes` by the system |
| C~7 | The assistant uses the existing SYSTEM role; the frontend labels it "Snoozy · Automated assistant" |
| C~16 / C~18 | SD23 catch-up replay selection (approved) |

---

## 5. Conventions

- A SYSTEM-authored message in a ticket GUEST thread is the automated assistant; do not use the SYSTEM role for anything else there (C~7). Agent text is treated as trusted-for-stop only and never enters the prompt.
- Assistant tests that file tickets must start from `pending()` in `AssistantIntegrationTest` (real listeners run on filing; the helper files with a DISABLED gateway and clears invocations).
- `TicketAssistant` is explicitly keyed by `ticket_id` (approved data model), an exception to the ordinary entity `id` column convention; it is a one-to-one extension of the existing ticket.

---

## 6. Known Gaps

Accepted limitations are the spec's R12 list (conventions.md): a call lost at a restart leaves the row `PENDING` (shown stale) with no sweep; screens are a floor, not a proof, and drop some harmless text; no re-run after `UNAVAILABLE`; model questions are Guest-influenced; steered prose matching no pattern passes; `flagged` is pattern-based; the feature shares the gateway budget with the other role features and many throwaway Guest accounts can fill the two worker threads and fifty-job queue; drafts and assistant messages persist with the ticket (owner W-SECURITY); with the gateway disabled each ticket still costs a row insert, a job and a delete. The ticket-opened listener inserts its row in a REQUIRES_NEW transaction during the after-commit phase, which can need a second pool connection per filing request under heavy load (code review, not load-tested). Also D~1: a 6,000-character draft can exceed the 4,000-character notes limit, so the Agent may need to edit before saving.

---

## 7. Needs a Human

| ID | What is needed | What it blocks | Raised |
|---|---|---|---|
| Q~4 | Operator tuning-kit run and `KEEP` row for the exact committed prompt (plan T10 step 3); also decide who commits the pre-existing tuning-kit edits | Merge (spec R15); gate 3 can still be reviewed | 2026-10-23 |

---

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~19 | 2026-10-23 | Approved (Xiao-Congchen, 2026-10-23): revised plan with T4 step 4a, E93/E94, staged E49; resume T4 and continue Waves 4–5. No push/PR/live-service action authorized | Operator: "Approve plan" at gate 2. Ends the C~17 pause | Operator conversation |
| C~18 | 2026-10-23 | Approved (Xiao-Congchen, 2026-10-23): SD23 catch-up replay selection (C~16 accepted); spec gate 1. Plan gets step 4a and E93/E94/staged E49 traceability; revised-plan gate 2 still pending | Operator: "Approve SD23" in reply to the explicit gate-1 question after asking to continue and finish the feature. Supersedes the pause C~17 for the spec part only | Operator conversation |
| C~17 | 2026-10-23 | Pause before Wave 4; T4 catch-up conflict prevents finishing current Wave 3 without SD23 approval. Finish independent checks and prepare the amendment for review, then leave the resume point | Operator: "pause after the current wave". No SD23 behavior change, further wave, push or PR authorized by this request | Operator conversation |
| C~16 | 2026-10-23 | Proposed, not approved: SD23 omits only the newest question from one catch-up replay when a newer-than-cursor Guest reply precedes it. Preserve stored order/body, round/call caps and E84 otherwise | Codex proposes after E8/E49 failed: chronological replay ends ASSISTANT, preventing required follow-up. Proposal rejects fabricated/reordered Guest turns, uncounted question rounds and silently dropping catch-up. Operator decision pending | T4 concurrency evidence; D~5 |
| C~14 | 2026-10-23 | Approved (Xiao-Congchen, 2026-10-23): revised SD22 plan, including current scoped T10 tuning-kit adjustment; resume execution | Operator: "approve and resume building" to revised-plan gate 2. Reopened Wave 1 precedes Wave 2; no push/PR or live-service authorization | Operator conversation |
| C~15 | 2026-10-23 | The prompt-tuning runner assesses question quality and supports human review of factual summaries; dedicated model-hijack attempts stay in backend security tests. Print each bounded full transcript and treat any score as a human-vetted suggestion, not an automatic KEEP gate | Operator: backend has the more comprehensive hijack evaluation; operator wants to vet the full, short conversation and final summary. Rejected duplicating dedicated hijack cases in prompt ranking or making automated summary scores authoritative | Operator conversation |
| C~13 | 2026-10-23 | Approved (Xiao-Congchen, 2026-10-23): SD22 amended High-risk spec, fixed support email via backend expansion | Operator: "approve and continue building" in response to amended-spec approval question. Continue through revised-plan gate; this does not approve an unwritten plan revision or push/PR | Operator conversation |
| C~12 | 2026-10-23 | Operator reverses the blanket contact prohibition only for a fixed support-email instruction. Approved SD22 uses exact model marker expanded by backend to support@snoozeshare.com with stored ticket UUID subject | Operator selected revise spec for fixed support email. Rejected retaining the blanket prohibition; arbitrary addresses/subjects remain forbidden. No apology-rule reversal authorized | Operator async answer |
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

- **D~5 (T4, 2026-10-23):** E8/E49 fail with 2 calls instead of 3/4. A reply arrives during a call, before its new question in stored order. Catch-up claims PENDING, but exact replay ends ASSISTANT and E84 returns WAITING_GUEST without a call. These approved requirements conflict. SD23 proposes one-question replay selection (C~16/Q~3); spec and plan approval reopened. No behavior workaround or test weakening. Partial T4 source/tests remain uncommitted; no full-backend-green claim. Independent checks are recorded separately.

- **D~4 (tuning review, 2026-10-23):** The operator narrowed this kit to prompt-quality evaluation because backend tests own dedicated hijack attempts (C~15). The runner now rejects unknown fields, duplicate JSON keys and normalized/escaped links; mirrors the approved support-document marker and fixed displayed instruction; prints bounded conversations and per-turn drafts; fingerprints run settings, runner and data; and compares only matching runs. The documented compare CLI was broken by argument parsing and is now fixed with a selftest. It has no automatic summary factuality/completeness judge; the operator reviews transcripts and decides any KEEP. The input-sanitizer/injection-detector mirror remains approximate and is not a security evaluation. Live coverage: all 21 cases, one run per prompt and case, with T01/T10/T14 repeated in an initial sample. On the remaining 18, v2 led schema validity (86.4%) and no-issue rate (77.8%), while v3 led first-call `enough` accuracy (100%) and question-topic coverage (70.0%); no universal winner. Full bounded transcripts are in ignored result files; changelog has the summary. Operator transcript vetting and KEEP decision remain.

- **D~1 (planning, 2026-10-23):** Existing notes save/textarea limits are 4,000 characters; approved drafts allow 6,000 and insertion appends existing notes. Plan preserves full local insertion and existing server validation, so an Agent may need to edit down before saving. No silent truncation or limit increase. Carry this UX limitation to review/Handover.
- **D~2 (baseline, 2026-10-23):** Restricted Gradle/Docker access required escalated build commands. All component builds passed; frontend npm ci/lint/build passed. Default frontend suite had six timeout failures, two-worker retry had two different timeout failures (308/310 passed); full single-worker suite passed unchanged (49 files, 310/310 tests), without changing timeouts/assertions. Use --maxWorkers=1. Existing docs/Reflections.md changes are operator work and stay untouched.
- **D~3 (concurrent work, 2026-10-23):** Earlier operator prompt variants were held pending support-email clarification. C~12/C~13 now approve the exact backend-expanded fixed support instruction; the current variants use `[supporting-documents]` and do not generate the address, ticket id or subject. P10 remains unchanged: apology language is not approved.

---

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-23 | Close-out: architecture 4.17, conventions and Known Gaps written; code review (7 findings): strengthened E44/E45/E8 assertions and imports, connection-pool risk recorded as a gap. Backend build/Checkstyle, frontend 351 tests/lint/build and kit selftest pass. Operator tuning KEEP (Q~4) not run | W-AI-AGENT | This session |
| 2026-10-23 | Pause at Wave 3 blocker as requested: T7 committed/350 frontend tests green; T4 independent 50 tests/6 classes pass with no failures/errors/skips, Checkstyle/package pass, but E8/E49 remain red and no full-current-backend pass is claimed. SD23 reviewed by four lenses, exact burst-count finding fixed (E49/E94), spec lint 0 errors/1 near-cap warning. All sessions/reviewers retired; owner resumption/spec gate 1, revised-plan gate 2, then T4 fix/tests next. No push/PR | W-AI-AGENT | This session |
| 2026-10-23 | T4 partial: scalar read snapshot, guarded transactional applier/job, shared enabled fixture with pre-reset worker drain and legacy opt-out. Missing-type reds recorded; E31 cursor regression red then repaired. Independent focused 50/50 across six classes; Checkstyle and packaging pass with test task excluded. E8/E49 remain red from chronological catch-up/USER-last spec conflict; no full suite or task completion claimed. Steps 1–4 checked, 5–6 held for SD23. Implementer session retired; root owns review/amendment/integration | W-AI-AGENT | This session |
| 2026-10-23 | T4 concurrency found D~5 after initial focused context/flow/runner/boundary checks passed. Draft SD23 replay selection/E93 without changing application behavior; reopen spec/plan approval. T7 committed at 93482d1 (350 frontend tests, lint/build pass). Preserve concurrent tuning/reflection work | W-AI-AGENT | This session |
| 2026-10-23 | T7 done: seven-field nullable frontend projection, every assistant panel state/warning, full manual dirty-note insertion with permission/busy guards, SYSTEM labels and literal pre-wrapped thread text. Focused37/37; full frontend350/350 across53files, eslint and production build pass. Existing4,000notes limit retained/tested with full6,000draft; Agent session retired, root owns commit | W-AI-AGENT | This session |
| 2026-10-23 | T7 red recorded: 21/36 focused tests fail for absent panel/labels/pre-wrap. Build Agent panel, nullable frontend projection, manual insertion and shared thread labels; expand test inventory with ThreadPanel.test.tsx. Initial green attempt exposed test-only timer flush issue; await initial notes and wrap the new polling path in act | W-AI-AGENT | This session |
| 2026-10-23 | Run preliminary prompt comparison on T01/T10/T14 with one run each for v1/v2/v3; v1 omitted `hostPoints` in all outputs, v2 had one `mustNotAsk` hit, v3 had no deterministic issues. Fix `--compare` CLI parser and add selftest. Human transcript scores suggested but not approved; no KEEP decision | W-AI-AGENT | This session |
| 2026-10-23 | Expand live prompt comparison to the remaining 18 cases, one run per prompt; all 21 cases now covered. v2 leads schema validity/no-issue rate, v3 leads `enough` accuracy/topic coverage; single-run results remain directional. Operator transcript vetting/KEEP pending | W-AI-AGENT | This session |
| 2026-10-23 | T2 Done: internal SYSTEM/GUEST posting, bounded input/open-ticket errors, Host isolation, Guest unread increase and commit-only events. Shared Wave 2 full gate 618/618 across 101 classes, style/package pass; root independently confirms actual AssistantPostingTest 4/4 and read API 5/5 XML. Frontend/desktop unchanged reuse Wave 1 gate | W-AI-AGENT | This session |
| 2026-10-23 | T6 done: nullable seven-field Agent-only assistant snapshot, read-only transaction and runtime-independent settings/stale calculation; detail-returning actions preserve projection, Guest/Host recursive privacy coverage. Red missing constructor; focused API35/35; shared full backend618/618 across101classes, Checkstyle and packaging pass (also covers T2). Test-only Host endpoint typo corrected from controller contract. Session retired; root owns commit | W-AI-AGENT | This session |
| 2026-10-23 | T6/API focused checks pass 35/35: private read projection/stale/auth/action responses. Corrected filter accounting: service.assistant.AssistantPostingTest matched no T2 class; unfiltered full suite will verify actual service.AssistantPostingTest, not yet claimed green. Plan Create-path lint waits until task completion as designed | W-AI-AGENT | This session |
| 2026-10-23 | T2 red: missing postAssistant signature. Implement internal bounded SYSTEM/GUEST posting with commit-only event; focused three tests pass, fourth corrected to preserve existing Host conversation with null preview rather than assuming no conversation. T6 compile-red tests now own build slot; rerun combined focused checks when projection compiles | W-AI-AGENT | This session |
| 2026-10-23 | T3 SD22 done: marker normalization/deduplication and mixed/contact/subject/UUID screens; required server UUID expands fixed support line and preserves stored replay; P13 prompt. Red: six missing-signature compilation errors. Focused 17/17; full backend 609/609 across 99 classes, Checkstyle and packaging pass. Current offline kit selftest 40/40 (concurrent kit edits preserved). Agent retired; root owns commit | W-AI-AGENT | This session |
| 2026-10-23 | Close tuning-kit gross-gap review per C~15: validator parity for unknown/duplicate/link outputs, approved fixed-email marker display, 21 prompt-quality cases without dedicated hijack tests, bounded full transcripts with manual review guidance, and run fingerprints/comparability. Node syntax check, all 42 offline selftests and T01 dry run pass. No live model evaluation or KEEP decision | W-AI-AGENT | This session |
| 2026-10-23 | T8 SD22 continuation done: E89 literal stored email/UUID thread and preview regressions; focused 13/13 and full frontend 327/327 across 52 files pass, eslint and production build pass. Two test files only; no UI behavior change. Guest session retired | W-AI-AGENT | This session |
| 2026-10-23 | Record revised-plan approval and resume T3/T8 SD22 continuation. Preserve operator reflection/prompt edits and existing scoped T10 plan adjustment | W-AI-AGENT | This session |
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
