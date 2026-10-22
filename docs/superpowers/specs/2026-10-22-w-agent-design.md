# W-AGENT Design

- **Status:** Approved (Xiao-Congchen, 2026-10-22)
- **Amended 2026-10-22:** see branch state C~8 (T5 also edits `AuditLogSearchRepositoryImpl`) and C~9 (path helper, lock timeout mechanism, escape character)
- **Risk tier:** High - agent-only endpoints settle disputes (moves escrow money), suspend and reactivate accounts (cascades cancellations and refunds) and expose the audit log; they sit behind role authorization.
- **Workstream:** W-AGENT | **Plan:** not yet written
- **Constraints consulted:** C20 (full escrow settled two-sided), C22 (no Force Cancel/Complete), C23, C70, C73 (DTO layer), C78 (polling, no WebSocket), C79, C86, C92 (UID from the verified token, client-side guards), C95, C96, C99 and C100 (JPA, NUMERIC scales); § Known Gaps (client-side route guards, process-local rate limiting, Firebase revocation check per request, single instance, SERIALIZABLE only for bookings); Explicitly out of scope: agent Force Cancel/Complete.
- **Reviewed by lenses:** Implementer, Tester, Maintainer, Attacker (reason: roles, money, untrusted text) - rounds 1 and 2 done (cap reached), findings applied below

## 1. Problem

**Current behaviour (verified in code).** The Support Agent role has a landing shell only: `frontend/src/app/agent/page.tsx` is an empty `<main>`, and every tab in `RoleNavbar` links to `/agent`. The backend has no REST controller for the agent: the only controller is `AuthController` under `/api/auth`. The services an agent needs exist and are tested (W-DOMAIN): `DisputeQueryService`, `TicketService` (assign, notes, resolve, categories), `MessageService`, `AccountGovernanceService`, `AuditService.search`. `RoleAuthorizationFilter` restricts `/api/agent/**` to an ACTIVE user whose database role is AGENT but does not hand the acting user's id to a controller, and it matches the path on the raw request URI. The services report every failure as `IllegalArgumentException` or `IllegalStateException` with a message and have no HTTP mapping. Ticket state changes are plain `@Transactional` with no row lock, so two agents can both settle one escrow (only bookings use SERIALIZABLE). The frontend `authenticatedFetch` has no typed callers.

**Desired behaviour.** A signed-in agent works the four desktop tabs on the web: Disputes (queue, detail, two message threads, internal notes, Accept / Reject / Manual adjustment), Accounts (suspend and reactivate Guests and Hosts), Audit Log (search, filters, load more) and Categories (add, rename, activate, delete). Behaviour follows the desktop screens; visuals follow the design artifact (`https://claude.ai/artifact/PWBCxbfv9e9FGVvY6RKUwd`, boards `AgentDisputeQueue`, `AgentDisputeDetail`, `AgentAccounts`, `AgentAuditLog`, `AgentTicketCategories` and the `Confirm*` dialogs). Where the two differ the operator chose the desktop behaviour (SD2 to SD5). Settling and assigning a ticket become safe under concurrency (SD10).

## 2. Goals

Sixteen functional and eight non-functional goals; each has an acceptance criterion in [requirements.md](2026-10-22-w-agent-design/requirements.md).

**Functional** - F1 token-derived agent identity; F2 stable error contract; F3 dispute queue; F4 dispute detail; F5 assign; F6 notes; F7 resolve; F8 ticket threads; F9 accounts list; F10 suspend and reactivate; F11 audit search; F12 categories; F13 routes and navigation; F14 dispute pages and dialogs; F15 accounts, audit and category pages; F16 API client auth-failure handling.

**Non-functional** - N1 authorization; N2 money; N3 audit; N4 logging; N5 performance; N6 layering test; N7 accessibility; N8 polling.

## 3. Non-goals

- Force complete or force cancel booking - C22 and the out-of-scope list; the artifact's two buttons are dropped (SD2). The Audit Log filter still lists every audit action, because a suspension cascade writes `BOOKING_FORCE_CANCELLED` rows.
- Agent AI features, the SoCLaaS assistant and any use of `LlmGateway` - W-AI-AGENT, Q6.
- Guest and Host endpoints and pages - W-GUEST, W-HOST. W-AGENT builds only the shared REST foundation they will reuse (SD1).
- Unassign a ticket from the UI - neither the desktop nor the artifact has it; `TicketService.unassign` stays unexposed (it gets the row lock for consistency).
- Agent unread counts and read receipts - the artifact has none.
- Suspending, reactivating or listing Support Agent accounts - the service refuses it and the desktop lists Guests and Hosts only.
- Disabling a Firebase account on suspension - suspension is a database status checked on every request by `RoleAuthorizationService.requireActiveUser`; Firebase is not touched.
- Pagination of the queue and accounts lists - a data set of a few hundred rows is the design size; accepted limit.
- Seed or demo data for the agent screens - tests build their own data.
- Real-time push (WebSocket) - C78, polling only.
- Per-agent rate limiting - agents are trusted staff; process-local limiting is a Known Gap.

## 4. Components

- [Requirements](2026-10-22-w-agent-design/requirements.md) - F1-F16 and N1-N8 with acceptance criteria.
- [API](2026-10-22-w-agent-design/api.md) - the 17 `/api/agent/**` operations, common rules, error table, touched files, threat list.
- [UI](2026-10-22-w-agent-design/ui.md) - routes, the five pages, dialogs, states, copy.
- [Conventions](2026-10-22-w-agent-design/conventions.md) - package layout, ArchUnit rules, error mapping, money and time formats, test rules.
- [Edge cases](2026-10-22-w-agent-design/edge-cases.md) - E1-E82 and the data-corruption guards.
- [Implementation order](2026-10-22-w-agent-design/implementation-order.md) - T1-T12 with the wave table.
- No data model changes: every table the agent touches (`users`, `tickets`, `ticket_categories`, `messages`, `audit_log`, `bookings`, wallets) exists from V002-V005; no migration, no seed change. The row lock is a query, not a column.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | W-AGENT builds the shared REST foundation (global `ApiExceptionHandler`, `@CurrentUserId`, `ApiErrorResponse`, ArchUnit layering test, typed frontend helpers) in packages the Guest and Host controllers reuse | `origin/W-HOST` is a fixture-only frontend branch with no backend and `gh` shows no open pull request for it, and no Guest controller exists, so nothing to adopt. Rejected: a separate foundation workstream (delays the agent) and agent-only copies (diverging conventions) | Operator, 2026-10-22 ("see what [W-HOST] has done") and finding |
| SD2 | Keep C22: no force complete or force cancel; drop the artifact buttons | Operator chose to keep the recorded decision | Operator, 2026-10-22 |
| SD3 | Manual adjustment follows the desktop and C20: override type and one Guest refund amount; the host side is derived. Rejected: the artifact's per-wallet selector | Operator | Operator, 2026-10-22 |
| SD4 | Audit filters follow the desktop and `AuditFilter`: Search, Action type, From, To, Apply, Clear, Load more. Rejected: the artifact's User ID and Booking ID inputs | Operator | Operator, 2026-10-22 |
| SD5 | Accounts list Guests and Hosts only with no Suspend for agents; categories keep Delete; amounts display in SGD. Rejected: the artifact's agent row, missing Delete and `$` | Operator | Operator, 2026-10-22 |
| SD6 | Resolve calls `TicketService.resolve` (which derives the refund for Accept) and returns the refreshed `DisputeDetailResponse`. Rejected: calling `DisputeSettlementService.settle` from the controller | One rule set for money | Maintainer reading of `TicketServiceImpl` |
| SD7 | Add `NotFoundException extends IllegalArgumentException` for a missing ticket, user, category or thread. `Booking`, `Property` and `Guest wallet does not exist` stay 400 (data-corruption cases, accepted). Rejected: matching the text "does not exist" | Typed, backward compatible | Implementer |
| SD8 | `RoleAuthorizationFilter` stores the resolved `AuthUser` id in a request attribute; `CurrentUserIdResolver` exposes it as `@CurrentUserId UUID` and throws when it is absent (401). Rejected: a second `findByFirebaseUid` per controller | One lookup per request, fail closed | Attacker |
| SD9 | Agent controllers are registered only when `snoozeshare.security.enabled=true` and a database is configured | With security off the chain is permit-all and no actor exists | Attacker |
| SD10 | Tickets are locked with `findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`, 5 s timeout) as the first read in `assignToMe`, `saveNotes`, `unassign`, `resolve` and `settle`, before `resolve` loads the ticket or booking (Hibernate does not refresh an already-loaded entity). Rejected: `@Version` (turns a race into a 500 retry), SERIALIZABLE on resolve (heavier retries), documenting the race. This edits W-DOMAIN code and is recorded as a Deviation | Reviewers found no lock: two agents could both settle one escrow | Tester, Attacker, Maintainer |
| SD11 | Both custom filters match `/api/guest/`, `/api/host/` and `/api/agent/` on the decoded servlet path, and `SecurityConfig` adds `hasRole` rules for the three namespaces as a second layer | The raw-URI prefix check could be bypassed by an encoded or odd path | Attacker |
| SD12 | Add ArchUnit (test scope) and `ApiLayerTest` as the web layering test; the rules apply to every role's controllers. Rejected: a source-text scan (easy to bypass) | PROJECT_STATE says the desktop layering tests are re-decided per area | Maintainer, Implementer |
| SD13 | Request size is capped by a filter on `/api/agent/**` (Content-Length required, 65,536 bytes). Rejected: a global Jackson `maxStringLength` (it would also cap the AI gateway's `OutputValidator` and auth bodies) | Scope the limit to this API | Implementer |
| SD14 | `agentNotes` moves from the shared `TicketResponse` to the agent-only `DisputeDetailResponse` | Guest and Host controllers reuse `TicketResponse` and must not be able to leak notes | Implementer, Attacker |
| SD15 | `ApiExceptionHandler` is a global `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`; `AuthController`'s local handlers still win for its routes. Rejected: a package-scoped advice, which does not see 404, 405 and 415 raised before a handler is chosen | Every agent failure uses one body | Implementer, Maintainer |
| SD16 | The audit From and To dates are whole days in a fixed `Asia/Singapore` zone (property `snoozeshare.audit.zone`, default `Asia/Singapore`), applied in `AuditServiceImpl` only. Rejected: the server default zone (UTC on Render, which puts a day boundary at 08:00 Singapore time) and changing the shared `Clock` (the cascade and phase labels also use it) | Operator, 2026-10-22: "Use Singapore timezone as default" | Operator |
| SD17 | The queue is oldest first (the repository order); `unassignedCount` is a new count query on OPEN unassigned tickets. The `status` filter is dropped from A1 (the UI never uses it). A new `GET /audit/actions` supplies the action list | Avoids two N+1 queue builds and a stale hard-coded enum | Implementer, Maintainer |
| SD19 | `AccountGovernanceServiceImpl.suspend` and `reactivate` lock the target user row (`UserRepository.findByIdForUpdate`) first. Rejected: leaving the cascade race, which could refund one booking twice | Round 2: two agents suspending one user would both see ACTIVE | Attacker, Implementer |
| SD20 | Audit reads go through a new `AuditQueryService` returning `AuditEntry` records, so controllers never depend on the write-side `AuditService` or the entity. Rejected: exempting one controller from the layering rule | Keeps N3 and N6 true | Implementer |
| SD21 | The error handler sends an `IllegalArgumentException` or `IllegalStateException` message to the client only when the exception was thrown from `service` or `domain` (first stack frame); other exceptions get fixed text. A lock wait timeout returns 409 | Framework text must not leak (N4) | Attacker |
| SD18 | The frontend does no money comparison: it checks only decimal format; the server enforces the escrow limit | A client bound would need arithmetic against the escrow, which N2 forbids | Tester |
