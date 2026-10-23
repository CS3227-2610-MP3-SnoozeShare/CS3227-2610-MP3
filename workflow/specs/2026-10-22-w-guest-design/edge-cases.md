# Edge Cases and Non-Happy Paths

IDs are permanent: never reuse or renumber. Every F requirement and every API error appears in at least one row.

## Input

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E1 | F1 | Search with no filters (all fields empty) | Returns all ACTIVE listings with `available: true` (no date range to check) | Default search must work | integration |
| E2 | F1 | Search with `endDate` ≤ `startDate` | 400 `INVALID_DATE_RANGE` "Check-out must be after check-in and dates must not be in the past" | StayDatePolicy rejects this; the controller must surface it | integration |
| E3 | F1 | Search with `startDate` in the past | 400 `INVALID_DATE_RANGE` | C53: no past dates | integration |
| E4 | F1 | Search with `guests` = 0 or negative | 400 `VALIDATION_ERROR` | Must be ≥ 1 | integration |
| E5 | F1 | Search with non-numeric `maxNightlyRate` (frontend) | Frontend shows inline validation "Enter a number or leave empty"; no API call made | Prevents bad request | unit (frontend) |
| E6 | F3 | Estimate cost with missing dates | 400 `VALIDATION_ERROR` | Both dates are required for the estimate | integration |
| E7 | F7 | File ticket with blank title or description | 400 `VALIDATION_ERROR` (Jakarta `@NotBlank`) | Required fields enforced | integration |
| E8 | F9 | Submit review with rating 0 or 6 | 400 `VALIDATION_ERROR` (`@Min(1) @Max(5)`) | Rating must be 1-5 | integration |
| E9 | F9 | Submit review with blank comment | 400 `VALIDATION_ERROR` (`@NotBlank`) | Comment is required | integration |
| E10 | F10 | Top up with amount = 0 or negative | 400 `VALIDATION_ERROR` (`@Positive`) | Mocked top-up still validates amount | integration |
| E11 | F10 | Withdraw with amount = 0 or negative | 400 `VALIDATION_ERROR` (`@Positive`) | Same validation as top-up | integration |
| E44 | F7 | File ticket with title exceeding 200 characters | 400 `VALIDATION_ERROR` (`@Size`) | Input length limits prevent oversized payloads | integration |
| E45 | F8 | Post message with body exceeding 5000 characters | 400 `VALIDATION_ERROR` (`@Size`) | Input length limits prevent oversized payloads | integration |
| E46 | F9 | Submit review with comment exceeding 2000 characters | 400 `VALIDATION_ERROR` (`@Size`) | Input length limits prevent oversized payloads | integration |

## State

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E12 | F4 | Book a listing that is inactive (deactivated by host or suspended) | 400 `LISTING_INACTIVE` | Guest should not book unavailable listings | integration |
| E13 | F4 | Book dates that are already booked by another guest | 409 `DATES_UNAVAILABLE` | SERIALIZABLE transaction catches overlap | integration |
| E14 | F4 | Book own listing (guest is also the host) | 400 `SELF_BOOKING` "Cannot book your own listing" | Prevent circular escrow | integration |
| E15 | F6 | Cancel a booking that is already CANCELLED_BY_GUEST | 400 `NOT_CANCELLABLE` | BookingStateMachine rejects the transition | integration |
| E16 | F6 | Cancel a COMPLETED booking | 400 `NOT_CANCELLABLE` | Cannot cancel after completion | integration |
| E17 | F6 | Cancel a REJECTED booking | 400 `NOT_CANCELLABLE` | Already terminal | integration |
| E18 | F7 | File dispute outside the dispute window (endDate + 8 or later) | 400 `DISPUTE_WINDOW_CLOSED` | DisputeWindowPolicy enforces C48 | integration |
| E19 | F7 | File a second ticket for the same booking | 409 `DUPLICATE_TICKET` | One ticket per booking | integration |
| E20 | F8 | Send message to a resolved ticket thread | 400 `THREAD_CLOSED` | Resolved tickets are read-only (C37) | integration |
| E21 | F8 | Send message to a closed booking conversation (>7 days after check-out) | 400 `THREAD_CLOSED` | BookingConversationService enforces the window | integration |
| E22 | F9 | Submit review for a PENDING booking | 400 `NOT_REVIEWABLE` | Only COMPLETED bookings can be reviewed | integration |
| E23 | F9 | Submit a second review for the same booking | 409 `DUPLICATE_REVIEW` | One review per booking | integration |
| E24 | F2 | View detail of an inactive listing | 404 `LISTING_NOT_FOUND` | Do not reveal existence of deactivated listings | integration |
| E25 | F10 | Withdraw more than wallet balance | 400 `INSUFFICIENT_BALANCE` | LedgerWriter prevents negative balance | integration |
| E40 | F5 | Guest has bookings in all three groups (Upcoming, Active, Past) | Each booking appears in exactly one group; Upcoming sorted by startDate asc, Active sorted by startDate asc, Past sorted by endDate desc | Grouping logic must be correct and mutually exclusive | integration |
| E41 | F5 | Guest with a CONFIRMED booking where endDate < today (past confirmed, not yet completed) | Grouped in Past, not Active | DisputeWindowPolicy may still allow filing; grouping is by date, not by completion status | integration |
| E42 | F5 | Guest has a FORCE_CANCELLED booking | Grouped in Past; no actions available (not cancellable, not reviewable, not dispute-eligible) | FORCE_CANCELLED must appear in a group and not be orphaned | integration |
| E43 | F9 | Guest tries to review a FORCE_COMPLETED booking | 400 `NOT_REVIEWABLE` — `ReviewServiceImpl` checks `COMPLETED` only | FORCE_COMPLETED is not the same as COMPLETED; the "Leave review" button must not appear | integration |

## Permission

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E26 | N1 | HOST role calls `GET /api/guest/bookings` | 403 `ROLE_MISMATCH` (from RoleAuthorizationFilter) | URL namespace isolation | integration |
| E27 | N1 | Guest A calls `POST /api/guest/bookings/{id}/cancel` with guest B's booking ID | 403 `FORBIDDEN` | Tenancy: guest can only cancel own bookings | integration |
| E28 | N1 | Guest A calls `GET /api/guest/messages/tickets/{id}` for guest B's ticket | 403 `FORBIDDEN` | Tenancy: guest can only read own ticket threads | integration |
| E29 | N1 | Unauthenticated caller hits any `/api/guest/**` endpoint | 401 `INVALID_TOKEN` | FirebaseAuthFilter rejects | integration |
| E30 | N1 | Guest A calls `GET /api/guest/wallet` (returns only A's wallet, not B's) | 200 with A's wallet only | Wallet scoped to authenticated user; no `userId` parameter | integration |

## Concurrency

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E31 | F4 | Two guests book the same listing for overlapping dates simultaneously | Exactly one succeeds with 201; the other gets 409 `DATES_UNAVAILABLE` | SERIALIZABLE transaction in BookingService prevents double-booking (Known Gap: SERIALIZABLE is the chosen strategy) | integration |
| E32 | F10 | Two simultaneous withdrawals that together exceed the balance | Exactly one succeeds; the other gets 400 `INSUFFICIENT_BALANCE` | LedgerWriter holds a row-level lock on the wallet | integration |

## Money

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E33 | F4 | Guest submits booking but wallet balance is less than totalAmount | 400 `INSUFFICIENT_BALANCE` "Insufficient wallet balance" | Escrow cannot be held without funds | integration |
| E34 | F6 | Cancel a PENDING booking | Refund 100% of totalAmount (C8) | Pending bookings get full refund | integration |
| E35 | F6 | Cancel a CONFIRMED booking more than 48h before check-in | Refund 100% of totalAmount | Policy: full refund if far enough ahead | integration |
| E36 | F6 | Cancel a CONFIRMED booking within 48h of check-in | Refund 50% of totalAmount | Policy: reduced refund for late cancellation | integration |

## Time / clock

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E37 | F4, N3 | Guest tries to book with startDate = yesterday | 400 `INVALID_DATE_RANGE` | StayDatePolicy rejects past start dates; frontend date picker also blocks this (C53) | integration |
| E38 | F7 | Guest files dispute on endDate + 7 (last eligible day) | 201 Created — ticket filed | DisputeWindowPolicy: open through endDate + 7 (C48) | integration |
| E39 | F7 | Guest files dispute on endDate + 8 | 400 `DISPUTE_WINDOW_CLOSED` | Auto-completion starts at endDate + 8 (C48) | integration |

## What could corrupt data (High tier)

- **Double booking:** Two concurrent booking requests for overlapping dates on the same listing could double-book. Guard: `BookingService.submitRequest` uses a SERIALIZABLE transaction with an overlap check before insert (Known Gap: accepted strategy).
- **Wallet balance drift:** Concurrent top-up and withdraw could leave the balance inconsistent with the sum of transactions. Guard: `LedgerWriter` holds a row-level lock on `wallets` and writes the balance atomically with the transaction row.
- **Escrow without booking:** A crash between escrow hold and booking insert could orphan funds. Guard: both happen in the same database transaction; the SERIALIZABLE isolation prevents partial commits.
- **Duplicate review or ticket:** Concurrent submissions for the same booking could create two records. Guard: `ReviewService` and `TicketService` check for existing records inside a transaction. No DB-level UNIQUE constraint exists on `tickets.booking_id` or `reviews.booking_id` (SD11: accepted as a Known Gap — the service-layer check prevents normal-use duplicates; only a concurrent race can bypass it; a V006 migration is deferred).
