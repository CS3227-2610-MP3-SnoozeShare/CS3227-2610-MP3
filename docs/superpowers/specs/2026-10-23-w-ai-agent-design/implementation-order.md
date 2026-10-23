# Implementation Order

The plan will mirror these tasks one for one. Waves group tasks that can run together because they share no files.

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Schema and persistence | Migration `V006__ticket_assistant.sql` (table, widened `messages.author_role` CHECK with `DROP CONSTRAINT IF EXISTS`); `TicketAssistant` entity, `TicketAssistantRepository` with the compare-and-set updates and the locked read; the two new `AuditAction` constants; `TestDataReset` deleting `ticket_assistants` before `messages`; the migration test that reads the constraint name from `pg_constraint` before V006 runs; the `FlywayBaselineTest` update | F5, F9, F11, N5 |
| T2 | Assistant posting | `MessageService.postAssistant` and its implementation; the test that the existing `AuditService.SYSTEM_ACTOR_ID` equals the V004 SYSTEM row (no new constant); thread, unread and conversation-list behaviour for `SYSTEM` messages, including the Host assertions | F2, F7, F8, N3 |
| T3 | Feature definition | `ClarifyOutput`, `ClarifyPrompt`, the `AiFeature` bean `ticket.clarify`, the question and draft screens (R4, R4b, lists A to C), the draft renderer (R5), the message-body builder (R6), `AssistantProperties` (R3); the prompt text chosen with the tuning kit (R15) | F2, F4, F5, F10, N3, N4 |
| T4 | Orchestration | The context builder (repositories only), the job, the applier (guards, lifecycle, audit rows, follow-up check), `AssistantRunner` with its private daemon pool, `awaitIdle` and test constructor (not an `Executor` bean), the `AssistantIntegrationTest` base with the `@MockitoSpyBean LlmGateway` (R10), and the R2 architecture test | F1 to F5, F8 to F11, N1 to N5 |
| T5 | Triggers | `TicketOpenedListener` and `MessagePostedListener` with the decision table, `REQUIRES_NEW` writes, the stop rule, exception containment and the rejection paths; the default `snoozeshare.assistant.enabled=false` in the existing test base so no existing suite starts a job | F1, F3, F8, F9, N1 |
| T6 | Agent read API | `DisputeDetail` and `DisputeDetailResponse` gain `assistant`; `AssistantResponse`, the mapper and the `jsonPath` contract test | F6, N3 |
| T7 | Agent screens | The "AI assistant" panel and Insert into notes in `DisputeDetail.tsx`; the shared role-label helper and `SYSTEM` in `ThreadPanel.tsx`; frontend types, fixtures and tests for every state | F6, F7, N3 |
| T8 | Guest screen | `SYSTEM` label in `MessageThread.tsx` and the conversation list; the Guest-side `Role` type; a check that no exhaustive `switch` or `Record<Role, ...>` in `frontend/` breaks on `SYSTEM` | F7 |
| T9 | Configuration and operations | `application.yml` and `application-dev.yml` entries; the runbook section; the Agent audit label check (R7, E62) | F11, N5 |
| T10 | Verification and close-out | End-to-end test with the gateway's mock reply; full backend and frontend builds; the live SoCLaaS check marked `Post-merge`, with the committed prompt run through the tuning kit and recorded in its changelog; the branch state's § Architecture (new § 4.17 Ticket assistant, linked from § 4.11 and the role-AI Conventions line), § Conventions (including the meaning of a SYSTEM-authored thread message, SD20), § Known Gaps (R12) and Handover items | all |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Schema and persistence | - | T3, T8 | 1 |
| T2 Assistant posting | T1 | T6 | 2 |
| T3 Feature definition | - | T1, T8 | 1 |
| T4 Orchestration | T1, T2, T3 | T7 | 3 |
| T5 Triggers | T4 | T9 | 4 |
| T6 Agent read API | T1 | T2 | 2 |
| T7 Agent screens | T6 | T4 | 3 |
| T8 Guest screen | - | T1, T3 | 1 |
| T9 Configuration and operations | T4 | T5 | 4 |
| T10 Verification and close-out | T5, T7, T8, T9 | - | 5 |

Shared-file hazards: T2 and T6 touch different files (`MessageService*` against `DisputeDetail*`); T7 and T8 both touch frontend role types, but they are in different waves (3 and 1), so T8 lands first and T7 builds on it; T1 adds the `AuditAction` constants that T4 and T9 use; T9 edits `application*.yml` that T3's properties record reads, which is safe because T3 finishes in wave 1.

SD22 amendment keeps every task and dependency unchanged: T3 adds the exact marker to P13, R4 screening and the ticket-id-taking R6 builder plus E89/E90/E92 unit tests; T4 supplies the locked stored ticket id and tests E89/E91 lifecycle behavior; T7/T8 test literal fixed-line rendering; T10 includes these rows in final traceability and reconciles the tuning-kit contract before its KEEP gate. No new table, endpoint, gateway change or email service is added.
