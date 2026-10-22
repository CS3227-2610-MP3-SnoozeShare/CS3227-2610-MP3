# Conventions

Cite existing PROJECT_STATE § Conventions by ID; list only rules new or applied here.

| Rule | Applies to | Source |
|---|---|---|
| Controllers in `com.snoozeshare.controller.guest` package; one class per feature area | Backend controllers | New in this spec (SD1) |
| Controllers use `@ConditionalOnBean(PropertyRepository.class)` to avoid loading in no-DB test contexts | All guest controllers | § Conventions (existing pattern from service impls) |
| The guest's database UUID is propagated by `RoleAuthorizationFilter` as request attribute `snoozeshare.userId` (SD7); controllers read it via a shared helper. Controllers never read a user ID from the request body | All guest endpoints | C79, C92, SD7 |
| Controllers do view-model composition (trip grouping, enrichment, action flags), visibility filtering (active-listing checks) and ownership authorization, but business invariants (state transitions, escrow, refund policy) stay in services (SD8) | All guest controllers | SD8 |
| New response DTOs (`GuestTripResponse`, `PropertyDetailResponse`, `GuestConversationListResponse`) follow the existing record-with-static-`from()` pattern | New response DTOs in `api/response/` | C73 (DTO layer), existing DTO pattern |
| Checkstyle import order for backend: `java.*` then `org.*` then `com.*` then `jakarta.*`, blank lines between groups | All new Java files | § Conventions |
| Frontend pages under `frontend/src/app/guest/<feature>/page.tsx`; shared components under `frontend/src/components/guest/` | Frontend pages and components | New in this spec |
| Frontend API calls use `authenticatedFetch` from `src/lib/api.ts`; no direct `fetch` for authenticated endpoints | All frontend API calls | Existing pattern from W-AUTH |
| Date pickers disable past dates | All frontend date inputs | C53 |
| Money values display as "SGD X.XX" (2 decimal places, HALF_UP) | Frontend display | Existing desktop pattern |
| Error responses use `{ "code": "<CODE>", "message": "<human-readable>", "requestId": "<uuid>" }` matching the existing `AuthErrorResponse` pattern (C96), via a `@RestControllerAdvice` exception handler that maps service exceptions (SD9) | All guest error responses | SD9, C96 |
| State transitions go through `BookingStateMachine` / `TicketStateMachine` | Booking and ticket operations | § Conventions (all state transitions through shared state machines) |
| Only `LedgerWriter` changes a wallet balance | Wallet operations | § Conventions |
| Input length limits: ticket title max 200 chars, ticket description and supporting text max 5000, message body max 5000, review comment max 2000 (enforced by `@Size` on request DTOs) | All text inputs | New in this spec |
| Backend integration tests extend `PostgresIntegrationTest` (one JVM-wide Testcontainers Postgres) | All guest controller tests | § Conventions |
