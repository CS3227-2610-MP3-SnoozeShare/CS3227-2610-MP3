# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Backend REST foundation | `ApiErrorResponse`, `ApiExceptionHandler` (global), `NotFoundException` thrown by the agent-reachable services, `@CurrentUserId` with resolver, `ApiWebConfig`, `MissingActorException`, the filter attribute, decoded-path matching in both filters plus the `hasRole` rules in `SecurityConfig`, `NoStoreFilter`, the request-size filter (after the auth filters), `AuthErrorWriter` made public with a message and `no-store`, the origin-based exception mapping, ArchUnit dependency and `ApiLayerTest`, the `application.yml` unknown-property test | F1, F2, N1, N4, N6 |
| T2 | Dispute endpoints A1-A5 | `AgentDisputeController`, `DisputeQueueResponse`, `DisputeQueryService.unassignedCount`, `TicketRepository.findByIdForUpdate` taken first in `assignToMe`, `saveNotes`, `unassign`, `resolve` and `settle`, queue order `createdAt` then `id`, `agentNotes` moved from `TicketResponse` to `DisputeDetailResponse`, request constraints, the two-thread tests | F3, F4, F5, F6, F7, N2, N3, N5 |
| T3 | Thread endpoints A6-A7 | `AgentMessageController`, the `@Size` cap on `PostMessageRequest` (also applies to booking messages) | F8 |
| T4 | Account endpoints A8-A10 | `AgentAccountController`, `AccountGovernanceService.listGovernableAccounts`, `UserRepository.findByIdForUpdate` taken first in `suspend` and `reactivate`, `AccountMapper`, the `@Size` cap on `AccountActionRequest` | F9, F10 |
| T5 | Audit endpoints A11, A17 | `AgentAuditController`, `AuditQueryService` with `AuditEntry`, `AuditEntryResponse`, the `snoozeshare.audit.zone` property (default `Asia/Singapore`) applied in `AuditServiceImpl`, wildcard escaping in `AuditLogSearchRepositoryImpl` | F11, N5 |
| T6 | Category endpoints A12-A16 | `AgentCategoryController`, `SetCategoryActiveRequest`, request constraints | F12 |
| T7 | Frontend foundation | `lib/api.ts` JSON helper (204-safe) and session-failure mapping, `lib/agent-api/types.ts`, `usePolling`, `SessionNotice`, `AgentDialog`, `RoleNavbar` AGENT links without the Wallet pill, `id` added to `AuthProfile` and `AuthProvider` so a page knows if a ticket is its own, `/agent` redirect | F13, F16, N7, N8 |
| T8 | Disputes pages | `lib/agent-api/disputes.ts`, queue page, detail page, three dialogs | F14 |
| T9 | Accounts page | `lib/agent-api/accounts.ts`, accounts page and dialog | F15 |
| T10 | Audit Log page | `lib/agent-api/audit.ts`, audit page | F15 |
| T11 | Categories page | `lib/agent-api/categories.ts`, categories page and dialogs | F15 |
| T12 | Verification and close-out | Backend and frontend builds, a browser walk-through of each flow against local Postgres, branch state close-out | N1-N8 |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Backend REST foundation | - | T7 | 1 |
| T2 Dispute endpoints A1-A5 | T1 | T3, T4, T5, T6, T8, T9, T10, T11 | 2 |
| T3 Thread endpoints A6-A7 | T1 | T2, T4, T5, T6, T8, T9, T10, T11 | 2 |
| T4 Account endpoints A8-A10 | T1 | T2, T3, T5, T6, T8, T9, T10, T11 | 2 |
| T5 Audit endpoints A11, A17 | T1 | T2, T3, T4, T6, T8, T9, T10, T11 | 2 |
| T6 Category endpoints A12-A16 | T1 | T2, T3, T4, T5, T8, T9, T10, T11 | 2 |
| T7 Frontend foundation | - | T1 | 1 |
| T8 Disputes pages | T7 | T2, T3, T4, T5, T6, T9, T10, T11 | 2 |
| T9 Accounts page | T7 | T2, T3, T4, T5, T6, T8, T10, T11 | 2 |
| T10 Audit Log page | T7 | T2, T3, T4, T5, T6, T8, T9, T11 | 2 |
| T11 Categories page | T7 | T2, T3, T4, T5, T6, T8, T9, T10 | 2 |
| T12 Verification and close-out | T2, T3, T4, T5, T6, T8, T9, T10, T11 | - | 3 |

Shared-file hazards: T1 alone edits `RoleAuthorizationFilter`, `FirebaseAuthFilter`, `SecurityConfig`, `backend/build.gradle`, and all not-found throws in `TicketServiceImpl`, `MessageServiceImpl`, `DisputeQueryServiceImpl` and `AccountGovernanceServiceImpl`; those files are edited again only by later waves. Within wave 2: T2 alone edits `TicketServiceImpl`, `DisputeSettlementServiceImpl`, `DisputeQueryService` and its implementation, `TicketRepository`, `TicketQueueRepositoryImpl`, `TicketResponse`, `DisputeDetailResponse`, `SettleDisputeRequest` and `SaveNotesRequest`; T3 alone edits `PostMessageRequest`; T4 alone edits `AccountGovernanceService` and its implementation, `UserRepository` and `AccountActionRequest`; T5 alone edits `AuditServiceImpl`, `AuditLogSearchRepositoryImpl` and `application.yml`; T6 alone edits `CreateCategoryRequest`. T1 also edits `AuthErrorWriter`. T7 alone edits `RoleNavbar`, `lib/api.ts` and `app/agent/page.tsx`; the `origin/W-HOST` branch also edits `RoleNavbar` and `RoleShell`, so whichever merges second resolves that conflict (re-check at plan time). T8-T11 each write their own `lib/agent-api/<area>.ts`, `app/agent/<area>/` and `components/agent/<Area>*` files.
