# Implementation Order

The plan will mirror these tasks one for one. Waves group tasks that can run together because they share no files.

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Schema and persistence | Migration `V006__ticket_assistant.sql`; `TicketAssistant` entity, `TicketAssistantRepository` with the compare-and-set update and the locked read; the two new `AuditAction` constants; the Flyway test update | F5, F9, F11, N5 |
| T2 | Assistant posting | `MessageService.postAssistant` and its implementation; the SYSTEM-user constant; thread and unread behaviour for `SYSTEM` messages | F2, F7, F8, N3 |
| T3 | Feature definition | `ClarifyOutput`, `ClarifyPrompt`, the `AiFeature` bean `ticket.clarify`, the question screen (R4), the draft renderer (R5), the message-body builder (R6), `AssistantProperties` (R3) | F2, F4, F5, F10, N3, N4 |
| T4 | Orchestration | The context builder, the job, the applier (guards, lifecycle, audit rows, follow-up check), the `assistantExecutor` bean, and the R2 architecture test | F1 to F5, F8 to F11, N1 to N5 |
| T5 | Triggers | `TicketOpenedListener` and `MessagePostedListener` with the decision table, the stop rule and the rejection paths | F1, F3, F8, F9, N1 |
| T6 | Agent read API | `DisputeDetail` and `DisputeDetailResponse` gain `assistant`; `AssistantResponse`, the mapper and the `jsonPath` contract test | F6, N3 |
| T7 | Agent screens | The "AI assistant" panel and Insert into notes in `DisputeDetail.tsx`; the shared role-label helper and `SYSTEM` in `ThreadPanel.tsx`; frontend types and fixtures | F6, F7, N3 |
| T8 | Guest screen | `SYSTEM` label in `MessageThread.tsx` and the conversation list; the `Role` type | F7 |
| T9 | Configuration and operations | `application.yml` and `application-dev.yml` entries; the runbook section; Agent audit label check (R7) | F11, N5 |
| T10 | Verification and close-out | End-to-end test with the gateway's mock reply; full backend and frontend builds; the live SoCLaaS check marked `Post-merge`; branch state and guide handover | all |

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

Shared-file hazards: T2 and T6 touch different files (`MessageService*` against `DisputeDetail*`); T7 and T8 both touch the frontend `Role` type, but they are in different waves (3 and 1), so T8 lands first and T7 builds on it; T1 and T4 both touch `AuditAction` consumers only through T1's constants; T9 edits `application*.yml` that T3's properties record reads, which is safe because T3 finishes in wave 1.
