# API

All operations live under `/api/agent`, in classes in `com.snoozeshare.api.controller.agent`, one controller per area (`AgentDisputeController`, `AgentMessageController`, `AgentAccountController`, `AgentAuditController`, `AgentCategoryController`). Entity-to-DTO conversion happens in `api.mapper` classes and the `from(...)` factories of the response records, never in a controller.

## Common rules (apply to every operation)

| Field | Definition |
|---|---|
| Caller and principal | A Firebase-authenticated user with database role AGENT and status ACTIVE. The chain is `FirebaseAuthFilter`, then `RoleAuthorizationFilter`, then a `SecurityConfig` rule `requestMatchers("/api/agent/**").hasRole("AGENT")` (SD11) |
| Authorization rule | The filter checks ACTIVE and the role by URL namespace, now on the path from `UrlPathHelper.getPathWithinApplication`, which is decoded, drops `;` parameters and collapses `//` (SD11); the `hasRole` rule repeats it on Spring Security's own path matching. `AccountGovernanceService` re-asserts an ACTIVE agent (a race can make it return 409 `Account is not active`); `TicketService`, `DisputeSettlementService` and `MessageService` re-assert the AGENT role only, because ACTIVE is the filter's job. A controller never decides authorization |
| Actor | `@CurrentUserId UUID agentId`, resolved from a request attribute the filter sets from `AuthUser.id()` after the role check (SD8). When the attribute is absent the resolver throws `MissingActorException`, which the handler maps to 401 `INVALID_TOKEN`; a null actor never reaches a service. Bodies are records with `@JsonIgnoreProperties(ignoreUnknown = false)`; the rejection itself comes from `spring.jackson.deserialization.fail-on-unknown-properties: true` in `application.yml`, which a test pins |
| Tenancy scope | The agent role is platform-wide by design: any agent may read any dispute, account and audit row, including other agents' reasons. What is scoped is the actor: notes and resolve apply only to a ticket assigned to the caller (the service refuses otherwise, 409) |
| Registration | Controllers carry `@ConditionalOnProperty(snoozeshare.security.enabled=true)` and `@ConditionalOnDatabase` (SD9). `ProdEnvironmentGuard` already requires security to be enabled in production (C95) |
| Responses | JSON; `Cache-Control: no-store` on success and error; ids are UUID strings; instants are ISO-8601 UTC; dates are `yyyy-MM-dd`; money is a JSON number with at most 2 decimals (request amounts may be numbers or strings); list endpoints other than A11 return everything (accepted, Known Gap) |
| Request size | A filter on `/api/agent/**`, placed after `FirebaseAuthFilter` and `RoleAuthorizationFilter` so an anonymous caller gets 401 first, rejects a request with `Content-Length` above 65,536 (413 `PAYLOAD_TOO_LARGE`) and a request that declares `Transfer-Encoding` instead of `Content-Length` (411 `LENGTH_REQUIRED`) before the body is read (SD13). A bodyless PUT or DELETE has neither header and passes |
| Cache headers | A `NoStoreFilter` first in the chain for `/api/agent/**` sets `Cache-Control: no-store` on every response, including filter-written errors and 404s |
| Never returned or logged | Firebase UID, ID token, registration codes, request bodies, reasons, notes, message text, SQL parameter values. `agentNotes` appears only in `DisputeDetailResponse` (SD14) |
| Idempotency | Reads are safe. Mutations are not idempotent: a repeat hits the service's state check and returns 409 (E14, E21, E25) |
| Transaction boundary | Each mutating endpoint makes one mutating service call, which owns the transaction. A3-A5 then read the result with `DisputeQueryService.detail` outside that transaction; if the read fails after the mutation committed, the endpoint returns the error and the client treats a failed mutation response as unknown and refetches (E64). `MessageService.post` has no `@Transactional`: it is a single save and needs none |
| Existing files touched | `RoleAuthorizationFilter`, `FirebaseAuthFilter` (path matching), `AuthErrorWriter` (made public, takes the message, sets `no-store`), `SecurityConfig`, `TicketRepository` (row lock, unassigned count), `TicketQueueRepositoryImpl` (order by `createdAt`, then `id`), `UserRepository` (row lock), `TicketServiceImpl`, `DisputeSettlementServiceImpl`, `DisputeQueryService` and its implementation, `AccountGovernanceService` and its implementation, `AuditServiceImpl`, `AuditLogSearchRepositoryImpl` (escape `%`, `_` and `!` with `!` in text patterns and add `ESCAPE '!'`, E82), the services' not-found throws, `TicketResponse`, `DisputeDetailResponse`, `SettleDisputeRequest`, `SaveNotesRequest`, `PostMessageRequest` (its cap also applies to booking messages), `CreateCategoryRequest`, `AccountActionRequest`, `application.yml`, `backend/build.gradle` (ArchUnit), frontend `AuthProfile` and `AuthProvider` (add `id`) |

**Error contract (F2).** Body `{ "code": string, "message": string, "requestId": string }`, produced by `ApiExceptionHandler`, a `@RestControllerAdvice` extending `ResponseEntityExceptionHandler` and not restricted to a package, so that errors raised before a handler is chosen (unknown route, 405, 415, unreadable body) use the same body (SD15). `AuthController` keeps its own local handlers, which take precedence for its routes; an integration test pins that `/actuator/health` still returns exactly `{"status":"UP"}` and every auth response is unchanged. The filters write the same shape through `AuthErrorWriter`.

| Condition | Status | `code` | `message` |
|---|---|---|---|
| Missing or bad token, or a missing actor | 401 | `INVALID_TOKEN` | existing |
| Profile row missing | 404 | `PROFILE_NOT_REGISTERED` | existing |
| User not ACTIVE | 403 | `PROFILE_INACTIVE` | existing |
| Role is not AGENT | 403 | `ROLE_MISMATCH` | existing |
| Bean validation, unreadable JSON, unknown property, bad enum, date or UUID, `limit` or `offset` out of range | 400 | `VALIDATION_ERROR` | `Request is not valid` (never echoes input) |
| `NotFoundException`, or no route | 404 | `NOT_FOUND` | the service message, or `Not found` for a route |
| Other `IllegalArgumentException` thrown from `com.snoozeshare.service` or `com.snoozeshare.domain` (decided from the first stack frame) | 400 | `INVALID_REQUEST` | the service message |
| `IllegalStateException` thrown from `com.snoozeshare.service` or `com.snoozeshare.domain` | 409 | `CONFLICT` | the service message |
| Lock wait timed out (`PessimisticLockingFailureException`) | 409 | `CONFLICT` | `Another change is in progress. Try again.` |
| `IllegalArgumentException` or `IllegalStateException` thrown from anywhere else (framework, JDK, Hibernate) | 400 / 500 | `VALIDATION_ERROR` / `INTERNAL_ERROR` | the fixed messages of their rows, never the exception text |
| Wrong HTTP method | 405 | `METHOD_NOT_ALLOWED` | `Method not allowed` |
| Body without `Content-Length` | 411 | `LENGTH_REQUIRED` | `Content-Length is required` |
| Body over 65,536 bytes | 413 | `PAYLOAD_TOO_LARGE` | `Request is too large` |
| Unsupported content type | 415 | `UNSUPPORTED_MEDIA_TYPE` | `Unsupported media type` |
| Anything else (including database errors) | 500 | `INTERNAL_ERROR` | `Something went wrong` |

Service messages are written for end users and contain no personal data, ids or SQL; a test calls every agent-reachable service path that throws and asserts that no message contains a UUID, an email address, `SQL` or `Exception` (E40). The accepted exceptions: `Booking does not exist`, `Property does not exist` and `Guest wallet does not exist` stay `IllegalArgumentException` and map to 400 `INVALID_REQUEST`, and `AuthorizationService.requireRole` stays an `IllegalStateException` (409), unreachable behind the filter. The handler logs `requestId`, status, `code`, path and agent id at WARN (4xx) or ERROR (500); the 500 log line carries the exception class and a stack trace whose message text is removed (N4).

## Operations

| # | Method and path | Input | Service call | Success | Operation-specific errors |
|---|---|---|---|---|---|
| A1 | `GET /disputes` | query `assignee` = ALL, UNASSIGNED or MINE (default ALL) | `DisputeQueryService.queue(null, assignee, agentId)` and the new `DisputeQueryService.unassignedCount()` (count of OPEN tickets with no assignee, one count query) | 200 `DisputeQueueResponse { items: DisputeSummaryResponse[], unassignedCount: int }`, oldest `createdAt` first (the repository order) | 400 bad enum |
| A2 | `GET /disputes/{ticketId}` | path UUID | `DisputeQueryService.detail` | 200 `DisputeDetailResponse` | 404 |
| A3 | `PUT /disputes/{ticketId}/assign` | none | `TicketService.assignToMe` (ticket row lock is its first read, SD10) then `detail` | 200 `DisputeDetailResponse` | 404; 409 `Ticket is not open` |
| A4 | `PUT /disputes/{ticketId}/notes` | `{ notes: string \| null }`, max 4,000 characters; null or empty stores the empty string | `TicketService.saveNotes` (row lock first) then `detail` | 200 `DisputeDetailResponse` | 400 over length; 404; 409 `Notes can only be saved while the ticket is in review`, `Ticket is not assigned to this agent` |
| A5 | `PUT /disputes/{ticketId}/resolve` | `{ mode: ACCEPT\|REJECT\|MANUAL, guestRefund: decimal \| null, reason: string }`; reason `@NotBlank` max 1,000; `guestRefund` `@DecimalMin("0")` `@Digits(integer=9, fraction=2)`; the existing `SettleDisputeRequest`, edited to add the constraints | `TicketService.resolve(ticketId, new ResolutionRequest(mode, guestRefund, reason), agentId)` then `detail`. `resolve` takes the ticket row lock (`findByIdForUpdate`) as its first ticket read, before `loadTicket` and the booking read, and `settle` takes the same lock again (a no-op inside the same transaction) before it reads status or escrow; the loser of a race therefore sees the committed state and gets `Ticket is not in review` (SD10) | 200 `DisputeDetailResponse` of the resolved ticket | 400 refund missing for MANUAL or for ACCEPT on PARTIAL_REFUND or OTHER; 400 `Accept needs a refund above zero; use Reject or Manual adjustment for a zero refund`; 400 `Guest refund cannot exceed the escrow held`; 404; 409 `Ticket is not in review`, `Ticket is not assigned to this agent`, `Booking is not confirmed`, `Escrow is not held for this booking` |
| A6 | `GET /disputes/{ticketId}/messages/{channel}` | `channel` = GUEST or HOST | `MessageService.thread(ticketId, channel, agentId, AGENT)` | 200 `MessageResponse[]` oldest first | 400 bad channel; 404 |
| A7 | `POST /disputes/{ticketId}/messages/{channel}` | `{ body: string }`, `@NotBlank`, max 2,000 | `MessageService.post(ticketId, channel, agentId, AGENT, body)` | 201 `MessageResponse` | 400; 404; 409 `Ticket is resolved` |
| A8 | `GET /accounts` | none | new `AccountGovernanceService.listGovernableAccounts()` (Guests and Hosts, oldest first) | 200 `AccountSummaryResponse[]` | none |
| A9 | `PUT /accounts/{userId}/suspend` | `{ reason }`, `@NotBlank` max 1,000 (`AccountActionRequest`, edited to add `@Size`) | `AccountGovernanceService.suspend(userId, agentId, reason)`; the target user row is locked first (SD19) | 200 `AccountSummaryResponse` of the updated user | 404; 400 `Support agent accounts cannot be suspended or reactivated` (Agent or System target); 409 `Account is already suspended` |
| A10 | `PUT /accounts/{userId}/reactivate` | `{ reason }` as A9 | `AccountGovernanceService.reactivate` (target row locked first) | 200 `AccountSummaryResponse` | 404; 400 as A9; 409 `Account is not suspended` |
| A11 | `GET /audit` | query `text` (trimmed, max 100 characters), `actions` (comma-separated `AuditAction` names, at most 40 items; whitespace trimmed, duplicates and empty items ignored), `from`, `to` (`yyyy-MM-dd`; either may be given alone; `from` not after `to`), `limit` 1-200 (default 200), `offset` 0 or more (default 0) | new `AuditQueryService.search(new AuditFilter(text, actions, from, to), limit, offset)` returning `AuditEntry` service records; it delegates to `AuditService.search`, so controllers never touch the write-side `AuditService` or the entity (SD20). The day boundaries use the audit zone (SD16). `actions` is bound as `Set<AuditAction>` by Spring's converter, so a bad name is a type mismatch (400 `VALIDATION_ERROR`) and never reaches `valueOf` | 200 `AuditEntryResponse[]` newest first (timestamp, then id, descending); fewer rows than `limit` means no more | 400 any bad parameter |
| A17 | `GET /audit/actions` | none | `AuditAction.values()` | 200 `string[]` in enum order | none |
| A12 | `GET /categories` | none | `TicketService.listAllCategories()` | 200 `TicketCategoryResponse[]` | none |
| A13 | `POST /categories` | `{ label }`, `@NotBlank` max 60 (`CreateCategoryRequest`, edited to add `@Size`) | `TicketService.createCategory(label, agentId)` | 201 `TicketCategoryResponse` | 400 `Category label already exists` (the match ignores case and surrounding spaces) |
| A14 | `PUT /categories/{categoryId}` | `{ label }` as A13 | `renameCategory` | 200 `TicketCategoryResponse` | 400 duplicate; 404 |
| A15 | `PUT /categories/{categoryId}/active` | `{ active: boolean }`, `@NotNull` (new `SetCategoryActiveRequest`) | `setCategoryActive` | 200 `TicketCategoryResponse` | 404 |
| A16 | `DELETE /categories/{categoryId}` | none | `deleteCategory` | 204, no body | 404; 409 `Category is in use by tickets; deactivate it instead` |

`AuditEntryResponse` (new, `api.response`, built from the `AuditEntry` record): `id`, `timestamp`, `actorName`, `actionType`, `entityType`, `entityId`, `beforeState`, `afterState`, `walletAdjustment`, `balanceAfter` (both rounded half-up to 2 decimals), `reason`, `subjectName`, `bookingId`, `ticketId`. It omits `actorUserId` and `subjectUserId`. `DisputeDetailResponse` gains `agentNotes` and `TicketResponse` loses it, so the shared ticket DTO the Guest and Host controllers will reuse cannot leak notes. A11 never returns more than 200 rows; paging by offset can repeat or skip a row when new entries arrive between two pages (accepted).

The `GUEST` thread is the guest-visible channel; text typed in the internal notes box is never posted to either thread (E17).

### Threat list (Attacker lens, High tier)

| Actor | Abuse | Mitigation | Test |
|---|---|---|---|
| Guest or Host token | Calls `/api/agent/**`, also through an encoded, double-slash, trailing-slash or `;` path variant | Filters match on the decoded servlet path and `SecurityConfig` repeats the rule with `hasRole("AGENT")` | E1, E62 |
| Unauthenticated caller | Calls any agent route | 401 `INVALID_TOKEN` | E2 |
| Suspended former agent | Reuses a valid Firebase token | `requireActiveUser` returns 403 `PROFILE_INACTIVE` on every request; a request already past the filter completes (accepted) | E3 |
| Any agent | Acts as another agent by sending an `agentId` | The field is rejected as an unknown property; the actor is the token's user | E4 |
| Code path that skips the filter | Reaches a controller without an actor | The resolver throws, 401; the controller is not registered when security is off | E5, E69 |
| Framework or driver exception | Its text (class names, enum values, bound input) reaches the browser | Only exceptions thrown from `service` or `domain` pass their message; the rest get fixed text | E75 |
| Agent A | Resolves or saves notes on a ticket assigned to agent B | Service rule: 409 `Ticket is not assigned to this agent` | E12, E15 |
| Two agents or two clicks | Race to assign or resolve one ticket, double-paying the escrow | Pessimistic row lock on the ticket as the first read of every ticket mutation (SD10); lock timeout 5 s, then 409; deterministic two-thread tests | E10, E20, E76 |
| Two agents | Suspend the same guest or host at once, double-refunding the cascade | Pessimistic row lock on the target user first (SD19); the loser sees SUSPENDED and gets 409 | E74 |
| Agent | Suspends or reactivates an Agent or the System user | `requireGovernable` rejects with 400 | E24 |
| Agent | Uses suspend then reactivate repeatedly to cancel bookings | Each suspension audits the account row and every cancellation and refund; reactivation restores nothing | E26 |
| Agent | Injects HTML or script through a reason, note, message or label | Stored and returned as plain text; the frontend renders text nodes only. The audit page shows labels and reasons that were concatenated into audit reasons (`Renamed "..."`) as text too | E36 |
| Agent | Floods mutations or fills the database with messages or categories | No per-agent limit is added (agents are trusted staff; Known Gap: process-local limiting); every mutation is audited; request size is capped | accepted |
| Anyone | Sends an oversized body, signed in or not | 413/411 before parsing but after authentication (anonymous gets 401), `@Size` caps after | E7, E81 |
| Compromised agent account (reusable code leaked, C88) | Settles disputes or suspends accounts | Not preventable here (Known Gap); every action is audited with actor and reason | accepted |
| Operator | Deploys with security disabled | `ProdEnvironmentGuard` requires `snoozeshare.security.enabled=true`; with it false the routes do not exist | E5 |
| Future AI feature | Audit reasons, notes and messages written by agents become LLM input | Out of scope here; recorded as a Handover for W-AI-AGENT (prompt injection) | none |
