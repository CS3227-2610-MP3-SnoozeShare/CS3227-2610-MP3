# W-GUEST Design

- **Status:** Approved (Nathan, 2026-10-22)
- **Risk tier:** High - money (wallet top-up/withdraw, escrow via bookings), role authorization (GUEST-only endpoints), personal data (bookings, messages, reviews)
- **Workstream:** W-GUEST | **Plan:** —
- **Constraints consulted:** C7 (no guest fee), C8 (rejected/cancelled refund 100%), C17 (escrow through dispute window), C22 (no Force Cancel/Complete), C42 (positive nightly rate ≤ 2 decimals), C48 (dispute window endDate+7), C53 (no past-date pickers), C73 (DTO layer), C78 (polling, not WebSocket), C79 (Firebase Auth, URL namespaces), C92 (UID from bearer token, client-side guards), C99 (Spring Data JPA); Known Gaps: mocked top-up/withdraw, SERIALIZABLE booking overlap + escrow, no multi-instance, client-side route guards
- **Reviewed by lenses:** Implementer, Tester, Maintainer, Attacker (money, authorization, personal data)

## 1. Problem

The W-DOMAIN workstream ported the domain model, repositories, services, DTOs and mappers to the Spring Boot backend, but there are no REST controllers exposing guest functionality and no Next.js pages for the guest role. A guest who logs in sees an empty shell with no search, booking, messaging, wallet or review capabilities. The desktop app (`desktop/src/main/java/com/snoozeshare/ui/guest/`) has all these features; the web app needs the same behaviour delivered through REST endpoints and a Next.js frontend.

**Current behaviour:** `GET /api/guest/*` returns 401 (no controller registered; the path requires authentication). The guest layout (`frontend/src/app/guest/layout.tsx`) renders an empty `RouteGuard` + `RoleShell` with no content.

**Desired behaviour:** guest REST controllers serve search, booking, wallet, messaging, ticket and review endpoints under `/api/guest/**`; the Next.js frontend provides search, trips, messages and wallet pages that call these endpoints.

## 2. Goals

More than 6 requirements; see [requirements.md](2026-10-22-w-guest-design/requirements.md) for the full list with acceptance criteria.

**Summary:** F1-F3 search and listing detail, F4-F6 booking and cancellation, F7-F8 dispute filing and messaging, F9 reviews, F10 wallet, N1-N4 security/performance/usability/audit.

## 3. Non-goals

- AI search assistant — deferred to W-AI-GUEST (Upcoming table)
- Real payment gateway integration — top-up/withdraw remain mocked (Known Gap)
- WebSocket or server-sent events for real-time updates — polling only (C78)
- Host or agent endpoints — separate workstreams (W-HOST, W-AGENT)
- Multi-instance deployment or horizontal scaling — single instance (Known Gap, C69)
- Booking completion sweep endpoint — the existing `BookingService.completeEligibleBookings()` runs server-side and is not exposed to guests; a scheduled task or host/agent trigger is deferred
- Force Cancel/Complete — agents only settle through tickets (C22)
- Audit log viewing for guests — guests see their own transactions through the wallet; the audit log tab is agent-only

## 4. Components

- [Requirements](2026-10-22-w-guest-design/requirements.md) — F1-F10, N1-N4 with acceptance criteria
- [API: Search & Booking](2026-10-22-w-guest-design/api-search-booking.md) — GuestSearchController (3 endpoints) and GuestBookingController (4 endpoints)
- [API: Messages & Tickets](2026-10-22-w-guest-design/api-messages.md) — GuestMessageController (10 endpoints: tickets, conversations, threads, mark-read)
- [API: Wallet, Review & DTOs](2026-10-22-w-guest-design/api-wallet-review.md) — GuestWalletController (4 endpoints), GuestReviewController (1 endpoint), new DTOs, security summary
- [UI](2026-10-22-w-guest-design/ui.md) — 4 pages (search, trips, messages, wallet) plus 4 dialogs
- [Conventions](2026-10-22-w-guest-design/conventions.md) — controller and frontend patterns
- [Edge cases](2026-10-22-w-guest-design/edge-cases.md) — 46 scenarios across input, state, permission, concurrency, money and time
- [Implementation order](2026-10-22-w-guest-design/implementation-order.md) — 10 tasks in 3 waves
- No schema changes: all migrations and entities were delivered by W-DOMAIN. Minor service enhancements (self-booking guard in `BookingServiceImpl`, resolved-user-ID propagation from `RoleAuthorizationFilter`) and a `@RestControllerAdvice` error handler are in scope
- No data-model.md: no new entities, tables or migrations

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Five backend controllers in `com.snoozeshare.controller.guest`: `GuestSearchController`, `GuestBookingController`, `GuestWalletController`, `GuestMessageController`, `GuestReviewController` | Matches the webapp architecture design § 3.1 and the URL namespace `/api/guest/**`; one controller per feature area keeps files short. Rejected: single monolithic controller (too large) | Spec author |
| SD2 | A new `GuestTripResponse` DTO enriches `BookingResponse` with `listingTitle`, `hostDisplayName`, `canCancel`, `canFileDispute` and `hasReview` flags, computed by the controller | The trips page needs display names and action availability; the same pattern as the existing `HostBookingRowResponse`. Rejected: returning raw `BookingResponse` and making the frontend call listing/user/review endpoints per trip (N+1 round trips) | Spec author |
| SD3 | Ticket filing and ticket-message endpoints share one controller (`GuestMessageController`) rather than a separate `GuestTicketController` | Tickets and their message threads are closely related in the guest UI (desktop `GuestMessagesController` handles both); separate controllers would split a single user flow. Rejected: separate ticket controller (adds a class for 2 endpoints) | Spec author |
| SD4 | The guest landing page (`/guest`) redirects to `/guest/search` | The desktop shell defaults to the Search (Explore) tab; search is the primary guest action | Spec author |
| SD5 | Listing detail is a Shadcn Sheet (side panel), not a separate page | Keeps the search context visible (results stay behind the sheet); matches the desktop modal overlay. Rejected: separate `/guest/search/[id]` page (loses search state, adds a route) | Spec author |
| SD6 | Cancel confirmation, ticket filing and review submission use Shadcn Dialog modals | These are short, focused forms that return to the parent page on completion; the same interaction pattern as the desktop | Spec author |
| SD7 | `RoleAuthorizationFilter` stores the resolved database user UUID as a request attribute (`snoozeshare.userId`); controllers read it via a shared helper. The filter already resolves the `AuthUser` on every request (line 46) and discards the result — this reuses that lookup | The alternative (each controller injecting `AuthUserRepository` and re-querying) crosses the auth/domain boundary in 5 places and doubles the per-request DB query. Rejected: storing the full `AuthUser` (leaks auth internals into controllers) | Review round 1 (all 4 lenses) |
| SD8 | Controllers do view-model composition (date-based trip grouping, display-name enrichment, action-flag computation, active-listing visibility filtering, ownership authorization) but do not enforce business invariants (state transitions, escrow, refund policy, dispute window). The "no business logic in controllers" convention applies to business rules; presentation and authorization checks are controller concerns | The desktop code also groups trips and enriches display data in the UI controller. Rejected: adding a `GuestTripService` (moves presentation logic into the service layer; no other consumer needs this grouping) | Review round 1 (all 4 lenses) |
| SD9 | Error responses use `{ "code": "...", "message": "...", "requestId": "..." }` matching the existing `AuthErrorResponse` (C96) pattern, via a `@RestControllerAdvice` exception handler. The field is `code`, not `error` | All 4 review lenses identified the mismatch with the existing auth error contract and the frontend `api.ts` parser (`body.code`). Rejected: introducing a second error shape with `error` instead of `code` | Review round 1 (all 4 lenses) |
| SD10 | Add a self-booking guard to `BookingServiceImpl.submitRequest` (reject when `guestId.equals(property.getHostId())`). This is a business invariant, not a controller concern | Review identified that this check is missing from the service and D12 notes "a self-booking loses an update" in the desktop code. Rejected: checking in the controller (violates SD8's invariant rule) | Review round 1 (Implementer, Attacker) |
| SD11 | Duplicate ticket and duplicate review races (concurrent check-then-insert without a DB UNIQUE constraint) are accepted as a Known Gap for this workstream. Adding a V006 migration for uniqueness constraints is a schema change better handled in a dedicated small fix | Review found no UNIQUE constraint on `tickets.booking_id` or `reviews.booking_id` in V003. The service-layer check prevents normal-use duplicates; only a concurrent race can bypass it | Review round 1 (Attacker) |
