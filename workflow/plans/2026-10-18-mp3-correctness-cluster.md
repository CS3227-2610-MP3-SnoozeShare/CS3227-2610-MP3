# MP3-0 Correctness Cluster Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix B1 (auto-complete vs dispute window), B3/D1 (stale wallet header via manual event publishing), B4/B5 (date validation), D2 (audit atomicity) and D3/D5 (duplicated rules and fee literals) with tests written first.

**Architecture:** Pure `domain.policy` classes own the date and dispute-window rules. A `PostCommitEvents` queue (implements a new `TransactionListener`) is flushed by `TransactionManager` after commit and discarded on rollback; `LedgerWriter` enqueues every wallet event, so services no longer publish wallet events. One shared `TransactionManager` is built in `AppContext` and injected; listing, review and ticket mutations run in it so their audit rows are atomic with them.

**Tech Stack:** Java 25, JavaFX 25 (UI only touched in Task 2), Gradle, SQLite via JDBC, JUnit 5. Spec: [`docs/superpowers/specs/2026-10-18-mp3-correctness-cluster-design.md`](../specs/2026-10-18-mp3-correctness-cluster-design.md).

**Commands (Git Bash, repo root `S:/CS3227/CS3227-2610-MP3`):** single test `./gradlew test --tests "com.snoozeshare.<fqcn>"`; full `./gradlew build` (runs Checkstyle + all tests, ~526 tests before this plan).

**Plan-level refinements to the spec (record in PROJECT_STATE § Deviations in Task 8):**
1. Policies take a `Clock` only; the clock carries the zone (spec said `Clock` + `ZoneId`, same effect).
2. `PostCommitEvents` has a `strict` mode (production: `enqueue` outside a transaction throws) and an explicit `autocommit` mode (publishes immediately; used only by `LedgerTestSupport` for tests that post to the ledger outside any transaction).
3. `UserServiceImpl` keeps its own `new TransactionManager(connection)` — it never posts to the ledger, so it needs no listener (8 production sites change, not 9).
4. `TransactionManager` gains `inTransactionUnchecked(Supplier)` to avoid repeating SQLException wrapping in three more services.
5. `WalletServiceImpl` and `TransactionServiceImpl` lose their `EventBus` constructor parameter (it only carried wallet events).
6. Event order for dispute settlement changes: wallet events (guest refund, host payout, platform fee) now fire at commit, before `TicketResolvedEvent`. `postPayout` now emits two wallet events (payout + System fee row); the two UI subscribers (`NavShellController`, `WalletDashboardController`) refresh by wallet and ignore other wallets.

---

## File structure

**Create**
- `src/main/java/com/snoozeshare/domain/policy/DisputeWindowPolicy.java` — filing/auto-complete rules, one constant.
- `src/main/java/com/snoozeshare/domain/policy/StayDatePolicy.java` — stay and search-range date rules.
- `src/main/java/com/snoozeshare/infra/db/TransactionListener.java` — begin/commit/rollback callbacks.
- `src/main/java/com/snoozeshare/infra/events/EventSink.java` — one-method publish seam (cloud swap point).
- `src/main/java/com/snoozeshare/infra/events/PostCommitEvents.java` — queue + listener.
- `src/test/java/com/snoozeshare/domain/policy/DisputeWindowPolicyTest.java`, `StayDatePolicyTest.java`
- `src/test/java/com/snoozeshare/infra/db/TransactionManagerTest.java`
- `src/test/java/com/snoozeshare/infra/events/PostCommitEventsTest.java`
- `src/test/java/com/snoozeshare/service/DisputeWindowBoundaryTest.java`
- `src/test/java/com/snoozeshare/service/WalletEventPublishingTest.java`
- `src/test/java/com/snoozeshare/service/AuditAtomicityTest.java`
- `src/test/java/com/snoozeshare/testsupport/Tx.java` — wires strict `PostCommitEvents` + shared `TransactionManager` + `LedgerWriter` for tests.

**Modify (main)** `TransactionManager`, `LedgerWriter`, `BookingServiceImpl`, `TransactionServiceImpl`, `WalletServiceImpl`, `DisputeSettlementServiceImpl`, `AccountGovernanceServiceImpl`, `ListingServiceImpl`, `ReviewServiceImpl`, `TicketServiceImpl`, `SettlementCalculator`, `AppContext`, `TripDashboardController`, `GuestSearchController` (error display only if it does not already show service errors).

**Modify (tests)** every test that constructs the changed services (listed by grep in Tasks 5–6), `LedgerTestSupport`, `BookingServiceTest` (day-7 fixtures).

---

### Task 1: Policy classes (D3)

**Files:**
- Create: `src/main/java/com/snoozeshare/domain/policy/DisputeWindowPolicy.java`
- Create: `src/main/java/com/snoozeshare/domain/policy/StayDatePolicy.java`
- Test: `src/test/java/com/snoozeshare/domain/policy/DisputeWindowPolicyTest.java`, `StayDatePolicyTest.java`

- [ ] **Step 1: Write the failing tests**

`DisputeWindowPolicyTest.java`:

```java
package com.snoozeshare.domain.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;

class DisputeWindowPolicyTest {

    private static final LocalDate END = LocalDate.of(2026, 9, 5);

    private static DisputeWindowPolicy on(LocalDate today) {
        return new DisputeWindowPolicy(Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private static Booking booking(BookingStatus status) {
        return new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), END.minusDays(4), END, status,
                new BigDecimal("100.00"), new BigDecimal("400.00"), Instant.parse("2026-08-20T00:00:00Z"),
                Instant.parse("2026-08-21T00:00:00Z"), null);
    }

    @Test
    void filingIsOpenThroughSevenDaysAfterCheckoutAndClosedOnTheEighth() {
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        assertTrue(on(END.plusDays(7)).canFileTicket(confirmed));
        assertFalse(on(END.plusDays(8)).canFileTicket(confirmed));
    }

    @Test
    void filingNeedsTheTripToHaveStarted() {
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        assertFalse(on(END.minusDays(5)).canFileTicket(confirmed));
        assertTrue(on(END.minusDays(4)).canFileTicket(confirmed));
    }

    @Test
    void filingNeedsConfirmedOrCompletedStatus() {
        assertTrue(on(END).canFileTicket(booking(BookingStatus.COMPLETED)));
        assertFalse(on(END).canFileTicket(booking(BookingStatus.PENDING)));
        assertFalse(on(END).canFileTicket(booking(BookingStatus.CANCELLED_BY_GUEST)));
    }

    @Test
    void autoCompleteOnlyAfterTheFilingWindowCloses() {
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        assertFalse(on(END.plusDays(7)).canAutoComplete(confirmed));
        assertTrue(on(END.plusDays(8)).canAutoComplete(confirmed));
        assertFalse(on(END.plusDays(8)).canAutoComplete(booking(BookingStatus.COMPLETED)));
    }

    @Test
    void completionCutoffIsTheLastCheckoutDateTheSweepMayComplete() {
        assertEquals(END, on(END.plusDays(8)).completionCutoff());
    }

    @Test
    void neverBothFilableAndAutoCompletableOnAnyDay() {
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        for (int offset = -15; offset <= 15; offset++) {
            DisputeWindowPolicy policy = on(END.plusDays(offset));
            assertFalse(policy.canFileTicket(confirmed) && policy.canAutoComplete(confirmed),
                    "both true at END+" + offset);
        }
    }
}
```

`StayDatePolicyTest.java`:

```java
package com.snoozeshare.domain.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class StayDatePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);
    private final StayDatePolicy policy =
            new StayDatePolicy(Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @Test
    void acceptsTodayAsStartAndAFutureEnd() {
        assertDoesNotThrow(() -> policy.requireValidStay(TODAY, TODAY.plusDays(2)));
    }

    @Test
    void rejectsAStartBeforeToday() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.requireValidStay(TODAY.minusDays(1), TODAY.plusDays(2)));
    }

    @Test
    void rejectsEndNotAfterStartAndNulls() {
        assertThrows(IllegalArgumentException.class, () -> policy.requireValidStay(TODAY.plusDays(3), TODAY.plusDays(3)));
        assertThrows(IllegalArgumentException.class, () -> policy.requireValidStay(TODAY.plusDays(3), TODAY.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> policy.requireValidStay(null, TODAY.plusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> policy.requireValidStay(TODAY, null));
    }

    @Test
    void searchRangeRejectsInversionButAllowsOpenOrPartialRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.requireValidSearchRange(TODAY.plusDays(5), TODAY.plusDays(2)));
        assertDoesNotThrow(() -> policy.requireValidSearchRange(null, null));
        assertDoesNotThrow(() -> policy.requireValidSearchRange(TODAY.plusDays(1), null));
        assertDoesNotThrow(() -> policy.requireValidSearchRange(TODAY.plusDays(1), TODAY.plusDays(4)));
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew test --tests "com.snoozeshare.domain.policy.*"`
Expected: compilation FAIL (`DisputeWindowPolicy`/`StayDatePolicy` do not exist).

- [ ] **Step 3: Implement**

`DisputeWindowPolicy.java`:

```java
package com.snoozeshare.domain.policy;

import java.time.Clock;
import java.time.LocalDate;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;

/**
 * The one definition of the dispute window. Filing stays open through {@link #WINDOW_DAYS} days after check-out;
 * auto-completion (which pays the host) is allowed only once that window has closed, so an accepted complaint
 * always reaches a booking that can still be settled.
 */
public final class DisputeWindowPolicy {

    public static final int WINDOW_DAYS = 7;

    private final Clock clock;

    public DisputeWindowPolicy(Clock clock) {
        this.clock = clock;
    }

    /** CONFIRMED or COMPLETED, trip started, and not later than check-out plus {@value #WINDOW_DAYS} days. */
    public boolean canFileTicket(Booking booking) {
        LocalDate today = LocalDate.now(clock);
        boolean statusEligible = booking.status() == BookingStatus.CONFIRMED
                || booking.status() == BookingStatus.COMPLETED;
        return statusEligible && !today.isBefore(booking.startDate())
                && !today.isAfter(booking.endDate().plusDays(WINDOW_DAYS));
    }

    /** A CONFIRMED booking whose filing window has closed. */
    public boolean canAutoComplete(Booking booking) {
        return booking.status() == BookingStatus.CONFIRMED
                && LocalDate.now(clock).isAfter(booking.endDate().plusDays(WINDOW_DAYS));
    }

    /** The latest check-out date the completion sweep may complete today. */
    public LocalDate completionCutoff() {
        return LocalDate.now(clock).minusDays(WINDOW_DAYS + 1L);
    }
}
```

`StayDatePolicy.java`:

```java
package com.snoozeshare.domain.policy;

import java.time.Clock;
import java.time.LocalDate;

/** The one definition of which stay and search dates are acceptable; the UI only mirrors it. */
public final class StayDatePolicy {

    private final Clock clock;

    public StayDatePolicy(Clock clock) {
        this.clock = clock;
    }

    /** A booking needs both dates, a start of today or later, and an end after the start. */
    public void requireValidStay(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (start.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("EndDate must be after startDate");
        }
    }

    /** A search may leave either date open, but a given range must not be inverted. */
    public void requireValidSearchRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }
}
```

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew test --tests "com.snoozeshare.domain.policy.*" --tests "com.snoozeshare.architecture.LayerDependencyTest"`
Expected: PASS (the architecture test confirms `domain.policy` imports no JavaFX/`java.sql`).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/snoozeshare/domain/policy src/test/java/com/snoozeshare/domain/policy
git commit -m "feat(domain): add DisputeWindowPolicy and StayDatePolicy"
```

---

### Task 2: Apply the dispute-window policy (B1)

**Files:**
- Modify: `src/main/java/com/snoozeshare/service/impl/TicketServiceImpl.java:84-87,118-124`
- Modify: `src/main/java/com/snoozeshare/service/impl/BookingServiceImpl.java` (constructor, `complete`, `completeEligibleBookings`)
- Modify: `src/main/java/com/snoozeshare/app/AppContext.java:102-111`
- Modify: `src/main/java/com/snoozeshare/ui/guest/trips/TripDashboardController.java:332-341`
- Test: `src/test/java/com/snoozeshare/service/DisputeWindowBoundaryTest.java`; update `BookingServiceTest` (lines ~574-626) and any other test building `BookingServiceImpl`.

- [ ] **Step 1: Write the failing boundary test**

`DisputeWindowBoundaryTest.java` (uses `BookingServiceTest`'s package-private helpers):

```java
package com.snoozeshare.service;

import static com.snoozeshare.service.BookingServiceTest.createService;
import static com.snoozeshare.service.BookingServiceTest.migratedConnection;
import static com.snoozeshare.service.BookingServiceTest.seedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.repository.jdbc.JdbcBookingRepository;

/** Guards the joined boundary TA1 found: completion must never pay the host while filing is still open. */
class DisputeWindowBoundaryTest {

    private static Booking confirmedEndedDaysAgo(Connection connection, BookingTestIds ids, BookingService service,
                                                 int daysAgo) {
        Booking booking = service.submitRequest(ids.guestId(), ids.propertyId(),
                LocalDate.now().minusDays(daysAgo + 3L), LocalDate.now().minusDays(daysAgo));
        return new JdbcBookingRepository(connection).save(new Booking(booking.bookingId(), booking.listingId(),
                booking.guestId(), booking.startDate(), booking.endDate(), BookingStatus.CONFIRMED,
                booking.nightlyRateSnapshot(), booking.totalAmount(), booking.createdAt(), Instant.now(), null));
    }

    @Test
    void sweepDoesNotCompleteABookingWhoseFilingWindowIsStillOpen() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("1000.00"));
            BookingService service = createService(connection);
            Booking onDaySeven = confirmedEndedDaysAgo(connection, new BookingTestIds(ctx.guestId(), ctx.propertyId()),
                    service, 7);

            assertEquals(0, service.completeEligibleBookings());
            assertThrows(IllegalStateException.class, () -> service.complete(onDaySeven.bookingId()));
            assertEquals(BookingStatus.CONFIRMED,
                    new JdbcBookingRepository(connection).findById(onDaySeven.bookingId()).orElseThrow().status());
        }
    }

    @Test
    void sweepCompletesOnceTheWindowHasClosed() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("1000.00"));
            BookingService service = createService(connection);
            Booking onDayEight = confirmedEndedDaysAgo(connection, new BookingTestIds(ctx.guestId(), ctx.propertyId()),
                    service, 8);

            assertEquals(1, service.completeEligibleBookings());
            assertEquals(BookingStatus.COMPLETED,
                    new JdbcBookingRepository(connection).findById(onDayEight.bookingId()).orElseThrow().status());
        }
    }

    private record BookingTestIds(java.util.UUID guestId, java.util.UUID propertyId) {
    }
}
```

`seedContext` returns the private record `TestContext`; widen it: in `BookingServiceTest` change `private record TestContext(...)` to `record TestContext(...)` (package-private) so `ctx.guestId()` and `ctx.propertyId()` accessors resolve from this test class.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew test --tests "com.snoozeshare.service.DisputeWindowBoundaryTest"`
Expected: `sweepDoesNotCompleteABookingWhoseFilingWindowIsStillOpen` FAILS (current code completes at `endDate <= today-7`, so `completeEligibleBookings()` returns 1). The day-eight test passes already.

- [ ] **Step 3: Wire the policy**

In `BookingServiceImpl`: add `import java.time.Clock;` and `import com.snoozeshare.domain.policy.DisputeWindowPolicy;`, add field `private final DisputeWindowPolicy disputeWindow;`, add a final constructor parameter `Clock clock` (after `TicketRepository tickets`) and `this.disputeWindow = new DisputeWindowPolicy(clock);`. Replace the eligibility check and sweep in `complete`/`completeEligibleBookings`:

```java
        if (booking.status() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be completed");
        }
        if (!disputeWindow.canAutoComplete(booking)) {
            throw new IllegalStateException("Booking is not eligible for completion");
        }
```
(replacing the `endDate().isAfter(LocalDate.now().minusDays(7))` block), and

```java
    public int completeEligibleBookings() {
        int completed = 0;
        for (Booking booking : bookings.findConfirmedEndingOnOrBefore(disputeWindow.completionCutoff())) {
```
(replacing the `LocalDate cutoff = ...` lines). Remove the now-unused `LocalDate` import only if no other use remains (`submitRequest` still uses it).

In `TicketServiceImpl`: add `import com.snoozeshare.domain.policy.DisputeWindowPolicy;`, add `private final DisputeWindowPolicy disputeWindow;`, set `this.disputeWindow = new DisputeWindowPolicy(clock);` in the 8-arg constructor, and replace lines 84-87 and delete `ticketWindowOpen`:

```java
        if (!disputeWindow.canFileTicket(booking)) {
            throw new IllegalStateException("Booking is not eligible for a dispute");
        }
```
Remove unused imports (`LocalDate`) if the compiler/Checkstyle flags them.

In `AppContext`: change `Clock clock = Clock.systemUTC();` to `Clock clock = Clock.systemDefaultZone();` (so the policy, the sweep and the UI all use the same local calendar date; only `LocalDate` derivation changes, `Instant`s are zone-independent), and pass `clock` as the new last argument to `new BookingServiceImpl(...)`. Because `clock` is declared after the `BookingServiceImpl` construction, move the `Clock clock = ...` line to just after `this.eventBus = new InProcessEventBus();`.

In `TripDashboardController`, add a field `private final DisputeWindowPolicy disputeWindow = new DisputeWindowPolicy(Clock.systemDefaultZone());` (imports `java.time.Clock`, `com.snoozeshare.domain.policy.DisputeWindowPolicy`) and replace the body of `canFileDispute` after the `bookedTicketIds` guard:

```java
        return disputeWindow.canFileTicket(booking);
```
Drop the now-unused locals; remove `LocalDate`/`BookingStatus` imports only if unused elsewhere in the file (`BookingStatus` is still used by `canReview`).

- [ ] **Step 4: Update the existing tests that assumed completion at day 7**

In `BookingServiceTest` change the fixtures in `completeSettlesNetHostPayoutAndIsIdempotent` and `completeBlocksBookingWithOpenTicket` from `LocalDate.now().minusDays(10), LocalDate.now().minusDays(7)` to `LocalDate.now().minusDays(11), LocalDate.now().minusDays(8)`. In `createService(Connection, InProcessEventBus)` add `Clock.systemDefaultZone()` as the new last constructor argument (and `import java.time.Clock;`). Find any other constructor call: `grep -rn "new BookingServiceImpl(" src` (expect only `BookingServiceTest` and `AppContext`).

Also check seed data near the boundary: `grep -n "endDate\|checkout" db/seed-mock-data.sql | head` and confirm no seeded CONFIRMED booking ends exactly 7 days before the seed's reference date in a way a test depends on; if `CommittedMockDbTest` or `MockDbFixtureTest` fail in Step 5, adjust the specific assertion, not the policy.

- [ ] **Step 5: Run the tests**

Run: `./gradlew test --tests "com.snoozeshare.service.DisputeWindowBoundaryTest" --tests "com.snoozeshare.service.BookingServiceTest" --tests "com.snoozeshare.service.FileTicketTest" --tests "com.snoozeshare.service.TicketService*" --tests "com.snoozeshare.ui.guest.*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A src
git commit -m "fix(booking): complete only after the dispute window closes (B1)"
```

---

### Task 3: Stay date validation (B4, B5)

**Files:**
- Modify: `src/main/java/com/snoozeshare/service/impl/BookingServiceImpl.java:84-87` (uses the `clock` added in Task 2)
- Modify: `src/main/java/com/snoozeshare/service/impl/ListingServiceImpl.java` (constructor unchanged here; `search`)
- Modify: `src/main/java/com/snoozeshare/ui/guest/search/GuestSearchController.java` (only the error display)
- Test: `src/test/java/com/snoozeshare/service/BookingServiceTest.java`, `ListingServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Append to `BookingServiceTest` (inside the class, in the submitRequest section):

```java
    @Test
    void submitRequestRejectsAStartDateInThePast() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            BookingService service = createService(connection);

            assertThrows(IllegalArgumentException.class, () -> service.submitRequest(ctx.guestId, ctx.propertyId,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(2)));
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT COUNT(*) FROM bookings")) {
                rows.next();
                assertEquals(0, rows.getInt(1));
            }
        }
    }
```

Append to `ListingServiceTest` (use its existing construction helper for the service; if it builds `ListingServiceImpl` with fakes, reuse that; the call below only needs a `ListingService service` variable and a `SearchCriteria` — copy the construction used by the nearest existing `search` test in that file):

```java
    @Test
    void searchRejectsAnInvertedDateRange() {
        SearchCriteria inverted = existingCriteriaWithDates(LocalDate.now().plusDays(9), LocalDate.now().plusDays(4));

        assertThrows(IllegalArgumentException.class, () -> service.search(inverted));
    }
```
Implement `existingCriteriaWithDates` by copying how the nearest existing date-range search test in `ListingServiceTest` builds `SearchCriteria` (same constructor arguments) with `startDate`/`endDate` set to the two arguments.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew test --tests "com.snoozeshare.service.BookingServiceTest.submitRequestRejectsAStartDateInThePast" --tests "com.snoozeshare.service.ListingServiceTest.searchRejectsAnInvertedDateRange"`
Expected: both FAIL (no exception thrown).

- [ ] **Step 3: Implement**

`BookingServiceImpl`: add field `private final StayDatePolicy stayDates;` (import `com.snoozeshare.domain.policy.StayDatePolicy`), set `this.stayDates = new StayDatePolicy(clock);` next to `disputeWindow`, and replace `DomainValidation.requireDateRange(start, end);` with `stayDates.requireValidStay(start, end);` (keep the `DomainValidation` import if still used, else remove it).

`ListingServiceImpl`: add `private static final StayDatePolicy SEARCH_DATES = new StayDatePolicy(Clock.systemDefaultZone());` (imports `java.time.Clock`, `com.snoozeshare.domain.policy.StayDatePolicy`) and at the top of `search`:

```java
        SEARCH_DATES.requireValidSearchRange(criteria.startDate(), criteria.endDate());
```
(`requireValidSearchRange` does not read the clock, so a static is safe; add a short comment saying so.)

`GuestSearchController` (line ~104 `context.listingService().search(criteria)`): check whether the call is already inside a try/catch that shows the message. If not, wrap it:

```java
        List<SearchResult> results;
        try {
            results = context.listingService().search(criteria);
        } catch (IllegalArgumentException invalid) {
            showSearchError(invalid.getMessage());
            return;
        }
```
where `showSearchError` reuses whatever existing status/label mechanism the controller already has for messages (read the file first and call that; do not invent a new widget).

- [ ] **Step 4: Run the tests**

Run: `./gradlew test --tests "com.snoozeshare.service.BookingServiceTest" --tests "com.snoozeshare.service.ListingServiceTest" --tests "com.snoozeshare.ui.guest.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "fix(booking): validate stay and search dates in the service (B4, B5)"
```

---

### Task 4: Transaction listener and post-commit queue (D1 infrastructure, no behaviour change yet)

**Files:**
- Create: `src/main/java/com/snoozeshare/infra/db/TransactionListener.java`
- Modify: `src/main/java/com/snoozeshare/infra/db/TransactionManager.java`
- Create: `src/main/java/com/snoozeshare/infra/events/EventSink.java`, `PostCommitEvents.java`
- Test: `src/test/java/com/snoozeshare/infra/db/TransactionManagerTest.java`, `src/test/java/com/snoozeshare/infra/events/PostCommitEventsTest.java`

- [ ] **Step 1: Write the failing tests**

`PostCommitEventsTest.java`:

```java
package com.snoozeshare.infra.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PostCommitEventsTest {

    private record TestEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
        TestEvent() {
            this(UUID.randomUUID(), Instant.now());
        }
    }

    private final List<DomainEvent> published = new ArrayList<>();

    @Test
    void commitPublishesQueuedEventsInOrder() {
        PostCommitEvents events = PostCommitEvents.strict(published::add);
        TestEvent first = new TestEvent();
        TestEvent second = new TestEvent();
        events.onBegin();
        events.enqueue(first);
        events.enqueue(second);
        assertTrue(published.isEmpty(), "nothing is published before commit");
        events.afterCommit();
        assertEquals(List.of(first, second), published);
    }

    @Test
    void rollbackDiscardsQueuedEvents() {
        PostCommitEvents events = PostCommitEvents.strict(published::add);
        events.onBegin();
        events.enqueue(new TestEvent());
        events.afterRollback();
        events.onBegin();
        events.afterCommit();
        assertTrue(published.isEmpty());
    }

    @Test
    void strictModeRejectsEnqueueOutsideATransaction() {
        PostCommitEvents events = PostCommitEvents.strict(published::add);
        assertThrows(IllegalStateException.class, () -> events.enqueue(new TestEvent()));
    }

    @Test
    void autocommitModePublishesImmediatelyOutsideATransaction() {
        PostCommitEvents events = PostCommitEvents.autocommit(published::add);
        TestEvent event = new TestEvent();
        events.enqueue(event);
        assertEquals(List.of(event), published);
    }

    @Test
    void beginningWhileActiveIsRejectedAsNesting() {
        PostCommitEvents events = PostCommitEvents.strict(published::add);
        events.onBegin();
        assertThrows(IllegalStateException.class, events::onBegin);
    }

    @Test
    void aFailingSinkDoesNotEscapeAfterCommit() {
        List<DomainEvent> delivered = new ArrayList<>();
        PostCommitEvents events = PostCommitEvents.strict(event -> {
            if (delivered.isEmpty()) {
                delivered.add(event);
                throw new IllegalStateException("subscriber blew up");
            }
            delivered.add(event);
        });
        events.onBegin();
        events.enqueue(new TestEvent());
        events.enqueue(new TestEvent());
        events.afterCommit();
        assertEquals(2, delivered.size(), "remaining events are still delivered");
    }
}
```

`TransactionManagerTest.java`:

```java
package com.snoozeshare.infra.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TransactionManagerTest {

    private static final class Recorder implements TransactionListener {
        final List<String> calls = new ArrayList<>();

        @Override
        public void onBegin() {
            calls.add("begin");
        }

        @Override
        public void afterCommit() {
            calls.add("commit");
        }

        @Override
        public void afterRollback() {
            calls.add("rollback");
        }
    }

    @Test
    void notifiesListenerAfterCommit() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            Recorder recorder = new Recorder();
            String result = new TransactionManager(connection, recorder).inTransaction(conn -> "ok");
            assertEquals("ok", result);
            assertEquals(List.of("begin", "commit"), recorder.calls);
            assertEquals(true, connection.getAutoCommit(), "autocommit is restored before the commit callback runs");
        }
    }

    @Test
    void notifiesListenerAfterRollback() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            Recorder recorder = new Recorder();
            assertThrows(IllegalArgumentException.class, () -> new TransactionManager(connection, recorder)
                    .inTransaction(conn -> {
                        throw new IllegalArgumentException("boom");
                    }));
            assertEquals(List.of("begin", "rollback"), recorder.calls);
        }
    }

    @Test
    void commitCallbackMayStartAnotherTransaction() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            TransactionManager[] holder = new TransactionManager[1];
            List<String> order = new ArrayList<>();
            TransactionListener listener = new TransactionListener() {
                private boolean first = true;

                @Override
                public void onBegin() {
                    order.add("begin");
                }

                @Override
                public void afterCommit() {
                    order.add("commit");
                    if (first) {
                        first = false;
                        try {
                            holder[0].inTransaction(conn -> "second");
                        } catch (java.sql.SQLException exception) {
                            throw new IllegalStateException(exception);
                        }
                    }
                }

                @Override
                public void afterRollback() {
                    order.add("rollback");
                }
            };
            holder[0] = new TransactionManager(connection, listener);
            holder[0].inTransaction(conn -> "first");
            assertEquals(List.of("begin", "commit", "begin", "commit"), order);
        }
    }

    @Test
    void nestedTransactionsAreRejected() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            TransactionManager manager = new TransactionManager(connection);
            assertThrows(IllegalStateException.class, () -> manager.inTransaction(outer ->
                    manager.inTransaction(inner -> "nested")));
        }
    }

    @Test
    void uncheckedVariantWrapsSqlFailuresAndKeepsRuntimeExceptions() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            TransactionManager manager = new TransactionManager(connection);
            assertThrows(IllegalArgumentException.class, () -> manager.inTransactionUnchecked(() -> {
                throw new IllegalArgumentException("domain failure");
            }));
            assertEquals("done", manager.inTransactionUnchecked(() -> "done"));
        }
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew test --tests "com.snoozeshare.infra.db.TransactionManagerTest" --tests "com.snoozeshare.infra.events.PostCommitEventsTest"`
Expected: compilation FAIL (new types do not exist).

- [ ] **Step 3: Implement**

`TransactionListener.java`:

```java
package com.snoozeshare.infra.db;

/** Lifecycle callbacks a {@link TransactionManager} fires; it knows nothing about what a listener does. */
public interface TransactionListener {

    /** Ignores every callback; used when nothing needs to react to the transaction. */
    TransactionListener NONE = new TransactionListener() {
        @Override
        public void onBegin() {
            // nothing to do
        }

        @Override
        public void afterCommit() {
            // nothing to do
        }

        @Override
        public void afterRollback() {
            // nothing to do
        }
    };

    void onBegin();

    /** Called after the commit, with the connection's auto-commit already restored. */
    void afterCommit();

    /** Called after the rollback, with the connection's auto-commit already restored. */
    void afterRollback();
}
```

`TransactionManager.java` (replace the whole file):

```java
package com.snoozeshare.infra.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Runs work in one database transaction and reports its outcome to a {@link TransactionListener}. Transactions
 * do not nest: a second {@code inTransaction} while one is open throws.
 */
public final class TransactionManager {

    private final Connection connection;
    private final TransactionListener listener;

    public TransactionManager(Connection connection) {
        this(connection, TransactionListener.NONE);
    }

    public TransactionManager(Connection connection, TransactionListener listener) {
        this.connection = connection;
        this.listener = listener;
    }

    public <T> T inTransaction(SqlWork<T> work) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        if (!originalAutoCommit) {
            throw new IllegalStateException("Nested transactions are not supported");
        }
        connection.setAutoCommit(false);
        listener.onBegin();
        T result;
        try {
            result = work.apply(connection);
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.rollback();
            } finally {
                connection.setAutoCommit(originalAutoCommit);
                listener.afterRollback();
            }
            throw exception;
        }
        try {
            connection.setAutoCommit(originalAutoCommit);
        } finally {
            listener.afterCommit();
        }
        return result;
    }

    /** Like {@link #inTransaction} for work that throws no checked exception; SQL failures become unchecked. */
    public <T> T inTransactionUnchecked(Supplier<T> work) {
        try {
            return inTransaction(conn -> work.get());
        } catch (SQLException exception) {
            throw new IllegalStateException("Database transaction failed", exception);
        }
    }
}
```

`EventSink.java`:

```java
package com.snoozeshare.infra.events;

/** Where committed events go. Today it is the in-process bus; a transactional outbox would replace it later. */
@FunctionalInterface
public interface EventSink {
    void publish(DomainEvent event);
}
```

`PostCommitEvents.java`:

```java
package com.snoozeshare.infra.events;

import java.util.ArrayList;
import java.util.List;

import com.snoozeshare.infra.db.TransactionListener;

/**
 * Queues events during a transaction and publishes them, in order, only after it commits; a rollback discards
 * them. Single-process assumption: this one shared queue relies on there being one connection and one thread of
 * work; it must become per-transaction if concurrency is ever introduced (see PROJECT_STATE § Known Gaps).
 */
public final class PostCommitEvents implements TransactionListener {

    private final EventSink sink;
    private final boolean autocommitMode;
    private final List<DomainEvent> pending = new ArrayList<>();
    private boolean active;

    private PostCommitEvents(EventSink sink, boolean autocommitMode) {
        this.sink = sink;
        this.autocommitMode = autocommitMode;
    }

    /** Production mode: enqueuing outside a transaction is a programming error and throws. */
    public static PostCommitEvents strict(EventSink sink) {
        return new PostCommitEvents(sink, false);
    }

    /** For code that runs in auto-commit mode (tests): outside a transaction each write is already committed. */
    public static PostCommitEvents autocommit(EventSink sink) {
        return new PostCommitEvents(sink, true);
    }

    public void enqueue(DomainEvent event) {
        if (!active) {
            if (autocommitMode) {
                publish(event);
                return;
            }
            throw new IllegalStateException("Events can only be queued inside a transaction");
        }
        pending.add(event);
    }

    @Override
    public void onBegin() {
        if (active) {
            throw new IllegalStateException("Nested transactions are not supported");
        }
        active = true;
        pending.clear();
    }

    @Override
    public void afterCommit() {
        active = false;
        List<DomainEvent> toPublish = new ArrayList<>(pending);
        pending.clear();
        toPublish.forEach(this::publish);
    }

    @Override
    public void afterRollback() {
        active = false;
        pending.clear();
    }

    private void publish(DomainEvent event) {
        try {
            sink.publish(event);
        } catch (RuntimeException ignored) {
            // The change is already committed; one failing subscriber must not hide it or stop later events.
        }
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew test --tests "com.snoozeshare.infra.db.TransactionManagerTest" --tests "com.snoozeshare.infra.events.PostCommitEventsTest" --tests "com.snoozeshare.infra.db.DatabaseBootstrapTest" --tests "com.snoozeshare.architecture.*"`
Expected: PASS. If an existing test (`DatabaseBootstrapTest`, any other) nests `inTransaction` or starts one on a connection already in manual mode, the new nested guard throws — fix that test to stop nesting; do not weaken the guard. If the architecture test objects to `infra.events` importing `infra.db`, move `TransactionListener` and `TransactionManager`'s callback usage unchanged but put `PostCommitEvents` in `infra.db` instead and update the imports in later tasks accordingly.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "feat(infra): add TransactionListener and PostCommitEvents queue"
```

---

### Task 5: Automatic wallet events and shared TransactionManager (D1, B3)

This task changes constructors across services and tests, so it is one compile-coupled task. Work service by service; compile only when all of main is done.

**Files:**
- Create: `src/test/java/com/snoozeshare/testsupport/Tx.java`; Modify `LedgerTestSupport.java`
- Modify: `LedgerWriter`, `WalletServiceImpl`, `TransactionServiceImpl`, `BookingServiceImpl`, `DisputeSettlementServiceImpl`, `AccountGovernanceServiceImpl`, `AppContext`
- Test: `src/test/java/com/snoozeshare/service/WalletEventPublishingTest.java`; update every test constructing those services

- [ ] **Step 1: Write the test harness and failing tests**

`src/test/java/com/snoozeshare/testsupport/Tx.java`:

```java
package com.snoozeshare.testsupport;

import java.sql.Connection;

import com.snoozeshare.infra.db.TransactionManager;
import com.snoozeshare.infra.events.EventBus;
import com.snoozeshare.infra.events.InProcessEventBus;
import com.snoozeshare.infra.events.PostCommitEvents;
import com.snoozeshare.repository.jdbc.JdbcWalletRepository;
import com.snoozeshare.service.impl.LedgerWriter;

/** Wires a shared transaction manager, a strict post-commit queue and a ledger writer the way AppContext does. */
public record Tx(PostCommitEvents events, TransactionManager manager, LedgerWriter ledger, EventBus bus) {

    public static Tx of(Connection connection) {
        return of(connection, new InProcessEventBus());
    }

    public static Tx of(Connection connection, EventBus bus) {
        PostCommitEvents events = PostCommitEvents.strict(bus::publish);
        LedgerWriter ledger = new LedgerWriter(new JdbcWalletRepository(connection),
                LedgerTestSupport.audit(connection), events);
        return new Tx(events, new TransactionManager(connection, events), ledger, bus);
    }
}
```

Modify `LedgerTestSupport`: both `writer(...)` overloads build `new LedgerWriter(new JdbcWalletRepository(connection), <audit>, PostCommitEvents.autocommit(event -> { }))` (import `com.snoozeshare.infra.events.PostCommitEvents`). These are for tests that post to the ledger outside any transaction.

`WalletEventPublishingTest.java`:

```java
package com.snoozeshare.service;

import static com.snoozeshare.service.BookingServiceTest.migratedConnection;
import static com.snoozeshare.service.BookingServiceTest.seedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.infra.events.InProcessEventBus;
import com.snoozeshare.infra.events.events.WalletTransactionRecordedEvent;
import com.snoozeshare.repository.jdbc.JdbcLedgerRepository;

/** The ledger writer queues the wallet event itself, so no caller can forget it (MP2 feedback B3 / D1). */
class WalletEventPublishingTest {

    @Test
    void cancellingABookingPublishesTheRefundEventOnce() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            var bus = new InProcessEventBus();
            BookingService service = BookingServiceTest.createService(connection, bus);
            Booking booking = service.submitRequest(ctx.guestId(), ctx.propertyId(),
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(13));
            List<WalletTransactionRecordedEvent> events = new ArrayList<>();
            bus.subscribe(WalletTransactionRecordedEvent.class, events::add);

            service.cancel(booking.bookingId(), ctx.guestId());

            var refund = new JdbcLedgerRepository(connection).entriesForBooking(booking.bookingId()).stream()
                    .filter(entry -> entry.type() == com.snoozeshare.domain.enums.WalletTransactionType.ESCROW_REFUND)
                    .findFirst().orElseThrow();
            assertEquals(1, events.size());
            assertEquals(refund.transactionId(), events.get(0).transactionId());
            assertEquals(refund.walletId(), events.get(0).walletId());
        }
    }

    @Test
    void aFailedTransactionPublishesNoWalletEvent() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("100.00"));
            var bus = new InProcessEventBus();
            List<WalletTransactionRecordedEvent> events = new ArrayList<>();
            bus.subscribe(WalletTransactionRecordedEvent.class, events::add);
            BookingService service = BookingServiceTest.createService(connection, bus);

            assertThrows(IllegalArgumentException.class, () -> service.submitRequest(ctx.guestId(), ctx.propertyId(),
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(13)));

            assertEquals(0, events.size());
        }
    }
}
```

- [ ] **Step 2: Make the tests compile, then see the B3 test fail**

Do Step 3 (compile fixes) first only as far as needed for `Tx` to compile: `LedgerWriter` needs its 3-arg constructor. Add it now (see Step 3, `LedgerWriter`) with the enqueue call **commented out**, update `AppContext`/`LedgerTestSupport` minimal wiring, and run:

Run: `./gradlew test --tests "com.snoozeshare.service.WalletEventPublishingTest"`
Expected: `cancellingABookingPublishesTheRefundEventOnce` FAILS with `expected: <1> but was: <0>` — this is the MP2 bug reproduced. (If compiling the rest of main/test makes this impractical, complete Step 3 with the enqueue line left out, run the test to see the failure, then add the line.)

- [ ] **Step 3: Implement**

`LedgerWriter`: add import `com.snoozeshare.infra.events.PostCommitEvents` and `com.snoozeshare.infra.events.events.WalletTransactionRecordedEvent`; field `private final PostCommitEvents events;`; replace the constructor with `LedgerWriter(WalletRepository wallets, AuditService audit, PostCommitEvents events)`; update the class Javadoc last sentence to "Wallet events are queued here and published by the TransactionManager after the caller's commit."; and in `post`, after the `audit.record(...)` call and before the `return`, add:

```java
        events.enqueue(new WalletTransactionRecordedEvent(transactionId, walletId, at));
```

`WalletServiceImpl`: replace constructor and `move`:

```java
    private final TransactionManager tx;
    private final WalletRepository wallets;
    private final LedgerWriter ledger;
    private final LedgerRepository ledgerEntries;

    public WalletServiceImpl(TransactionManager tx, WalletRepository wallets, LedgerWriter ledger,
                             LedgerRepository ledgerEntries) {
        this.tx = tx;
        this.wallets = wallets;
        this.ledger = ledger;
        this.ledgerEntries = ledgerEntries;
    }
```
```java
    private WalletTransaction move(UUID userId, WalletTransactionType type, BigDecimal amount) {
        Wallet wallet = getWallet(userId);
        return tx.inTransactionUnchecked(() ->
                ledger.post(wallet.walletId(), type, amount, userId, null, null, null, Instant.now()));
    }
```
Remove the `Connection`, `SQLException`, `EventBus`, `WalletTransactionRecordedEvent` imports. (Message for SQL failures changes from "Unable to write wallet ledger" to the generic one; if a test asserts that message, update it.)

`TransactionServiceImpl`: replace `Connection connection` with `TransactionManager tx`, drop the `EventBus` field/parameter, delete `publish(...)` and its two call sites, rewrite `inOwnTransaction` as `return tx.inTransactionUnchecked(work);`, and in `settleBookingCompletion` use `tx.inTransaction(current -> {...})` returning the payout directly (remove the `publish(payout);` line; keep the `catch (SQLException)`). New constructor: `TransactionServiceImpl(TransactionManager tx, BookingRepository bookings, PropertyRepository properties, WalletRepository wallets, LedgerWriter ledger, LedgerRepository ledgerEntries)`.

`BookingServiceImpl`: replace `Connection connection` with `TransactionManager tx` as the first constructor parameter; remove the `LedgerRepository ledgerEntries` parameter/field and the `EventBus`-wallet code: delete `publishWalletTransactionEvent` and its call in `submitRequest`, replace the three `new TransactionManager(connection).inTransaction(` with `tx.inTransaction(`, remove the `WalletTransactionRecordedEvent`, `LedgerRepository`, `Connection` imports. Final constructor: `(TransactionManager tx, BookingRepository bookings, PropertyRepository properties, AvailabilityBlockRepository blocks, WalletRepository wallets, LedgerWriter ledger, UserRepository users, ReviewRepository reviews, EventBus eventBus, TransactionService transactionService, TicketRepository tickets, Clock clock)`. `BookingCancelledEvent`/`BookingConfirmedEvent` publishing is untouched.

`DisputeSettlementServiceImpl`: first parameter becomes `TransactionManager tx` (field `tx`); `settle` uses `tx.inTransaction(current -> apply(...))`; delete `publish(settlement.guestTransaction()); publish(settlement.hostTransaction());` and the private `publish` method and unused import `WalletTransactionRecordedEvent`; keep `eventBus.publish(new TicketResolvedEvent(...))`.

`AccountGovernanceServiceImpl`: first parameter becomes `TransactionManager tx`; in `inTransaction(String what, Callable<T> work)` replace `new TransactionManager(connection).inTransaction(` with `tx.inTransaction(`; delete the `events.add(new WalletTransactionRecordedEvent(...))` line (keep the `BookingCancelledEvent` add); remove the unused `WalletTransactionRecordedEvent` and `Connection` imports.

`AppContext`: after `this.eventBus = new InProcessEventBus();` add:

```java
        PostCommitEvents postCommitEvents = PostCommitEvents.strict(eventBus::publish);
        TransactionManager tx = new TransactionManager(connection, postCommitEvents);
```
(imports `com.snoozeshare.infra.db.TransactionManager`, `com.snoozeshare.infra.events.PostCommitEvents`), build `LedgerWriter ledgerWriter = new LedgerWriter(wallets, auditService, postCommitEvents);`, and update the constructor calls: `new WalletServiceImpl(tx, wallets, ledgerWriter, ledgerRepository)`; `new TransactionServiceImpl(tx, bookingRepo, propertyRepo, wallets, ledgerWriter, ledgerRepository)`; `new BookingServiceImpl(tx, bookingRepo, propertyRepo, blockRepo, wallets, ledgerWriter, users, reviewRepo, eventBus, transactionService, ticketRepo, clock)`; `new DisputeSettlementServiceImpl(tx, ticketRepo, ...rest unchanged)`; `new AccountGovernanceServiceImpl(tx, users, ...rest unchanged)`. `UserServiceImpl` is deliberately unchanged (plan refinement 3).

- [ ] **Step 4: Update every test that constructs the changed services**

Find them: `grep -rn "new WalletServiceImpl(\|new TransactionServiceImpl(\|new BookingServiceImpl(\|new DisputeSettlementServiceImpl(\|new AccountGovernanceServiceImpl(\|new LedgerWriter(" src/test`. For each, replace the `connection` argument with the shared manager and the writer with the one from the same `Tx`, and make the bus the service receives the same bus the test subscribes to:

```java
        Tx tx = Tx.of(connection, bus);               // bus: the test's InProcessEventBus (or omit for a fresh one)
        new WalletServiceImpl(tx.manager(), wallets, tx.ledger(), ledgerRepository);
        new TransactionServiceImpl(tx.manager(), bookings, properties, wallets, tx.ledger(), ledgerRepository);
        new DisputeSettlementServiceImpl(tx.manager(), ...unchanged..., tx.ledger() ...);
        new AccountGovernanceServiceImpl(tx.manager(), ...unchanged..., tx.ledger() ...);
```
In `BookingServiceTest.createService(Connection, InProcessEventBus)` use:

```java
        InProcessEventBus bus = eventBus == null ? new InProcessEventBus() : eventBus;
        Tx tx = Tx.of(connection, bus);
        var ledgerEntries = new JdbcLedgerRepository(connection);
        return new BookingServiceImpl(tx.manager(),
                new JdbcBookingRepository(connection), new JdbcPropertyRepository(connection),
                new JdbcAvailabilityBlockRepository(connection), new JdbcWalletRepository(connection),
                tx.ledger(), new JdbcUserRepository(connection), new JdbcReviewRepository(connection),
                eventBus,
                new TransactionServiceImpl(tx.manager(), new JdbcBookingRepository(connection),
                        new JdbcPropertyRepository(connection), new JdbcWalletRepository(connection),
                        tx.ledger(), ledgerEntries),
                new JdbcTicketRepository(connection), Clock.systemDefaultZone());
```
(keep passing the original possibly-null `eventBus` as the booking service's `EventBus`; the `Tx` bus is only for wallet events). `AccountFixture` already owns a `bus` and a `ledger`: build a `Tx` from `connection` and `bus`, expose `tx.ledger()` as `ledger` and pass `tx.manager()` to services it builds. Tests that only call `LedgerTestSupport.writer(connection)` to seed money rows outside a transaction need no change (autocommit mode).

Tests that asserted wallet events through the service keep working because the `Tx` bus is the one they subscribe to. If a test asserted the old dispute-settlement event order (`TicketResolvedEvent` before the wallet events), change it to assert the new order (wallet events first) and note it in the commit message.

- [ ] **Step 5: Add the "every ledger write fires one event" test**

Append to `WalletEventPublishingTest`:

```java
    @Test
    void everyServiceLedgerWritePublishesExactlyOneEventPerRow() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("1000.00"));
            var bus = new InProcessEventBus();
            List<WalletTransactionRecordedEvent> events = new ArrayList<>();
            bus.subscribe(WalletTransactionRecordedEvent.class, events::add);
            var tx = com.snoozeshare.testsupport.Tx.of(connection, bus);
            var wallets = new com.snoozeshare.repository.jdbc.JdbcWalletRepository(connection);
            var ledgerRepo = new JdbcLedgerRepository(connection);
            WalletService walletService = new com.snoozeshare.service.impl.WalletServiceImpl(tx.manager(), wallets,
                    tx.ledger(), ledgerRepo);

            walletService.topUp(ctx.guestId(), new BigDecimal("25.00"));
            walletService.withdraw(ctx.guestId(), new BigDecimal("5.00"));

            var rows = ledgerRepo.entriesForWallet(wallets.findByUserId(ctx.guestId()).orElseThrow().walletId());
            assertEquals(rows.size(), events.size());
            assertEquals(2, events.size());
        }
    }
```
(Booking submit/reject/cancel/settle and dispute/suspension paths are covered by the existing per-service event assertions plus the cancel test above; this test covers the wallet service. `postPayout` writes two rows and therefore two events — `completeSettlesNetHostPayoutAndIsIdempotent` already asserts exactly one event for the payout row.)

- [ ] **Step 6: Run the whole suite**

Run: `./gradlew build`
Expected: PASS (all tests + Checkstyle). Likely fix-ups: tests still passing `connection` to a changed constructor; a test that nests transactions; Checkstyle unused-import warnings. Fix each at its cause.

- [ ] **Step 7: Commit**

```bash
git add -A src
git commit -m "fix(wallet): queue wallet events in LedgerWriter and publish after commit (B3, D1)"
```

---

### Task 6: Audit rows atomic with listing, review and ticket changes (D2)

**Files:**
- Modify: `ListingServiceImpl`, `ReviewServiceImpl`, `TicketServiceImpl`, `AppContext`
- Test: `src/test/java/com/snoozeshare/service/AuditAtomicityTest.java`; update tests that construct these three services

- [ ] **Step 1: Write the failing tests**

`AuditAtomicityTest.java` (reuses `BookingServiceTest.seedContext`, which provides a guest with a wallet, a host and a property):

```java
package com.snoozeshare.service;

import static com.snoozeshare.service.BookingServiceTest.migratedConnection;
import static com.snoozeshare.service.BookingServiceTest.seedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.TicketCategory;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.infra.db.TransactionManager;
import com.snoozeshare.repository.jdbc.JdbcAvailabilityBlockRepository;
import com.snoozeshare.repository.jdbc.JdbcBookingRepository;
import com.snoozeshare.repository.jdbc.JdbcPropertyRepository;
import com.snoozeshare.repository.jdbc.JdbcReviewRepository;
import com.snoozeshare.repository.jdbc.JdbcTicketCategoryRepository;
import com.snoozeshare.repository.jdbc.JdbcTicketRepository;
import com.snoozeshare.repository.jdbc.JdbcUserRepository;
import com.snoozeshare.repository.jdbc.JdbcWalletRepository;
import com.snoozeshare.service.impl.AvailabilityServiceImpl;
import com.snoozeshare.service.impl.ListingServiceImpl;
import com.snoozeshare.service.impl.ReviewServiceImpl;
import com.snoozeshare.service.impl.TicketServiceImpl;
import com.snoozeshare.service.impl.UserServiceImpl;
import com.snoozeshare.service.requests.NewTicketRequest;
import com.snoozeshare.testsupport.Fakes;
import com.snoozeshare.testsupport.FailingAuditService;
import com.snoozeshare.testsupport.LedgerTestSupport;

/** A failed audit write must roll back the change it was auditing (MP2 feedback D2). */
class AuditAtomicityTest {

    private static Property draftFrom(Property existing, UUID hostId) {
        return new Property(null, hostId, ListingStatus.ACTIVE, "Audit probe", existing.description(),
                existing.propertyType(), existing.streetAddress(), existing.city(), existing.region(),
                existing.postalCode(), existing.maxGuests(), existing.bedrooms(), existing.bathrooms(),
                existing.baseNightlyRate(), existing.checkInTime(), existing.checkOutTime(), existing.amenities(),
                null);
    }

    private static ListingServiceImpl listingService(Connection connection, AuditService audit) {
        var properties = new JdbcPropertyRepository(connection);
        var availability = new AvailabilityServiceImpl(properties, new JdbcAvailabilityBlockRepository(connection),
                new JdbcBookingRepository(connection));
        var users = new UserServiceImpl(connection, new JdbcUserRepository(connection),
                new JdbcWalletRepository(connection));
        return new ListingServiceImpl(new TransactionManager(connection), properties, availability, users, audit);
    }

    @Test
    void failedAuditRollsBackListingCreate() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            var properties = new JdbcPropertyRepository(connection);
            int before = properties.findByHostId(ctx.hostId()).size();
            Property draft = draftFrom(properties.findById(ctx.propertyId()).orElseThrow(), ctx.hostId());
            var service = listingService(connection, new FailingAuditService(LedgerTestSupport.audit(connection), 1));

            assertThrows(RuntimeException.class, () -> service.create(draft, ctx.hostId()));

            assertEquals(before, properties.findByHostId(ctx.hostId()).size());
        }
    }

    @Test
    void failedAuditRollsBackListingStatusChange() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            var properties = new JdbcPropertyRepository(connection);
            ListingStatus original = properties.findById(ctx.propertyId()).orElseThrow().status();
            ListingStatus other = original == ListingStatus.ACTIVE ? ListingStatus.INACTIVE : ListingStatus.ACTIVE;
            var service = listingService(connection, new FailingAuditService(LedgerTestSupport.audit(connection), 1));

            assertThrows(RuntimeException.class, () -> service.updateStatus(ctx.propertyId(), other, ctx.hostId()));

            assertEquals(original, properties.findById(ctx.propertyId()).orElseThrow().status());
        }
    }

    @Test
    void failedAuditRollsBackReviewSubmit() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            var bookings = new JdbcBookingRepository(connection);
            UUID bookingId = UUID.randomUUID();
            bookings.save(new Booking(bookingId, ctx.propertyId(), ctx.guestId(), LocalDate.now().minusDays(20),
                    LocalDate.now().minusDays(15), BookingStatus.COMPLETED, new BigDecimal("100.00"),
                    new BigDecimal("500.00"), Instant.now(), Instant.now(), Instant.now()));
            var reviews = new JdbcReviewRepository(connection);
            var service = new ReviewServiceImpl(new TransactionManager(connection), bookings, reviews,
                    new FailingAuditService(LedgerTestSupport.audit(connection), 1), Clock.systemUTC());

            assertThrows(RuntimeException.class, () -> service.submit(bookingId, ctx.guestId(), 5, "great"));

            assertTrue(reviews.findByBookingId(bookingId).isEmpty());
        }
    }

    @Test
    void failedAuditRollsBackTicketFilingAndCategoryCreation() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            Clock clock = Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC);
            var bookings = new JdbcBookingRepository(connection);
            UUID bookingId = UUID.randomUUID();
            bookings.save(new Booking(bookingId, ctx.propertyId(), ctx.guestId(), LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 5), BookingStatus.CONFIRMED, new BigDecimal("100.00"),
                    new BigDecimal("400.00"), Instant.parse("2026-08-20T00:00:00Z"),
                    Instant.parse("2026-08-21T00:00:00Z"), null));
            var categories = new JdbcTicketCategoryRepository(connection);
            categories.save(new TicketCategory(UUID.randomUUID(), "Cleanliness", true));
            var users = new JdbcUserRepository(connection);
            User agent = new User(UUID.randomUUID(), Role.AGENT, "Agent", "agent@example.com", AccountStatus.ACTIVE,
                    null, Instant.parse("2026-01-01T00:00:00Z"));
            users.save(agent);
            var tickets = new JdbcTicketRepository(connection);
            var service = new TicketServiceImpl(new TransactionManager(connection), tickets, categories, bookings,
                    users, new Fakes.RecordingSettlement(),
                    new FailingAuditService(LedgerTestSupport.audit(connection), 1), clock);

            assertThrows(RuntimeException.class, () -> service.fileTicket(new NewTicketRequest(bookingId,
                    "Cleanliness", "Dirty", "Not cleaned", RemedyType.FULL_REFUND, null), ctx.guestId(), Role.GUEST));
            assertTrue(tickets.findByRaisedByUserId(ctx.guestId()).isEmpty());

            var failingAgentService = new TicketServiceImpl(new TransactionManager(connection), tickets, categories,
                    bookings, users, new Fakes.RecordingSettlement(),
                    new FailingAuditService(LedgerTestSupport.audit(connection), 1), clock);
            int before = categories.findAll().size();
            assertThrows(RuntimeException.class, () -> failingAgentService.createCategory("Noise", agent.userId()));
            assertEquals(before, categories.findAll().size());
        }
    }
}
```

- [ ] **Step 2: Make it compile and see it fail**

Add the new `TransactionManager tx` first constructor parameter to the three services (Step 3) without yet wrapping the bodies, update `AppContext`, then run:

Run: `./gradlew test --tests "com.snoozeshare.service.AuditAtomicityTest"`
Expected: all four tests FAIL (each leaves the entity change behind — e.g. `expected: <1> but was: <2>`, `expected: <ACTIVE> but was: <INACTIVE>`).

- [ ] **Step 3: Implement**

For each service add `private final TransactionManager tx;` (import `com.snoozeshare.infra.db.TransactionManager`) and a first constructor parameter `TransactionManager tx`. `TicketServiceImpl` has two constructors; add `tx` first to both (the 7-arg one forwards it). Then wrap each mutation's `save` + `audit.record` pair in `tx.inTransactionUnchecked(() -> { ...; return result; })`, keeping validation before the transaction and event publishing after it:

`ListingServiceImpl.create`: replace the `properties.save`/`audit.record`/return tail:

```java
        return tx.inTransactionUnchecked(() -> {
            Property persisted = properties.save(saved);
            audit.record(AuditRecord.builder(host.userId(), AuditAction.LISTING_CREATED, "Property",
                            persisted.propertyId())
                    .status(null, persisted.status()).subject(host.userId())
                    .reason("Listing created: " + persisted.title()).build());
            return persisted;
        });
```
`update` and `updateStatus`: same shape around their `properties.save(...)` + `audit.record(...)` (return `persisted`).

`ReviewServiceImpl.submit`: after building `review`:

```java
        return tx.inTransactionUnchecked(() -> {
            Review saved = reviews.save(review);
            audit.record(AuditRecord.builder(guestId, AuditAction.REVIEW_SUBMITTED, "Review", saved.reviewId())
                    .subject(guestId).booking(bookingId).at(saved.createdAt()).build());
            return saved;
        });
```

`TicketServiceImpl`: wrap the `save` + `audit.record` of `fileTicket`, `createCategory`, `renameCategory`, `setCategoryActive`, `deleteCategory`, `assignToMe`, `saveNotes`, `unassign` the same way; in `fileTicket` publish `TicketOpenedEvent` after the wrapped block returns `saved`:

```java
        Ticket saved = tx.inTransactionUnchecked(() -> {
            Ticket persisted = tickets.save(ticket);
            audit.record(AuditRecord.builder(raisedByUserId, AuditAction.TICKET_OPENED, "Ticket",
                            persisted.ticketId())
                    .subject(raisedByUserId).booking(persisted.bookingId()).ticket(persisted.ticketId())
                    .at(now).build());
            return persisted;
        });
        eventBus.publish(new TicketOpenedEvent(saved.ticketId(), raisedByUserId, now));
        return saved;
```
For `deleteCategory` (void) return `null` from the lambda or use `tx.inTransactionUnchecked(() -> { ...; return null; });`.

`AppContext`: pass `tx` as the first argument of `new ListingServiceImpl(tx, ...)`, `new ReviewServiceImpl(tx, ...)`, `new TicketServiceImpl(tx, ...)`.

The audit repository writes use the same `Connection` as the entity repositories (`JdbcAuditLogRepository(connection)`), so the audit row joins the open transaction automatically — verified by the failing-audit tests in Step 4.

- [ ] **Step 4: Update the other test constructors and run**

Find them: `grep -rn "new ListingServiceImpl(\|new ReviewServiceImpl(\|new TicketServiceImpl(" src/test`. Services built over `Fakes` (no JDBC connection — `FileTicketTest`, `TicketService*Test`, `ListingServiceTest`, `ReviewServiceTest` if they use fakes) have no connection to give the manager. For those, add to `testsupport/Fakes.java`:

```java
    /** A transaction manager over an in-memory connection, for tests whose repositories are in-memory fakes. */
    public static com.snoozeshare.infra.db.TransactionManager transactions() {
        try {
            return new com.snoozeshare.infra.db.TransactionManager(
                    com.snoozeshare.infra.db.ConnectionFactory.open("jdbc:sqlite::memory:"));
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }
```
and pass `Fakes.transactions()` as the new first argument. (A fakes-backed test gets no real atomicity, which is fine; the JDBC-backed `AuditAtomicityTest` proves it. If `ConnectionFactory.open` throws a different checked exception type, adjust the `catch` to that type.)

Run: `./gradlew build`
Expected: PASS (all tests + Checkstyle), including all four `AuditAtomicityTest` tests.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "fix(audit): make listing, review and ticket changes atomic with their audit rows (D2)"
```

---

### Task 7: Single fee definition (D5)

**Files:**
- Modify: `src/main/java/com/snoozeshare/domain/settlement/SettlementCalculator.java`
- Modify: `BookingServiceImpl` (two `0.97` literals), `TransactionServiceImpl` (one), `LedgerWriter` (the "3%" text)
- Test: `src/test/java/com/snoozeshare/domain/settlement/SettlementCalculatorTest.java` (existing file; append)

Background: `TransactionServiceImpl` computes `net = gross × 0.97` then `fee = gross − net`, while `SettlementCalculator` computes `fee = gross × 0.03` then `net = gross − fee`. They differ by one cent when `gross × 0.03` ends in exactly half a cent (e.g. a gross of 0.50: fee 0.02 vs 0.01). Dispute settlement already uses the calculator, so the calculator's rule (fee rounded HALF_UP, net = gross − fee) is the single rule.

- [ ] **Step 1: Write the failing test**

Append to `SettlementCalculatorTest`:

```java
    @Test
    void hostNetAndPlatformFeeAlwaysSumToTheGross() {
        for (String gross : new String[] {"0.50", "0.01", "99.99", "291.00", "300.00", "1234.57"}) {
            BigDecimal amount = new BigDecimal(gross);
            assertEquals(0, amount.compareTo(
                    SettlementCalculator.hostNet(amount).add(SettlementCalculator.platformFee(amount))), gross);
        }
    }

    @Test
    void platformFeeMatchesTheDisputeSettlementSplit() {
        BigDecimal gross = new BigDecimal("300.00");
        assertEquals(0, SettlementCalculator.split(gross, BigDecimal.ZERO).fee()
                .compareTo(SettlementCalculator.platformFee(gross)));
        assertEquals(0, new BigDecimal("291.00").compareTo(SettlementCalculator.hostNet(gross)));
    }
```
(Add `import java.math.BigDecimal;` and the static `assertEquals` import if the file lacks them.)

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew test --tests "com.snoozeshare.domain.settlement.SettlementCalculatorTest"`
Expected: compilation FAIL (`hostNet`/`platformFee` do not exist).

- [ ] **Step 3: Implement**

In `SettlementCalculator` add (and make `split` use `platformFee`):

```java
    /** The platform's 3% share of a host gross, rounded half-up to the cent. */
    public static BigDecimal platformFee(BigDecimal hostGross) {
        return hostGross.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /** What the host receives: the gross minus the platform fee. */
    public static BigDecimal hostNet(BigDecimal hostGross) {
        return hostGross.setScale(2, RoundingMode.HALF_UP).subtract(platformFee(hostGross));
    }
```
and in `split` replace `BigDecimal fee = hostGross.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);` with `BigDecimal fee = platformFee(hostGross);`.

`BookingServiceImpl`: replace both `booking.totalAmount().multiply(new BigDecimal("0.97")).setScale(2, java.math.RoundingMode.HALF_UP)` expressions (in `toHostRows` and `previewHostEarnings`) with `SettlementCalculator.hostNet(booking.totalAmount())` (import `com.snoozeshare.domain.settlement.SettlementCalculator`).

`TransactionServiceImpl.settleBookingCompletion`: replace the `net`/`fee` lines with

```java
                BigDecimal net = SettlementCalculator.hostNet(gross);
                BigDecimal fee = SettlementCalculator.platformFee(gross);
```
and remove the `RoundingMode` import if it becomes unused.

`LedgerWriter`: the "3%" strings in reasons are descriptive text for audit rows; leave them but add a one-line comment above `postPayout`: `// The 3% rate itself lives in SettlementCalculator; the text below only describes it.`

- [ ] **Step 4: Run the tests**

Run: `./gradlew build`
Expected: PASS. If a test pinned the old one-cent behaviour for a half-cent gross, update its expected values to the calculator's rule and mention it in the commit message.

- [ ] **Step 5: Commit**

```bash
git add -A src
git commit -m "refactor(settlement): define the platform fee once in SettlementCalculator (D5)"
```

---

### Task 8: Idempotent sweep, full verification, records

**Files:**
- Modify: `src/test/java/com/snoozeshare/service/DisputeWindowBoundaryTest.java`
- Modify: `PROJECT_STATE.md`, `docs/project-state/done-ledger.md`, `docs/MP2-feedback-triage.md`

- [ ] **Step 1: Add the idempotency test**

Append to `DisputeWindowBoundaryTest`:

```java
    @Test
    void runningTheSweepTwiceSettlesOnce() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("1000.00"));
            BookingService service = createService(connection);
            Booking booking = confirmedEndedDaysAgo(connection, new BookingTestIds(ctx.guestId(), ctx.propertyId()),
                    service, 9);

            assertEquals(1, service.completeEligibleBookings());
            assertEquals(0, service.completeEligibleBookings());

            long payouts = new com.snoozeshare.repository.jdbc.JdbcLedgerRepository(connection)
                    .entriesForBooking(booking.bookingId()).stream()
                    .filter(entry -> entry.type() == com.snoozeshare.domain.enums.WalletTransactionType.BOOKING_PAYOUT)
                    .count();
            assertEquals(1, payouts);
        }
    }
```

Run: `./gradlew test --tests "com.snoozeshare.service.DisputeWindowBoundaryTest"` — Expected: PASS (settlement already returns early for `COMPLETED`; this pins it).

- [ ] **Step 2: Full verification**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL — all tests (526 + new) and Checkstyle for production and test code pass. Record the new test count.

Then run the app against a copy of the mock DB and check the two user-visible fixes by hand (record the result; if you cannot run the GUI, say so in the Done ledger entry instead of claiming it):
- `SNOOZESHARE_DB_URL=jdbc:sqlite:build/acceptance.db ./gradlew run`: as a guest, cancel a confirmed booking — the top-right balance must update immediately.
- As a guest, a trip that ended exactly 7 days ago still offers "File dispute"; an agent can resolve it.

- [ ] **Step 3: Update the records**

`PROJECT_STATE.md`:
- § Workstreams row `MP3-0`: status `Building` → set to `In review` when Step 2 passes; Progress `Tasks 1-8 done; awaiting operator review`; add the plan link in the Plan cell (`docs/superpowers/plans/2026-10-18-mp3-correctness-cluster.md`).
- § Architecture: update § 4.3 (Service) and § 4.5/4.6 (events) "Now" text — `PostCommitEvents`/`TransactionListener`, `LedgerWriter` queues wallet events, shared `TransactionManager`, `domain.policy`; update § 4.4 Domain with the two policies. Update the stale statement "Callers publish events after their commit" wherever it appears.
- § Conventions: add "Wallet events are queued by `LedgerWriter`; never publish `WalletTransactionRecordedEvent` by hand", "Dispute window and stay-date rules live only in `domain.policy`", "Integration commits stay small and run `./gradlew build` at each merge (P3 convention from MP2 feedback)".
- § Deviations: add the six plan-level refinements listed at the top of this plan and the changed settlement event order.
- § How to Resume: update your session row.
- § Known Gaps: note under the single-process entry that `PostCommitEvents` is a single shared queue by that assumption.

`docs/project-state/done-ledger.md`: add newest-first lines for B1, B3/D1, B4/B5, D2, D3/D5 with the commit SHAs.

`docs/MP2-feedback-triage.md`: mark B1, B3, B4, B5, D1, D2, D3, D5, P5 as fixed (add a "Status" note at the top listing them with the commit SHAs); leave the rest open.

- [ ] **Step 4: Commit**

```bash
git add -A PROJECT_STATE.md docs src
git commit -m "docs: record MP3-0 correctness cluster completion; add sweep idempotency test"
```

---

## Self-review (against the spec)

- **§ 4 Policies / B1 / B4 / B5 / D5:** Task 1 (policies + invariant test), Task 2 (B1 wiring, `TicketServiceImpl`, `BookingServiceImpl`, `TripDashboardController`, sweep cutoff), Task 3 (B4/B5 service validation), Task 7 (fee). `completionCutoff()` is used by the sweep as the spec requires.
- **§ 5 Post-commit events:** Task 4 (`TransactionListener`, `PostCommitEvents`, guards 1–3 each tested: strict enqueue, nesting, failing sink), Task 5 (LedgerWriter enqueue, shared manager, manual publishes deleted, B3 test, per-row event test). Refinements 2, 3, 5 explain the departures (autocommit mode, `UserServiceImpl` kept, `EventBus` params dropped).
- **§ 6 D2:** Task 6 covers listing create/update/status, review submit, and every ticket mutation named in the spec; tests prove rollback for listing create, listing status, review and ticket filing/category creation (`update`, `assignToMe`, `saveNotes`, `unassign`, rename/toggle/delete share the same wrapper and are exercised by existing service tests).
- **§ 7 Tests 1–9:** 1–2 Tasks 1–2; 3 and 5 Task 5; 4 Task 4; 6 Task 6; 7 Task 3; 8 Task 7 (uses the calculator's rule — see Background); 9 Tasks 2, 5, 8 (`./gradlew build`).
- **§ 8 Cloud seams:** `EventSink` (Task 4), injected `Clock` in policies (Task 1), idempotent sweep test (Task 8), single-process comment on `PostCommitEvents` (Task 4).
- **Types used across tasks:** `DisputeWindowPolicy(Clock)`, `canFileTicket/canAutoComplete/completionCutoff`; `StayDatePolicy(Clock)`, `requireValidStay/requireValidSearchRange`; `TransactionListener.onBegin/afterCommit/afterRollback`; `TransactionManager(Connection[, TransactionListener])`, `inTransaction`, `inTransactionUnchecked`; `PostCommitEvents.strict/autocommit/enqueue`; `LedgerWriter(WalletRepository, AuditService, PostCommitEvents)`; `Tx.of(connection[, bus])` with `manager()/ledger()/bus()`; `SettlementCalculator.platformFee/hostNet` — names match in every task.
- **Known soft spots the executor must resolve by reading the file, not guess:** the exact `SearchCriteria` construction in `ListingServiceTest` (Task 3), the message-display mechanism in `GuestSearchController` (Task 3), which tests nest transactions (Task 4/5), and the Fakes-backed constructor call sites (Task 6) — each step says what to look for.
