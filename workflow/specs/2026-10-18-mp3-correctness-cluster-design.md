# MP3-0 Correctness Cluster — Design

Date: 2026-10-18 · Workstream: MP3-0 (see `PROJECT_STATE.md` § Workstreams) ·
Source: [`docs/MP2-feedback-triage.md`](../../MP2-feedback-triage.md) items B1, B3, B4, B5, D1, D2, D3, D5, P5.

## 1. Problem

The MP2 review found four related defects that share one root cause: **a rule or side effect
lives in more than one place, so the copies drift.**

| Item | Symptom | Cause (verified in code) |
|---|---|---|
| B1 | On day `endDate+7` the sweep completes a booking (pays the host) while ticket filing is still open; every resolution then fails because the booking is `COMPLETED`. | `BookingServiceImpl.complete` is eligible at `endDate <= today-7`; `TicketServiceImpl.ticketWindowOpen` and `TripDashboardController` (duplicate copy) stay open through `endDate+7`. |
| B3 / D1 | Header wallet balance is stale after a guest cancels. | `LedgerWriter.post` moves money but each caller must separately publish `WalletTransactionRecordedEvent`; `BookingServiceImpl.cancel` does not. Five other call sites publish by hand. |
| D2 | A failed audit write leaves the entity change saved with no audit row. | Listing, review and ticket mutations call `save` then `audit.record` outside a shared transaction. |
| D3 / B4 / B5 / D5 | Past or inverted dates accepted when typed or filtered; fee rate repeated (`0.97` in `BookingServiceImpl` ×2, `0.03` in `SettlementCalculator`). | Rules enforced only in UI widgets or copied as literals. |

## 2. Goals and non-goals

**Goals:** one authoritative definition of each rule; wallet events published automatically after
commit; audit rows atomic with their entity change; every fix proven by a test written first.

**Non-goals:** B2, B6, B7 (small UI/service fixes), dead stubs (D4), documentation, CI. Moving
booking/ticket domain events onto the new queue is deferred. No cloud, outbox or connection-pool work (see § 8).

## 3. Decisions made in design

- **B1 rule (operator, 2026-10-18):** filing stays open through `endDate+7` as the guides say;
  **auto-completion moves to `endDate+8`** (complete only once the window has closed). Host payout is
  one day later. Rejected: closing filing a day earlier (changes guides/UI text, shortens guest window);
  settling from `COMPLETED` by claw-back (largest change, host may lack funds).
- **D1 mechanism (operator, 2026-10-18):** a dedicated post-commit event queue object with
  `TransactionManager` exposing only lifecycle callbacks — design B. Rejected: embedding the queue in
  `TransactionManager` (mixes responsibilities, needs hidden state to reach `LedgerWriter`).
- **Cloud (operator, 2026-10-18):** not built now; recorded as a future direction. Spec adds only cheap seams (§ 8).

## 4. Policies (D3, B1, B4, B5, D5)

New pure-Java package `domain.policy` (no JavaFX/JDBC, covered by `LayerDependencyTest`).

`DisputeWindowPolicy(Clock clock, ZoneId zone)`
- `static final int WINDOW_DAYS = 7`
- `boolean canFileTicket(Booking b)` — status `CONFIRMED` or `COMPLETED`, `today >= startDate`, `today <= endDate + WINDOW_DAYS`.
- `boolean canAutoComplete(Booking b)` — status `CONFIRMED` and `today > endDate + WINDOW_DAYS`.
- `LocalDate completionCutoff()` — `today - (WINDOW_DAYS + 1)`; the sweep query uses it.
- Invariant (tested): for every date, `canFileTicket` and `canAutoComplete` are never both true.

`StayDatePolicy(Clock clock, ZoneId zone)`
- `void requireValidStay(LocalDate start, LocalDate end)` — throws `IllegalArgumentException` if either is null, `start` is before today, or `end` is not after `start`.
- `void requireValidSearchRange(LocalDate from, LocalDate to)` — same inversion rule; past dates allowed only if search currently allows them (verify, do not change behaviour beyond inversion).

Callers: `TicketServiceImpl`, `BookingServiceImpl.complete` / `completeEligibleBookings`,
`TripDashboardController` (delete its private copy), `BookingServiceImpl.submitRequest`, the listing
search service and the Guest filter controller (UI keeps its disabling as a courtesy; the service is
authoritative).

D5: `SettlementCalculator` exposes `hostNet(BigDecimal gross)`; the two `0.97` literals in
`BookingServiceImpl` call it. Guide and UI text that state the rate are checked in the docs workstream.

## 5. Post-commit events (D1, B3)

Three single-purpose pieces:

| Piece | Responsibility | Knows about |
|---|---|---|
| `TransactionManager` | begin/commit/rollback; after commit call `listener.afterCommit()`, after rollback `listener.afterRollback()` | the `TransactionListener` interface only |
| `PostCommitEvents implements TransactionListener` | `enqueue(DomainEvent)`; `afterCommit` publishes queued events in order to the `EventBus` and clears; `afterRollback` clears | `EventBus`; no JDBC |
| `LedgerWriter` | after every successful `post`, `enqueue(new WalletTransactionRecordedEvent(...))` | `PostCommitEvents` |

Wiring: `AppContext` creates one `PostCommitEvents`, one `TransactionManager(connection, postCommitEvents)`
and injects the shared `TransactionManager` into services, replacing the nine
`new TransactionManager(connection)` sites (`BookingServiceImpl` ×3, `TransactionServiceImpl` ×2,
`WalletServiceImpl`, `DisputeSettlementServiceImpl`, `UserServiceImpl`, `AccountGovernanceServiceImpl`).
The five hand-written wallet publishes are deleted.

Guards (fail fast, each tested):
1. `enqueue` outside an active transaction throws `IllegalStateException`.
2. Nested `inTransaction` throws `IllegalStateException` (current code would commit early and flush early).
3. Subscriber failure is already isolated by `InProcessEventBus`; a flush failure must not roll back or throw into the committed caller.

`PostCommitEvents` and `TransactionListener` live in `infra.events` / `infra.db`; the `EventSink` seam is in § 8.

## 6. Audit atomicity (D2)

Wrap each mutation and its audit write in the shared `TransactionManager`:
`ListingServiceImpl` create/update/setStatus, `ReviewServiceImpl.submit`, `TicketServiceImpl`
fileTicket/assign/unassign/saveNote/category create-rename-toggle-delete. The audit repository write must join the active
connection (verify `AuditServiceImpl`/`JdbcAuditLogRepository` use the shared connection and do not open their own transaction, as `LedgerWriter` already does). Domain events (`TicketOpenedEvent` etc.)
publish after commit, outside the transaction.

## 7. Tests (written first, each must fail before its fix)

1. **Boundary (B1/P5):** booking ending 7 days ago, `CONFIRMED`: ticket can be filed; sweep does not complete it; agent resolution succeeds. At 8 days: filing rejected, sweep completes it. Service-level and one `TripDashboard` UI-state test.
2. **Policy invariant:** sweep over ±15 days around `endDate` — never both `canFileTicket` and `canAutoComplete`.
3. **Cancel event (B3):** cancelling publishes exactly one `WalletTransactionRecordedEvent` after commit.
4. **Queue semantics:** commit flushes in order; rollback discards; enqueue outside transaction throws; nested transaction throws; flush after commit survives a throwing subscriber.
5. **Every ledger write** fires exactly one event (parameterised over booking submit/reject/cancel/settle, dispute settle, suspension refund, top-up/withdraw).
6. **Audit rollback (D2):** inject a failing audit repository — listing create/update/status, review submit and each ticket mutation leave no entity change behind.
7. **Dates (B4/B5):** `submitRequest` with a past date or `end <= start` throws; search filter with inverted range throws; policy unit tests on boundary days.
8. **Fee:** `hostNet` matches the previous `0.97` results (HALF_UP, scale 2) for representative amounts.
9. Existing suite (526 tests) and Checkstyle stay green; tests that locked the old `endDate+7` completion are updated and noted in the Done ledger.

## 8. Cloud seams and single-process assumption

Single-process is a recorded Known Gap (`PROJECT_STATE.md` § Known Gaps); this spec keeps it. Cheap seams only:
- Policies take an injected `Clock` and `ZoneId` (also fixes `ZoneId.systemDefault()` use in refund timing where touched).
- `PostCommitEvents` sits behind an `EventSink` interface, so a transactional outbox with an external bus is a one-class swap.
- `completeEligibleBookings` and settlement stay idempotent (re-running on an already-completed booking is a no-op, as `complete` already returns early); add a test.
- Code comment on `PostCommitEvents`: *single shared queue assumes one connection/process; must become per-transaction if concurrency is introduced.*

Future-cloud concerns recorded, not built: per-transaction queue scope; outbox + at-least-once delivery with idempotent consumers; connection pool replacing the single `Connection` field; multi-instance sweep (row locking or single scheduler); Postgres serialization-failure retries and migration lock; per-request session identity; API boundary for the UI.

## 9. Risks

- Changing `completeEligibleBookings` timing alters payout dates; the seed DB and any test using day-7 data must be reviewed (`db/seed-mock-data.sql` bookings near the boundary).
- Replacing nine `TransactionManager` sites is mechanical but wide; do it as its own task with the full suite green before D2 builds on it.
- Guides state the 7-day rule and a 7-day payout; docs workstream must reflect "paid out after the dispute window closes (day 8)".

## 10. Suggested task order (for the plan)

1. Policies + B1/B4/B5 tests and fixes. 2. `TransactionListener`/`PostCommitEvents`/shared `TransactionManager` wiring (no behaviour change). 3. `LedgerWriter` enqueue; delete manual publishes; B3. 4. D2 audit atomicity. 5. D5 fee consolidation. 6. Docs touch-ups noted for the docs workstream.
