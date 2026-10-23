# W-AI-AGENT Ticket Clarification Assistant Design

- **Status:** Draft
- **Risk tier:** High - guest-written text goes to an LLM, the model's text is shown to a Guest and to the Support Agent who decides money, and the feature stores personal-data-bearing text (C67)
- **Workstream:** W-AI-AGENT | **Plan:** not written yet
- **Constraints consulted:** C80 (AI outputs are suggestions; server-side prompts; every AI interaction audited), C101 and C103 to C107, C109 to C111 (the gateway and its defaults), C69 (single instance), C99 (Spring Data JPA), C73 (DTO layer at the API boundary), C37 (per-ticket threads; C38, the private booking chat, is never touched), C40 (the Host row of § 4.6a: Hosts cannot file tickets; the W14 System-user row is a different C40), C20 and C22 (only an Agent settles money; no Force actions), C28 (one audit row per change, typed columns), C104 (audit holds metadata only); § Known Gaps on AI (process-local limiter state, steered prose passes validation, many accounts can exhaust the cap, reasoning is a global setting, 429 behaviour is unobserved); § Conventions on role AI code (call outside a transaction, server-built trusted context, ASSISTANT turns from stored history only, plain-text rendering); D52 (no bare `Executor` bean); Upcoming rows "Agent-typed text is untrusted LLM input" and "W-AI-AGENT scoping decisions"
- **Reviewed by lenses:** Implementer, Tester, Maintainer, Attacker (Attacker added because Guest- and Host-authored text goes to an LLM and model text is shown to a Guest and an Agent); two rounds run (the cap); the findings of both were fixed in the files, and the round-2 fixes were not re-reviewed

## 1. Problem

**Current behaviour.** A Guest files a ticket with `TicketServiceImpl.fileTicket` from a form: booking, category, title, description, requested remedy and optional supporting text. The ticket is saved `OPEN`, one `TICKET_OPENED` audit row is written and a `TicketOpenedEvent` is published; nothing listens to it. The Support Agent sees whatever the Guest typed. If the Guest was vague (no dates, no description of what was wrong, no evidence), the Agent has to ask in the GUEST thread (`MessageService`), wait, and only then contact the Host. The Agent keeps private findings in `tickets.agent_notes`, saved with `PUT /api/agent/disputes/{ticketId}/notes` while the ticket is `IN_REVIEW` and assigned to them.

**Desired behaviour.** Right after a Guest files a ticket, an automated assistant reads it. If key facts are missing it posts up to three short questions in the ticket's GUEST thread, labelled as an automated assistant. It answers each Guest reply the same way, up to a fixed round cap, and after every turn it refreshes a structured draft report that only the Agent can see. The Agent reads the draft on the dispute page and can insert it into their notes with one click; the system never writes into `agent_notes` itself. The assistant never decides, promises or settles anything, and a ticket works exactly as today when the assistant is off or fails.

## 2. Goals

Eleven functional and six non-functional goals, with acceptance criteria, are in [requirements.md](2026-10-23-w-ai-agent-design/requirements.md). In short:

**Functional**
- F1 to F4: the assistant starts after a Guest files a ticket, asks at most three questions per round for at most the round cap, and answers each Guest reply.
- F5 to F6: it keeps a draft report in its own record; the Agent reads it and may insert it into their notes.
- F7 to F8: the Guest sees labelled assistant messages; it stops when an Agent posts in the thread or the ticket is resolved.
- F9 to F11: it degrades silently to today's flow, stops on a suspicious Guest, and every step is audited.

**Non-functional**
- N1 to N6: filing is never slowed or failed by the assistant; at most four gateway calls per ticket; model text reaches users only through validation and plain-text rendering; no names or contact details in the prompt; restart and outage behaviour is bounded; tests run without the live service.

## 3. Non-goals

- Host-raised tickets and any Host-facing assistant - Hosts cannot file tickets (C40), and the HOST thread is the Agent's channel to the Host; the operator chose Guest tickets only.
- Agent-requested follow-ups, a "re-run the assistant" button or a "stop the assistant" button - the operator chose submission plus Guest replies only.
- The assistant recommending, scoring or predicting a resolution, or contacting the Host - only an Agent settles (C20, C22).
- Writing into `tickets.agent_notes` automatically - the operator chose a separate draft field.
- Agent-typed text in any prompt - the Upcoming row "Agent-typed text is untrusted LLM input" is met by excluding it entirely.
- A queue badge or filter for assistant state, and assistant analytics - not asked for.
- Changing the gateway, its limits or its model settings - the gateway is done (C101); reasoning stays the global `none` (§ Known Gaps).
- Pagination or retention changes for threads and audit rows - unchanged accepted gaps.

## 4. Components

- [Requirements](2026-10-23-w-ai-agent-design/requirements.md) - F1 to F11 and N1 to N6 with acceptance criteria
- [Data model](2026-10-23-w-ai-agent-design/data-model.md) - the `ticket_assistants` table, the message author-role change, the state machine, migration V006
- [API](2026-10-23-w-ai-agent-design/api.md) - the feature registration, the model output contract, the triggers, the job, and the changed Agent read
- [UI](2026-10-23-w-ai-agent-design/ui.md) - the Agent's draft panel, the Guest and Agent thread labels, fixed copy
- [Conventions](2026-10-23-w-ai-agent-design/conventions.md) - package placement, properties, audit rows, rendering and test rules
- [Edge cases](2026-10-23-w-ai-agent-design/edge-cases.md) - input, state, concurrency, failure, permission, volume and migration cases, with the Attacker threat list
- [Implementation order](2026-10-23-w-ai-agent-design/implementation-order.md) - tasks T1 to T10 with dependencies and waves

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | The feature is `ticket.clarify`, registered with `ModelTier.STANDARD`, priority 1, `allowedRoles` = {GUEST}, JSON output, links not allowed | The call is made on behalf of the Guest who triggered it, so the gateway's per-user limits apply to that Guest. Priority 0 is for staff-facing calls and keeps a reserved cap share (C107); this call is Guest-driven. Rejected: FAST tier (the report needs summarising several turns); priority 0 | Spec author |
| SD2 | One gateway call per turn returns both the next questions and the current report snapshot | Costs one request per Guest message and keeps questions and report consistent. Rejected: two calls per turn (doubles the request use against a shared 25 requests-a-minute ceiling, C110) | Spec author |
| SD3 | Assistant state lives in a new table `ticket_assistants`, one row per ticket, not in new columns on `tickets` | `Ticket` has a fifteen-argument constructor used across services and tests; new columns would change every call site and the W-AGENT mapping. A table also keeps AI data removable. Rejected: columns on `tickets` | Spec author |
| SD4 | The assistant runs on a dedicated bounded executor, started by `TicketOpenedEvent` (after commit) and `MessagePostedEvent` | The gateway refuses to run inside a transaction and can block for its whole deadline (§ 4.11), so it must not run on the request thread. Rejected: running it in the request thread; the shared Spring task executor | Spec author |
| SD5 | The round cap is 3 question rounds by default (`snoozeshare.assistant.max-rounds`, 1 to 5); after the Guest answers the last round, one final call refreshes the report and no further question is posted | Bounds cost to at most four calls per ticket (N2) and stops an endless exchange | Operator (cap exists), spec author (value) |
| SD6 | Model-written questions are shown to the Guest only after a deterministic screen drops any question that promises or predicts an outcome (conventions.md rule R4) | A steered or confused model must not promise a refund; prose that matches no leak pattern otherwise passes output validation (§ Known Gaps). Rejected: trusting the prompt alone | Spec author |
| SD7 | The draft report is rendered by the backend from the validated JSON into a fixed plain-text template of at most 6,000 characters, after control and format characters are removed and `summary` and `hostPoints` are screened; the schema caps keep the worst case under the limit, so truncation cannot hide the unknowns | A fixed template with labelled sections keeps Guest claims visibly unverified and removes any formatting channel. Rejected: a 4,000-character limit (the earlier caps could exceed it and cut the trailing "Still unknown" sections) | Spec author |
| SD8 | The assistant stops when an Agent posts in the GUEST thread or the ticket is resolved; assignment alone does not stop it | A human taking over the conversation ends the automation; an Agent who only assigns the ticket still benefits from the gathered facts | Spec author |
| SD9 | If the Guest text matches an injection pattern (`LlmResult.flagged`), the assistant posts no further questions, marks the draft flagged and shows the Agent a warning | A hostile Guest should not get more model turns; the Agent decides what to do | Spec author |
| SD10 | The Agent inserts the draft into notes in the browser (the textarea is filled, the Agent saves with the existing notes endpoint); there is no new write endpoint | The existing save rules (ticket `IN_REVIEW`, assigned to the caller) already protect the notes. Rejected: a server-side "apply draft" endpoint | Spec author |
| SD11 | The assistant is a new package `com.snoozeshare.service.assistant`; it imports only the public types of `com.snoozeshare.ai` | Keeps the rule that nothing outside `ai` imports `ai.config`, `ai.guard`, `ai.limit`, `ai.client` or `ai.audit` meaningful | Spec author |
| SD12 | `snoozeshare.assistant.enabled` defaults to true; the assistant is effectively off whenever the gateway reports `DISABLED` | The gateway's own switch (`snoozeshare.ai.enabled`, off in production until switched on) stays the single production control | Spec author |
| SD13 | The `ticket_assistants` row is inserted in `PENDING` at submission, before the first call, and deleted if the gateway reports `DISABLED` on that call | Every in-flight call then has a row, so restart staleness, an Agent stop and a lost job work the same for the first call as for replies (the Implementer and Tester lenses found the first call uncovered). Rejected: inserting only after the first call | Review round 1 |
| SD14 | Listener-side writes use `REQUIRES_NEW` and never throw; the runner owns a private pool and is not an `Executor` bean | Both events fire in the after-commit phase, where a default transaction is lost (found by three lenses); D52 | Review round 1 |
| SD15 | The ticket category label (Agent-managed) and the listing title (Host-authored) are not sent to the model at all; the trusted context holds enums, numbers and dates only | Round 1 moved them to the untrusted slot; round 2 showed a slot does not stop a Host from steering the model, and the Agent already sees both in the dispute detail. Rejected: the untrusted slot | Review rounds 1 and 2 |
| SD16 | `summary` and `hostPoints` are screened for promises and recommendations, `hostPoints` and questions also for sensitive-data and off-platform requests, and all model strings are NFKC-cleaned first | N3 promised more than the question-only screen delivered, and spacing and apostrophe variants bypassed the first lists | Review rounds 1 and 2 |
| SD17 | The closing message ("Thank you. Your answers have been added to your ticket for the support team.") is also posted when the state becomes `UNAVAILABLE` after at least one round, and for a flagged ticket; it makes no promise about what the team will do | The Guest must not be left with an unanswered question and no sign of what happens next | Review round 1 |
| SD18 | Replayed ASSISTANT turns are the stored question lines only, and the Guest-influenced nature of questions is an accepted gap (R12) | The gateway offers only the ASSISTANT role for them; the bounds are the schema, the screens, the gateway's sanitizing and the round cap | Review round 1 |
| SD19 | A lost or rolled-back job leaves the row `PENDING` and no sweep recovers it | A sweep adds a scheduler and a second writer for a rare, harmless case; the Agent sees "did not finish" and continues manually. Add a sweep only if it proves needed (R12) | Review round 1 |
| SD20 | The assistant posts as the existing `SYSTEM` role and user; no new `Role` value. The frontend labels every `SYSTEM` message in a ticket thread "Automated assistant" through the shared role-label helper, so a future SYSTEM-authored notice would be labelled the same way (recorded as a Convention at close-out) | Reverses the Upcoming row "W-AI-AGENT scoping decisions" (a new `AI` author role), per `AGENTS.md` § 4. Rejected: an `AI` value, which would need a second seeded user, a widened `users.role` CHECK, a copy of the shared `Role` enum into the desktop module and changes where `AccountSummary` and `AccountGovernanceServiceImpl` special-case SYSTEM | Operator |
