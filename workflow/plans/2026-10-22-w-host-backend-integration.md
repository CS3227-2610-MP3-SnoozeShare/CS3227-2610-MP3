# W-HOST Backend Integration Implementation Plan

- **Status:** Approved (Trevor, 2026-10-22)
- **Amended 2026-10-22:** Added prerequisite T0 with operator approval C~17; all service-dependent API tasks now depend on T0.
- **Amended and approved 2026-10-22:** Added T11-T17 to reverse collection/history pagination per approved spec amendment C~19.
- **Amended and approved 2026-10-22:** Reconciled completed T3 overlap response contract with source-specific 409 codes per spec amendment C~28; implementation follow-up completed in `e25744a`.
- **Execution note:** T0-T10 record the original completed paging contract; their checked historical evidence is superseded for collection reads by T11-T17. Final acceptance uses the current F8/E26-E28 traceability rows.

> **For agents:** tick `- [ ]` to `- [x]` as each step is completed, in the same commit as the work. Keep the W-HOST branch state's Progress cell current. Spec: [W-HOST Backend Integration Design](../specs/2026-10-22-w-host-backend-integration-design.md).

## Scope

**Goal:** Replace Host production fixtures with an authenticated `/api/host/**` API and API-backed pages, preserving the approved UI.

**In scope:** F1-F8; N1-N6. This includes deterministic activation of existing persistence-backed services, active Host identity/ownership, complete owner-scoped collection reads, listing/calendar/booking/message/wallet endpoints, and authenticated frontend state/mutations.

**Non-goals:** Payment-provider integration (C75); AI features (Q6); polling/WebSockets (C78); visual redesign; wallet request idempotency (C~16 / SD12).

**Risk tier:** High - authorization, tenancy, booking state, messaging, and wallet ledger behavior require security, concurrency, transaction, and failure-path tests.

## Task order

| Task | Depends on | Can run concurrently with | Wave | Status |
|---|---|---|---|---|
| T0 Activate W-DOMAIN service beans deterministically | - | T1 | 1 | Done |
| T1 Add Host identity resolver and stable error boundary | - | T0 | 1 | Done |
| T2 Add listing API and detail projection | T0,T1 | T3 | 2 | Done |
| T3 Make calendar mutations atomic and expose calendar API | T0,T1 | T2 | 2 | Done |
| T4 Serialize Host booking decisions | T0,T1 | T5,T6 | 2 | Done |
| T5 Add distinct Host conversation APIs | T0,T1 | T4,T6 | 2 | Done |
| T6 Add Host wallet API | T0,T1 | T4,T5 | 2 | Done |
| T7 Add authenticated frontend Host API adapter | T0,T1,T2,T3,T4,T5,T6 | - | 3 | Done |
| T8 Wire listing, calendar, and request screens | T2,T3,T4,T7 | - | 4 | Done |
| T9 Wire messages and wallet screens | T5,T6,T7,T8 | - | 5 | Done |
| T10 Verify integration and close the phase | T9 | - | 6 | Done |
| T11 Return complete listing and booking-request collections | T2,T4 | T13 | 7 | Done |
| T12 Return complete booking and ticket conversation collections | T5,T11 | T13 | 8 | Done |
| T13 Return the complete wallet statement | T6 | T11,T12 | 7 | Done |
| T14 Replace paged frontend API and provider types | T11,T12,T13 | - | 9 | Done |
| T15 Remove listing and request pagination UI | T14 | T16 | 10 | Done |
| T16 Remove message-history and wallet load-more UI | T14 | T15 | 10 | Done |
| T17 Verify the pagination reversal | T15,T16 | - | 11 | Done |

## Traceability

| Requirement | Task(s) | Verified by |
|---|---|---|
| F1 | T1,T2 | `HostIdentityResolverTest`, `HostListingsControllerTest` |
| F2 | T3,T8 | `HostCalendarIntegrationTest`, calendar page tests |
| F3 | T4,T8 | `HostRequestsIntegrationTest`, request page tests |
| F4 | T5,T9 | booking/ticket message integration tests |
| F5 | T6,T9 | `HostWalletIntegrationTest`, wallet page tests |
| F6 | T7-T10 | Host route/provider tests and manual route verification |
| F7 | T1,T7 | `HostSecurityIntegrationTest`, `hostApi.test.ts` |
| N1 | T1-T6 | security and cross-host integration tests |
| N2 | T3,T4,T6 | availability/booking concurrency and wallet transaction tests |
| N3 | T1-T9 | controller contract tests and frontend API tests |
| N4 | T1,T5,T6 | safe-error/log capture tests |
| N5 | T2-T10,T11-T17 | One request per dataset, large-result integration tests, and browser network verification |
| N6 | T0,T2 | `DomainServiceActivationTest`, `HostListingsControllerTest`, `ServerPortDefaultTest` |
| E1,E2,E4,E5,E15,E22 | T1,T2 | `HostSecurityIntegrationTest`, `HostListingsControllerTest` |
| E3 | T2-T6 | Host listing, calendar, booking, message, and wallet integration tests |
| E6,E18 | T3 | `HostCalendarIntegrationTest` |
| E7,E19 | T4 | `HostRequestsIntegrationTest` |
| E8,E16,E20 | T5 | booking/ticket message integration tests |
| E9,E10,E12 | T6,T9 | `HostWalletIntegrationTest`, wallet page tests |
| E11,E13 | T7-T10 | API client tests and auth recovery route tests |
| E21 | T8,T9 | Host text-rendering tests |
| E23 | T7-T9 | Host mutation failure/recovery tests |
| E24 | T0,T2 | `DomainServiceActivationTest`, `HostListingsControllerTest` |
| E25 | T0 | `ServerPortDefaultTest`, `ServerPortOverrideTest` |
| F8 | T11-T17 | Complete arrays in Host integration/API/provider/route tests; E26-E28 cases |
| E26 | T11-T17 | Each response type independently returns all 101 rows; 1,001-message history in the booking or ticket integration test |
| E27 | T11-T14 | Legacy `page`/`size` parameters do not alter results or add pagination fields |
| E28 | T12,T17 | 1,001 authorized messages returned in `sentAt DESC, id DESC` order |

## Verification gates

- Backend task tests: from `backend/`, run `./gradlew test --tests '<named test class>'`; run `./gradlew checkstyleMain checkstyleTest` after Java changes.
- Frontend task tests: from `frontend/`, run `npm test -- <test-file>`; also run `npm run lint` after TS/TSX changes.
- Before finishing: `cd backend && ./gradlew build`, then `cd frontend && npm test && npm run lint && npm run build` (with the configured `NEXT_PUBLIC_API_BASE_URL`); every F/N acceptance criterion has a passing test or recorded manual check.
- For the pagination-removal amendment, run the named backend integration tests and focused frontend tests after each task; before T17 closeout run the same complete backend and frontend gates. Browser/network acceptance is read-only and verifies each dataset is fetched once without `page`/`size`; no mutation or production data is involved.

---

### T0: Activate W-DOMAIN service beans deterministically

**Delivers:** Deterministic property-controlled activation of persistence-backed beans, preserving database-free contexts | **Satisfies:** N6,E24,E25 | **Depends on:** - | **Wave:** 1

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `backend/src/main/resources/application.yml` | Set `snoozeshare.persistence.enabled: true` as the application default. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/JdbcBookingMessageReadRepository.java` | Replace the repository-bean condition with the explicit property condition from spec `conventions.md`. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/JdbcLedgerRepository.java` | Replace the repository-bean condition with the explicit property condition from spec `conventions.md`. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/JdbcMessageReadRepository.java` | Replace the repository-bean condition with the explicit property condition from spec `conventions.md`. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/AccountGovernanceServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/AuditServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/AvailabilityServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/BookingConversationServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/BookingServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/DisputeQueryServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/DisputeSettlementServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/LedgerWriter.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/ListingMetricsServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/ListingServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/MessageServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/ReviewServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/TicketServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/TransactionServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/UserServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/WalletServiceImpl.java` | Replace repository-bean condition with the explicit property condition; preserve service behavior. |
| Test | `backend/src/test/java/com/snoozeshare/PersistenceBeanAssertions.java` | Share exact present/absent assertions for all 15 service contracts and three JDBC adapters. |
| Test | `backend/src/test/java/com/snoozeshare/DomainServiceActivationTest.java` | Without overriding the `application.yml` default, assert all 15 named service contracts and three named JDBC adapters register (E24). |
| Modify | `backend/src/test/java/com/snoozeshare/ServerPortDefaultTest.java` | Disable persistence beans in its JPA-excluded context and assert they are absent (E25). |
| Modify | `backend/src/test/java/com/snoozeshare/ServerPortOverrideTest.java` | Disable persistence beans in its JPA-excluded context and assert they are absent (E25). |

**Checklist**

- [x] 1. Add failing E24/E25 context assertions and run `./gradlew test --tests 'com.snoozeshare.DomainServiceActivationTest' --tests 'com.snoozeshare.ServerPortDefaultTest' --tests 'com.snoozeshare.ServerPortOverrideTest'`; verify database-backed service activation fails before the fix.
- [x] 2. Set `snoozeshare.persistence.enabled: true` in `application.yml`; replace `@ConditionalOnBean(PropertyRepository.class)` on all 15 service-contract implementations, `LedgerWriter`, and 3 JDBC adapters with `@ConditionalOnProperty(name = "snoozeshare.persistence.enabled", havingValue = "true", matchIfMissing = false)`.
- [x] 3. Set the property to `false` in the two database-free server tests; assert excluded service/adapters are absent there.
- [x] 4. Run the named context tests; verify default-on E24 service/adapters and explicit-off E25. T2 separately verifies Host controller startup with the activated `ListingService`.
- [x] 5. Run `./gradlew checkstyleMain checkstyleTest` and `./gradlew build` from `backend/`.
- [x] 6. Update branch-state Progress and Changes; commit `fix: activate W-DOMAIN services deterministically` (`9ba4ce2`).

### T1: Add Host identity resolver and stable error boundary

**Delivers:** Firebase-to-domain identity check, typed service/API failures, shared error envelope | **Satisfies:** F1-F7,N1,N3,N4 | **Depends on:** - | **Wave:** 1

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/auth/service/HostIdentityResolver.java` | Resolve `FirebaseAuthentication` UID through `AuthUserRepository`, then verify matching `User` UUID/UID/HOST/ACTIVE using `UserRepository`; spec `api.md` shared identity. |
| Create | `backend/src/main/java/com/snoozeshare/api/error/HostApiExceptionHandler.java` | Map validation, ownership, domain, stale-state, profile-drift, and unexpected failures to the exact `AuthErrorResponse` status/code matrix; never serialize exception internals. |
| Create | `backend/src/main/java/com/snoozeshare/api/error/HostApiException.java` | Typed API-safe exceptions for stable not-found, conflict, domain-rule, and profile-inconsistency outcomes. |
| Test | `backend/src/test/java/com/snoozeshare/auth/service/HostIdentityResolverTest.java` | Matching identity; absent or mismatched domain user returns `PROFILE_INCONSISTENT` (E22). |
| Test | `backend/src/test/java/com/snoozeshare/api/error/HostApiExceptionHandlerTest.java` | Exact status/code/envelope, request ID, safe message, and generic unexpected failure (E15). |
| Test | `backend/src/test/java/com/snoozeshare/HostSecurityIntegrationTest.java` | Missing/invalid token, wrong role, inactive profile, and no service call (E1,E2). |

**Checklist**

- [x] 1. Write failing identity/error tests for E1, E2, E15, and E22; run the named backend test classes and confirm they fail for missing resolver/error mapping.
- [x] 2. Implement typed error cases and resolve the Firebase UID against both auth and domain repositories; never accept a browser-supplied host UUID.
- [x] 3. Run the same tests; assert missing auth profile remains 404 `PROFILE_NOT_REGISTERED`, inconsistent domain profile is 409 `PROFILE_INCONSISTENT`, and role failures remain 403.
- [x] 4. Run `cd backend && ./gradlew checkstyleMain checkstyleTest` and `cd backend && ./gradlew build`.
- [x] 5. Update branch-state Progress and Changes; commit `feat: add Host identity and API error boundary`.

### T2: Add listing API and detail projection

**Delivers:** Owned listing collection/detail/create/update/status and metrics composition | **Satisfies:** F1,N1,N3 | **Depends on:** T0,T1 | **Wave:** 2

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `backend/src/main/java/com/snoozeshare/api/controller/HostListingController.java` | Continue the partial Host-scoped list/detail/create/update/status controller; it uses `ListingService` and `ListingMetricsService`, has no repository access, and is registered only when `snoozeshare.security.enabled=true` so its resolver is present. |
| Modify | `backend/src/main/java/com/snoozeshare/api/response/HostListingDetailResponse.java` | Continue the partial composite `PropertyResponse` + `ListingMetricsResponse` payload. |
| Modify | `backend/src/main/java/com/snoozeshare/api/response/PageResponse.java` | Continue the partial shared `{items,page,size,hasMore}` response. |
| Modify | `backend/src/main/java/com/snoozeshare/api/request/CreateListingRequest.java` and `backend/src/main/java/com/snoozeshare/api/request/UpdateListingRequest.java` | Enforce the approved title maximum of 120 characters and description maximum of 5,000 characters with Bean Validation. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/PropertyRepository.java` (`findByHostId`) | Add deterministic pageable Host query with `createdAt DESC, id ASC`. |
| Create | `backend/src/main/java/com/snoozeshare/repository/OffsetLimitPageable.java` | Provide `Pageable` with `getOffset() = (long) page * requestedSize` and `getPageSize() = requestedSize + 1`, preserving zero-based page boundaries while fetching the extra row. |
| Modify | `backend/src/main/java/com/snoozeshare/service/ListingService.java` and `backend/src/main/java/com/snoozeshare/service/impl/ListingServiceImpl.java` (`findByHostId`) | Expose bounded Host page read using `OffsetLimitPageable(page, size)`; preserve existing create/update/status authorization and audit behavior. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostListingsControllerTest.java` | JSON contracts, create/update/status, validation, owned/not-owned results (E3,E4,E5), page 0/page 1 continuity, and controller startup with activated `ListingService` (E24). |
| Test | `backend/src/test/java/com/snoozeshare/repository/PropertyRepositoryTest.java` | Host-scoped stable ordering, `size + 1` query bound, and zero-based page offsets without skipped or duplicated rows (E17). |

**Checklist**

- [x] 1. Write failing repository/controller tests for E3-E5, E17, and E24; verify another Host cannot read or mutate a listing, page 1 begins at row `size` with no gap/duplicate, and the bounded query returns at most `size + 1` rows.
- [x] 2. Add `OffsetLimitPageable(page, size)` with offset `page * size` and limit `size + 1`; use it in the Host listing query, then add the composite detail response and controller routes from `api.md`; gate the controller on `snoozeshare.security.enabled=true`; retain Bean Validation and reject unknown JSON fields.
- [x] 3. Run the named tests; verify deterministic `createdAt DESC, id ASC`, exact 404 `NOT_FOUND`, no entity serialization, and controller startup with the activated service.
- [x] 4. Run backend Checkstyle (`./gradlew checkstyleMain checkstyleTest`) and build.
- [x] 5. Update branch state; commit `feat: add Host listing API` (`b57b096`).

### T3: Make calendar mutations atomic and expose calendar API

**Delivers:** Month projection, inclusive date conversion, serializable block writes/removals with one fresh-transaction retry, audit/event behavior | **Satisfies:** F2,N1-N3 | **Depends on:** T0,T1 | **Wave:** 2

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/api/controller/HostCalendarController.java` | Host-owned month read, block creation, and deletion routes. |
| Create | `backend/src/main/java/com/snoozeshare/api/response/HostCalendarResponse.java` | Month/day and inclusive blocked-range response values. |
| Create | `backend/src/main/java/com/snoozeshare/api/request/CreateHostBlockRequest.java` | Validate ISO date values and the 240-code-point optional reason limit. |
| Modify | `backend/src/main/java/com/snoozeshare/service/AvailabilityService.java` and `backend/src/main/java/com/snoozeshare/service/impl/AvailabilityServiceImpl.java` (`createHostBlock`, `removeHostBlock`) | Apply one `SERIALIZABLE` transaction to overlap-check/write/delete and audit; retry SQLSTATE `40001` once in a fresh transaction, mapping a second failure to 409. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/AvailabilityBlockRepository.java` and `backend/src/main/java/com/snoozeshare/repository/BookingRepository.java` | Add bounded date/month projections needed for owned calendar results, including existing bookings that produce synthetic calendar blocks. |
| Modify | `backend/src/main/java/com/snoozeshare/domain/enums/AuditAction.java` | Add explicit audit actions for Host block creation/removal. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostCalendarIntegrationTest.java` | Inclusive wire/exclusive stored dates, precedence, ownership, overlap, audit, and concurrent same-owner requests for one listing (E3,E6,E18). |

**Checklist**

- [x] 1. Write failing tests for E3,E6,E18; cover malformed/past/reversed/overlapping ranges, cross-host 404s, and two concurrent overlapping requests by the authorized listing owner (two distinct owners cannot both be authorized for one listing).
- [x] 2. Add the month DTO/controller and transactional mutation retry; convert inclusive API end dates to the existing half-open service representation exactly once.
- [x] 3. Run tests; assert one overlapping block at most, source-specific 409 after an overlap retry (booking precedence), 409 `STATE_CONFLICT` after a second serialization failure, and audit only for committed writes.
- [x] 4. Run backend Checkstyle (`./gradlew checkstyleMain checkstyleTest`) and build.
- [x] 5. Update branch state; commit `feat: expose atomic Host calendar API` (`fb4188a`).

### T4: Serialize Host booking decisions

**Delivers:** Booking row lock, post-lock state validation, deterministic conflict result | **Satisfies:** F3,N1,N2 | **Depends on:** T0,T1 | **Wave:** 2

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/api/controller/HostRequestsController.java` | Paged pending/history reads and Host decision endpoint. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/BookingRepository.java` | Add Host-scoped deterministic pageable pending/history queries and `PESSIMISTIC_WRITE` booking lookup. |
| Modify | `backend/src/main/java/com/snoozeshare/service/BookingService.java` and `backend/src/main/java/com/snoozeshare/service/impl/BookingServiceImpl.java` (`pendingRequestRowsFor`, `historyRowsFor`, `decide`) | Use bounded reads; acquire row lock and re-check PENDING before applying existing state, escrow/ledger, audit, and event rules. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostRequestsIntegrationTest.java` | Pending/history paging, ownership, decision result and audit. |
| Test | `backend/src/test/java/com/snoozeshare/service/BookingDecisionConcurrencyTest.java` | Two racing decisions produce one legal outcome and one 409 loser (E7,E19). |

**Checklist**

- [x] 1. Write failing request/query and concurrency tests for E3, E7, E17, and E19.
- [x] 2. Add Host-page repository methods and row locking; keep state-machine, escrow, ledger, audit, and event work in the existing service transaction.
- [x] 3. Run the tests; verify one decision changes state and writes ledger/audit/event once, while the concurrent loser receives 409 `STATE_CONFLICT`.
- [x] 4. Run backend Checkstyle (`./gradlew checkstyleMain checkstyleTest`) and build.
- [x] 5. Update branch state; commit `feat: serialize Host booking decisions` (`740e1bc`).

### T5: Add distinct Host conversation APIs

**Delivers:** Separate booking/ticket routes, transactional message/read behavior, after-commit events | **Satisfies:** F4,N1,N3,N4 | **Depends on:** T0,T1 | **Wave:** 2

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/api/controller/HostBookingMessagesController.java` | Host participant-scoped booking conversation summary/history/post/read routes. |
| Create | `backend/src/main/java/com/snoozeshare/api/controller/HostTicketMessagesController.java` | Host-channel ticket summary/history/post/read routes; no ticket creation action. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/BookingRepository.java` and `backend/src/main/java/com/snoozeshare/repository/TicketRepository.java` | Add host-scoped, stable pageable parent queries for bounded conversation summaries. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/BookingMessageRepository.java` and `backend/src/main/java/com/snoozeshare/repository/MessageRepository.java` | Add participant/channel-scoped pageable history reads with stable newest-first order. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/JdbcBookingMessageReadRepository.java` and `backend/src/main/java/com/snoozeshare/repository/JdbcMessageReadRepository.java` | Compare unread message timestamps with the timestamp of the last-read message. |
| Modify | `backend/src/main/java/com/snoozeshare/service/BookingConversationService.java`, `backend/src/main/java/com/snoozeshare/service/MessageService.java`, and corresponding `*Impl.java` files | Add bounded summary/history queries; ensure message events publish only after commit. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostBookingMessagesIntegrationTest.java` | Booking privacy, paging, post/read behavior, event-after-commit and rollback (E3,E16,E20). |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostTicketMessagesIntegrationTest.java` | HOST channel, private/resolved thread behavior, no ticket creation. |

**Checklist**

- [x] 1. Write failing tests for E3, E16, and E17 in both channels; E8 for private/resolved tickets; and E20 rollback behavior for booking and ticket messages.
- [x] 2. Implement distinct routes and scoped page queries; keep conversation content out of logs and ensure event listeners run only after transaction commit.
- [x] 3. Run tests; verify cross-tenant 404, resolved-thread 409, authorized read/post, bounded newest-first history, and no booking or ticket event on rollback.
- [x] 4. Run backend Checkstyle (`./gradlew checkstyleMain checkstyleTest`) and build.
- [x] 5. Update branch state; commit `feat: add Host booking and ticket messaging APIs` (`0b1f0ef`).

### T6: Add Host wallet API

**Delivers:** Wallet statement/balance projection and mocked operations through existing transaction, row-lock, ledger, and audit behavior; no schema migration | **Satisfies:** F5,N1-N4 | **Depends on:** T0,T1 | **Wave:** 2

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/api/controller/HostWalletController.java` | Wallet read, bounded statement, top-up and withdrawal endpoints; no idempotency header or replay logic. |
| Create | `backend/src/main/java/com/snoozeshare/api/response/HostWalletResponse.java` | Wallet plus bounded statement page response. |
| Create | `backend/src/main/java/com/snoozeshare/api/response/HostWalletMutationResponse.java` | `mocked`, resulting wallet, and transaction only; no `replayed` field. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/LedgerRepository.java` and `backend/src/main/java/com/snoozeshare/repository/JdbcLedgerRepository.java` | Add direct database paging ordered by `timestamp DESC, id DESC`; do not load and slice a complete ledger list. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/WalletRepository.java` | Add wallet-ID-only lookup so mutations do not preload a stale entity before `LedgerWriter` locks it. |
| Modify | `backend/src/main/java/com/snoozeshare/service/WalletService.java` and `backend/src/main/java/com/snoozeshare/service/impl/WalletServiceImpl.java` (`statementFor`, `topUp`, `withdraw`) | Expose bounded statement query and preserve existing wallet-row lock, transaction, ledger, audit, and after-commit event behavior. Each accepted request is a separate operation. |
| Modify | `backend/src/main/java/com/snoozeshare/service/impl/LedgerWriter.java` | Dispatch wallet transaction events only after the enclosing transaction commits. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostWalletIntegrationTest.java` | Amount validation, owner scope, paged statement, insufficient balance, and atomic balance/ledger/audit behavior (E3,E9,E10,E12,E15,E17). |

**Checklist**

- [x] 1. Write failing wallet tests for E9, E10, E12, and E17; prove failed mutations do not change balance or ledger and the statement query is database-bounded.
- [x] 2. Implement routes and response DTOs with no schema migration, request key, payload hash, or replay response; use existing wallet row locking and ledger transaction.
- [x] 3. Run tests; verify every received mutation is a separate operation, concurrent balance changes serialize, invalid/insufficient requests roll back, and no wallet payload is logged.
- [x] 4. Run backend Checkstyle (`./gradlew checkstyleMain checkstyleTest`) and build.
- [x] 5. Update branch state; commit `feat: expose Host wallet API` (`ba67ad4`).

### T7: Add authenticated frontend Host API adapter

**Delivers:** Typed requests, Firebase token attachment, paging, safe errors, shared provider contract | **Satisfies:** F6,F7,N3-N5 | **Depends on:** T0,T1,T2,T3,T4,T5,T6 | **Wave:** 3

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `frontend/src/lib/hostApi.ts` | Typed endpoint functions, page query builder, response parsing, mutation/error behavior; use `authenticatedFetch` and never accept host UUID input. |
| Create | `frontend/src/components/host/HostApiProvider.tsx` | Async API-backed resource states and mutations with loading/error/empty/not-found and refresh operations; cache the wallet summary in shared provider state. |
| Keep | `frontend/src/components/host/HostFixtureProvider.tsx` | Retain the synchronous fixture adapter for deterministic visual tests while production routes adopt the async API contract. |
| Modify | `frontend/src/components/host/hostTypes.ts` | Add API DTO/page types and map backend values into existing visual types. |
| Modify | `frontend/src/app/host/layout.tsx` | Use API-backed provider for production Host routes while tests can continue mounting fixture provider. |
| Modify | `frontend/src/components/host/HostRoleShell.tsx` | Read navbar wallet balance from the same in-memory provider query used by Wallet; do not request the statement here. |
| Test | `frontend/src/lib/hostApi.test.ts` | Authenticated paths, request payloads, stable errors, paging and no host ID (E4,E13,E17). |
| Test | `frontend/src/components/host/HostApiProvider.test.tsx` | Async states, refetch, no local success on errors, wallet no-auto-retry behavior (E11-E13,E23). |

**Checklist**

- [x] 1. Write failing adapter/provider tests for E4, E11, E13, E17, and E23; assert token use, bounded paging, and no client-selected host identifier.
- [x] 2. Implement typed API calls and provider state; preserve fixture adapter test usage and stable user-safe error mapping. Keep wallet summary shared in provider memory so the navbar and Wallet page do not fetch balance twice.
- [x] 3. Add wallet mutation in-flight lock and ambiguous-failure refresh; do not automatically resend top-up/withdraw requests.
- [x] 4. Run focused frontend tests and `npm run lint`.
- [x] 5. Update branch state; commit `feat: add authenticated Host API adapter` (`1d0adda`).

### T8: Wire listing, calendar, and request screens

**Delivers:** First three Host domains use real API reads and mutations | **Satisfies:** F1-F3,F6,F7,N3,N5 | **Depends on:** T2,T3,T4,T7 | **Wave:** 4

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/app/host/listings/page.tsx`, `frontend/src/app/host/listings/[listingId]/page.tsx`, `frontend/src/app/host/listings/new/page.tsx`, `frontend/src/app/host/listings/[listingId]/edit/page.tsx` | Replace production fixture reads with API states; preserve empty/loading/error/not-found and visual contract. |
| Modify | `frontend/src/app/host/listings/[listingId]/calendar/page.tsx`, `frontend/src/app/host/requests/page.tsx` | Load selected month and paged pending/history; refetch after successful block or decision. |
| Modify | `frontend/src/components/host/HostListingForm.tsx`, `frontend/src/components/host/HostCalendarPanel.tsx`, `frontend/src/components/host/HostRequestsTable.tsx` | Submit API mutations; retain existing layout and show safe errors without optimistic success. |
| Test | `frontend/src/app/host/listings/page.test.tsx`, `frontend/src/app/host/listings/[listingId]/page.test.tsx`, `frontend/src/app/host/listings/[listingId]/calendar/page.test.tsx`, `frontend/src/app/host/requests/page.test.tsx` | Verify API-backed states and successful/error mutation refetch (E3-E7,E11,E17,E21,E23). |

**Checklist**

- [x] 1. Write failing route tests for E3-E7, E11, E17, E21, and E23 against the API provider.
- [x] 2. Wire list/detail/forms, month calendar, and pending/history screens to T7 provider calls; send no host ID and keep mutation results server-authoritative.
- [x] 3. Run affected route/component tests; verify existing visual checks and keyboard flows remain intact.
- [x] 4. Run frontend lint and production build.
- [x] 5. Update branch state; commit the listing and calendar/request portions separately (`2d8190f`, `80f3322`).

### T9: Wire messages and wallet screens

**Delivers:** Remaining Host pages use separate API message resources and real wallet behavior | **Satisfies:** F4-F7,N2-N5 | **Depends on:** T5,T6,T7,T8 | **Wave:** 5

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/app/host/messages/page.tsx`, `frontend/src/app/host/wallet/page.tsx` | Load booking/ticket conversations separately and wallet/statement from API; preserve page-level states. |
| Modify | `frontend/src/components/host/HostMessagesPanel.tsx`, `frontend/src/components/host/HostWalletPanel.tsx` | Connect send/read and mocked top-up/withdraw; no new-ticket action, no automatic wallet retries, refresh after ambiguous failure. The Wallet page requests only its statement page in addition to the summary already cached for the navbar. |
| Test | `frontend/src/app/host/messages/page.test.tsx`, `frontend/src/app/host/wallet/page.test.tsx`, `frontend/src/components/host/HostMessagesWallet.test.tsx` | Verify channel separation, resolved state, wallet success/error and no automatic retry (E8-E13,E16,E20,E21,E23). |

**Checklist**

- [x] 1. Write failing message/wallet route tests for E8-E13, E16, E20, E21, and E23.
- [x] 2. Wire message channels, history paging, read/post behavior, wallet statement and mocked operations through T7; disable controls while mutations are pending.
- [x] 3. On ambiguous wallet outcome, refresh balance and statement; keep actions disabled if refresh fails and do not resend the mutation.
- [x] 4. Run affected frontend tests, lint, and build; verify no New Ticket action or real-payment-success copy appears.
- [x] 5. Update branch state; commit `feat: connect Host messages and wallet`.

### T10: Verify integration and close the phase

**Delivers:** Backend/frontend acceptance verification | **Satisfies:** F1-F7,N1-N5 | **Depends on:** T9 | **Wave:** 6

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `workflow/plans/2026-10-22-w-host-backend-integration.md` | Tick verified checklists and task statuses; keep plan status aligned with approval and execution. |
| Modify | `frontend/src/app/host/listings/[listingId]/calendar/page.tsx` | Render a safe unavailable/not-found state when the calendar or listing request returns 404 instead of leaving a breadcrumb-only page. |
| Test | `frontend/src/app/host/listings/[listingId]/calendar/page.test.tsx` | Verify a missing listing shows a user-visible not-found state and no blank calendar page. |
| Modify | `frontend/src/components/host/HostApiProvider.tsx` | Coalesce same-key page-resource and wallet-summary reads while they are in flight, including React Strict Mode effect replay. |
| Test | `frontend/src/components/host/HostApiProvider.test.tsx` | Verify Strict Mode effect replay issues one request for a page resource and one for the shared wallet summary. |
| Verify | Host backend/frontend routes in local environment | Check all eight Host routes at desktop/tablet/mobile widths, auth recovery, API requests, loading/error/empty states, and wallet ambiguous failure handling. |

**Checklist**

- [x] 1. Run `cd backend && ./gradlew build --rerun-tasks` and `cd frontend && npm test && npm run lint && NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 npm run build`.
- [x] 2. Add a failing regression for calendar 404, render a safe unavailable state, fix the live-discovered 390px navbar overflow with a wrapping-row regression, and coalesce same-key in-flight page, wallet-summary, and wallet-statement reads with three Strict Mode regression tests.
- [x] 3. Verify every Traceability row; all eight routes render within the viewport at desktop/tablet/mobile. Fresh authenticated Network captures (one actual GET per listed data set; browser OPTIONS preflights excluded) showed: `/host/listings` — `/api/auth/me`, `/api/host/listings?page=0&size=50`, `/api/host/wallet?page=0&size=1` (all 200); `/host/listings/new` — auth/me and wallet summary (200; no listing read before form submission); `/host/listings/{550e8400-e29b-41d4-a716-446655440000}` and `/edit` — auth/me and wallet summary (200), listing detail (404; UI not-found); `/calendar` — auth/me and wallet summary (200), `/api/host/listings/{id}/calendar?year=2026&month=10` and listing detail (404; safe unavailable state); `/host/requests` — auth/me, `/api/host/requests?view=pending&page=0&size=50`, `/api/host/requests?view=history&page=0&size=50`, wallet summary (200); `/host/messages` — auth/me, `/api/host/messages/bookings?page=0&size=50`, `/api/host/messages/tickets?page=0&size=50`, wallet summary (200; empty state, no read/write operation); `/host/wallet` — auth/me, `/api/host/wallet?page=0&size=50`, wallet summary (200). No duplicate GET or polling loop was observed after the in-flight fixes; the Wallet statement capture initially showed two reads and now shows one (D~23/D~25/D~26).
- [x] 4. Inspect the API error logger and request adapter; logs include only request ID/status/code/path, and adapter tests verify no client-supplied host ID or wallet retry metadata.
- [x] 5. Record verification evidence: backend fresh Checkstyle/build passed; frontend 176 tests across 32 files, lint, and production build passed.

**Verification evidence (2026-10-22):** Backend `./gradlew build --rerun-tasks` completed with `BUILD SUCCESSFUL`. After the wallet-statement dedup regression fix, the frontend suite passed 176 tests across 32 files, `npm run lint` passed, and the production build generated all 16 routes. The live browser rendered all eight Host routes at 1680px, 1280px, and 390px with document width equal to viewport width. Authenticated clean-route Network captures are recorded in checklist step 3; the post-fix `/host/wallet` capture shows exactly one each for auth/me, statement page, and wallet summary. No wallet/booking/message write or message-read operation or token invalidation occurred. N5 capture is complete. Read-only code review found two stale-record issues, which were reconciled; plan closeout lint reports 0 errors and 0 warnings. T10 is complete and awaits owner review (D~23/D~25/D~26/D~27).

### T11: Return complete listing and booking-request collections

**Delivers:** Complete, owner-scoped, deterministically ordered listing, pending-request, and history arrays, ignoring legacy pagination query parameters | **Satisfies:** F1,F3,F8,N1,N3,N5,E26,E27 | **Depends on:** T2,T4 | **Wave:** 7

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `backend/src/main/java/com/snoozeshare/api/controller/HostListingController.java` (`GET /api/host/listings`) | Remove `page`/`size` inputs and `PageResponse`; return the complete service result as an ordered array. Unknown legacy query parameters are ignored by Spring. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/PropertyRepository.java` | Replace the bounded Host query with an owner-scoped, stable-order complete query. |
| Modify | `backend/src/main/java/com/snoozeshare/service/ListingService.java`, `backend/src/main/java/com/snoozeshare/service/impl/ListingServiceImpl.java` (`listForHost`) | Remove paging arguments and pass through every result from the Host-scoped repository query; preserve projection/DTO mapping. |
| Modify | `backend/src/main/java/com/snoozeshare/api/controller/HostRequestsController.java` (`GET /api/host/requests`) | Keep the `view` selector; remove page/size inputs and return a complete array for pending/history. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/BookingRepository.java` | Add complete owner-scoped pending/history queries in their existing deterministic order; retain the row-lock query used by decisions. |
| Modify | `backend/src/main/java/com/snoozeshare/service/BookingService.java`, `backend/src/main/java/com/snoozeshare/service/impl/BookingServiceImpl.java` (`pendingRequestRowsFor`, `historyRowsFor`) | Remove page/size arguments and map all matching rows while leaving decision transactions unchanged. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostListingsControllerTest.java`, `backend/src/test/java/com/snoozeshare/api/host/HostRequestsIntegrationTest.java`, `backend/src/test/java/com/snoozeshare/repository/PropertyRepositoryTest.java` | Assert all 101 independently seeded listings, pending bookings, and history bookings are returned; verify order, owner exclusion, array shape, and identical results when legacy `page`/`size` params are supplied (E26,E27). Update repository tests that directly asserted obsolete listing pagination while retaining complete stable-order and owner-scope coverage. |

**Checklist**

- [x] 1. Add failing integration assertions for 101 owned listings, pending bookings, and history bookings; include another Host's records and legacy page/size query values. Focused tests failed against the current paged API as expected (and exposed one existing history assertion that must follow the new array contract).
- [x] 2. Remove pagination inputs and envelope from the two GET controller contracts; implement complete owner-scoped repository/service reads with stable ordering.
- [x] 3. Run `./gradlew test --tests 'com.snoozeshare.api.host.HostListingsControllerTest' --tests 'com.snoozeshare.api.host.HostRequestsIntegrationTest' --tests 'com.snoozeshare.repository.PropertyRepositoryTest'`; verify all rows, scope, order, and unchanged decision behavior (all focused tests pass).
- [x] 4. Run `./gradlew checkstyleMain checkstyleTest` and `./gradlew build` from `backend/` (both pass).
- [x] 5. Update branch-state Progress and Changes; commit `feat: return complete Host listing and request collections`.

### T12: Return complete booking and ticket conversation collections

**Delivers:** Complete owner/participant-scoped summary and message arrays, preserving privacy, ordering, read/post transactions, and after-commit events | **Satisfies:** F4,F8,N1,N3,N4,N5,E3,E8,E16,E20,E26,E27,E28 | **Depends on:** T5,T11 | **Wave:** 8

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `backend/src/main/java/com/snoozeshare/api/controller/HostBookingMessagesController.java` | Remove page/size from booking summary/history GETs and return complete arrays; preserve message post/read endpoints. |
| Modify | `backend/src/main/java/com/snoozeshare/api/controller/HostTicketMessagesController.java` | Remove page/size from ticket summary/history GETs and return complete arrays; preserve HOST-channel authorization and post/read endpoints. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/BookingRepository.java`, `backend/src/main/java/com/snoozeshare/repository/TicketRepository.java` | Add complete Host-scoped summary queries with the existing stable ordering. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/BookingMessageRepository.java`, `backend/src/main/java/com/snoozeshare/repository/MessageRepository.java` | Add complete participant/channel-scoped history queries; use `sentAt DESC, id DESC` for each message collection. |
| Modify | `backend/src/main/java/com/snoozeshare/service/BookingConversationService.java`, `backend/src/main/java/com/snoozeshare/service/impl/BookingConversationServiceImpl.java`, `backend/src/main/java/com/snoozeshare/service/MessageService.java`, `backend/src/main/java/com/snoozeshare/service/impl/MessageServiceImpl.java` | Remove pagination arguments and return complete authorized projections; do not change read/post transaction or after-commit event semantics. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostBookingMessagesIntegrationTest.java`, `backend/src/test/java/com/snoozeshare/api/host/HostTicketMessagesIntegrationTest.java` | Independently seed 101 booking summaries, booking messages, ticket summaries, and ticket messages; seed a 1,001-message thread; assert complete deterministic arrays, legacy parameter equivalence, owner/privacy filtering, and unchanged mutation/event behavior (E26-E28). |

**Checklist**

- [x] 1. Add failing integration tests for all four 101-row response types, a 1,001-message history, legacy page/size values, and cross-tenant/private-thread exclusion; they failed against the prior paged API and passed after implementation.
- [x] 2. Remove paging from both controllers, service interfaces/implementations, and repository reads; preserve `sentAt DESC, id DESC`, channel scope, and existing transactional writes.
- [x] 3. Run `./gradlew test --tests 'com.snoozeshare.api.host.HostBookingMessagesIntegrationTest' --tests 'com.snoozeshare.api.host.HostTicketMessagesIntegrationTest'`; complete histories, privacy, and existing event/read/write tests pass.
- [x] 4. Run `./gradlew checkstyleMain checkstyleTest` and `./gradlew build` from `backend/`; both pass.
- [x] 5. Update branch-state Progress and Changes; commit `feat: return complete Host conversation collections`.

### T13: Return the complete wallet statement

**Delivers:** A complete owner-scoped newest-first wallet statement array in the existing wallet response, preserving the balance and transaction shape | **Satisfies:** F5,F8,N1-N5,E9,E10,E12,E26,E27 | **Depends on:** T6 | **Wave:** 7

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `backend/src/main/java/com/snoozeshare/api/controller/HostWalletController.java` | Remove page/size from the wallet GET and return the full statement with the wallet summary; ignore legacy page/size query params. |
| Modify | `backend/src/main/java/com/snoozeshare/api/response/HostWalletResponse.java` | Replace the page envelope with the complete statement list; preserve wallet and transaction DTO field names. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/LedgerRepository.java`, `backend/src/main/java/com/snoozeshare/repository/JdbcLedgerRepository.java` | Replace bounded statement query with complete Host-wallet-scoped rows ordered `timestamp DESC, id DESC`. |
| Modify | `backend/src/main/java/com/snoozeshare/service/WalletService.java`, `backend/src/main/java/com/snoozeshare/service/impl/WalletServiceImpl.java` (`statementFor`) | Remove page/size arguments and map the full statement; preserve wallet mutation locking, ledger/audit transaction, and event dispatch. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostWalletIntegrationTest.java` | Assert 101 owner rows are all returned in order, another Host is excluded, legacy params do not change results, and wallet balance/mutation invariants remain intact (E26,E27). |

**Checklist**

- [x] 1. Add failing wallet integration assertions for 101 ledger rows, timestamp/ID tie-breaking, owner exclusion, and legacy query parameters; the test failed against the paged response and passed after implementation.
- [x] 2. Remove paging from the wallet GET/response/service/repository path without changing mocked operations, row locking, ledger/audit, or after-commit behavior.
- [x] 3. Run `./gradlew test --tests 'com.snoozeshare.api.host.HostWalletIntegrationTest'`; the full response and existing mutation atomicity tests pass.
- [x] 4. Run `./gradlew checkstyleMain checkstyleTest` and `./gradlew build` from `backend/`; both pass.
- [x] 5. Update branch-state Progress and Changes; commit `feat: return complete Host wallet statement`.

### T14: Replace paged frontend API and provider types

**Delivers:** Typed complete-array requests/responses and one request per collection dataset | **Satisfies:** F6-F8,N3-N5,E11,E13,E26,E27 | **Depends on:** T11,T12,T13 | **Wave:** 9

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/lib/hostApi.ts` | Remove page/size arguments and query construction; parse complete arrays for listings, requests, booking/ticket summaries and histories, and wallet statement. Keep calendar year/month query and authenticated fetch behavior. |
| Modify | `frontend/src/components/host/hostTypes.ts` | Replace page-envelope types with the documented arrays; preserve existing view-model and mutation types. |
| Modify | `frontend/src/components/host/HostApiProvider.tsx` | Store complete result arrays, load each collection once, and simplify message/wallet history state without altering in-flight request coalescing; cache the complete wallet response so the navbar badge and Wallet page share one request and one statement array. |
| Modify | `frontend/src/app/host/listings/page.tsx`, `frontend/src/app/host/requests/page.tsx`, `frontend/src/app/host/messages/page.tsx`, `frontend/src/app/host/wallet/page.tsx` | Adapt current API-mode resource reads to complete arrays so this task remains buildable; pagination controls/handlers are removed in T15/T16. |
| Test | `frontend/src/lib/hostApi.test.ts`, `frontend/src/components/host/HostApiProvider.test.tsx`, `frontend/src/app/host/listings/page.test.tsx`, `frontend/src/app/host/requests/page.test.tsx`, `frontend/src/app/host/messages/page.test.tsx`, `frontend/src/app/host/wallet/page.test.tsx` | Assert no page/size query or arguments, complete array mapping beyond 100 rows, legacy parameters absent from generated requests, one request per dataset under Strict Mode, Wallet reuses the navbar's complete statement response without a second GET, and auth/error/wallet retry semantics are unchanged. |

**Checklist**

- [x] 1. Add failing adapter/provider tests using 101 rows and update API-mode route fixtures to the complete-array contract; assertions confirm no page/size URL or arguments and one shared complete wallet response.
- [x] 2. Update API response types and provider data loading to arrays; preserve auth, error, in-flight request coalescing, mutation recovery, and single-request wallet caching.
- [x] 3. Run the focused adapter/provider tests (13 pass), full `npm test` (177 pass), `npm run lint` (passes), and production build (16 routes generated).
- [x] 4. Update branch-state Progress and Changes; commit `refactor: consume complete Host API collections`.

### T15: Remove listing and request pagination UI

**Delivers:** Complete listings and pending/history request data rendered in existing bounded scroll regions without page controls | **Satisfies:** F6,F8,N5,E26-E27 | **Depends on:** T14 | **Wave:** 10

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/app/host/listings/page.tsx`, `frontend/src/app/host/requests/page.tsx` | Remove page state, incremental fetch handlers, and pagination props; render complete provider arrays. |
| Modify | `frontend/src/components/host/HostRequestsTable.tsx` | Remove any pagination/load-more affordance while retaining pending/history table structure and existing internal scrolling. |
| Test | `frontend/src/app/host/listings/page.test.tsx`, `frontend/src/app/host/requests/page.test.tsx`, `frontend/src/components/host/HostCalendarRequests.test.tsx` | Verify >100 listings and each request view render from one complete result, no page controls or extra fetches appear, and empty/loading/error states remain. |

**Checklist**

- [x] 1. Add route tests for 101 listings, 101 pending rows, and 101 history rows; assert complete rendering, no pagination controls, and one collection read per view.
- [x] 2. Remove page counters, request-table pagination props, and handlers; preserve the scroll containers, sticky table headers, decision behavior, and route/error behavior.
- [x] 3. Run the focused listing/request/calendar-request tests (31 pass), full `npm test` (177 pass), `npm run lint`, and production build (16 routes generated).
- [x] 4. Update branch-state Progress and Changes; commit `refactor: remove Host listing and request pagination`.

### T16: Remove message-history and wallet load-more UI

**Delivers:** Selected complete conversation history and complete wallet statement, with existing scroll regions and no load-more state/control | **Satisfies:** F5,F6,F8,N2-N5,E12,E16,E26-E28 | **Depends on:** T14 | **Wave:** 10

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/app/host/messages/page.tsx`, `frontend/src/app/host/wallet/page.tsx` | Consume complete summary and statement arrays; remove older-page/load-more state and handlers. |
| Modify | `frontend/src/components/host/HostMessagesPanel.tsx` | Remove history pagination props, loading state, and “Load older messages”; keep authorized thread selection, scroll-to-latest, read/post behavior, and internal transcript scrolling. |
| Modify | `frontend/src/components/host/HostWalletPanel.tsx` | Remove load-more props/state/control; retain the bounded internal statement scroll region and wallet mutation recovery lock. |
| Test | `frontend/src/app/host/messages/page.test.tsx`, `frontend/src/app/host/wallet/page.test.tsx`, `frontend/src/components/host/HostMessagesWallet.test.tsx` | Verify the complete 1,001-message history and 101-row statement render without follow-up requests; preserve resolved/private messaging and ambiguous wallet failure behavior. |

**Checklist**

- [x] 1. Add UI tests with 101 statement rows and a 1,001-message history; assert one complete response, no load-more control, and preserved scroll container.
- [x] 2. Remove pagination state/props/control while preserving channel privacy, read/post behavior, transcript scroll-to-latest, and wallet no-auto-retry recovery.
- [x] 3. Run focused tests (18 pass), full `npm test` (178 pass), `npm run lint`, and production build (16 routes generated).
- [x] 4. Update branch-state Progress and Changes; commit `refactor: remove Host message and wallet pagination`.

### T17: Verify the pagination reversal

**Delivers:** Backend/frontend contract, build, and authenticated read-only acceptance evidence for complete collections | **Satisfies:** F1-F8,N1-N6,E1-E28 | **Depends on:** T15,T16 | **Wave:** 11

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `workflow/plans/2026-10-22-w-host-backend-integration.md` | Record T11-T17 evidence and tick amendment checklists/status only after each task passes. |
| Verify | Backend and frontend Host API in local environment | Confirm all relevant endpoints return complete owner-scoped arrays, no pagination JSON or request parameters, stable ordering, one fetch per dataset, and calendar remains month-bounded. Use read-only browser/network inspection; do not invoke mutations. |

**Checklist**

- [x] 1. `./gradlew build --rerun-tasks` succeeded; frontend: 178 tests across 33 files, lint, and production build succeeded (16 routes generated).
- [x] 2. T11-T13 integration tests cover 101 rows independently for listings, pending/history requests, conversation summaries/messages, and wallet statements; booking-message history covers 1,001 rows in `sentAt DESC, id DESC`; legacy `page`/`size` equivalence is asserted.
- [x] 3. Authenticated Chrome Network capture: listings once; pending/history once each; booking/ticket conversation lists once each; wallet once and shared with Wallet view. Host GET URLs contain no `page`/`size`; only reads were performed. Local collections are empty, so large-result behavior is verified by tests.
- [x] 4. Reviewed Host identity/ownership and error boundaries, complete ordered collection queries, and wallet ledger transaction/row-lock path; no new N1/N3/N4 findings.
- [x] 5. Updated branch-state Progress and Changes; committed `test: verify complete Host collection contracts`.

### Original T0-T10 close-out (historical)

The checked items below document the original approved implementation and verification through T10. They do not close the pagination-removal amendment; T11-T17 remain the current acceptance work.

- [x] Every Traceability row is verified; all backend and frontend builds are green.
- [x] Spec status and plan task statuses reflect the delivered behavior; record any deviation in the branch state.
- [x] Update `workflow/project-state/branch-states/W-HOST.md`: W-HOST-API In review, concise Progress, Changes/Deviations complete, guide out-of-date note, and no completed session rows. Do not edit `PROJECT_STATE.md` or `docs/DeveloperGuide.md` on this feature branch.
- [x] Commit the close-out record with the final implementation task's documentation changes.
- [x] GATE: Hand off for operator branch review; do not push, open a PR, or merge until the operator approves.

### Pagination-removal amendment close-out

- [x] Every current Traceability row is verified against the complete-collection contract; T11-T17 checklists and task statuses reflect reality.
- [x] Backend Checkstyle/build and frontend tests/lint/production build pass after T17; authenticated read-only acceptance is recorded in the branch state.
- [x] Updated the branch state: W-HOST-API In review, concise Progress, Changes complete, guide out-of-date note, and no active sessions. `PROJECT_STATE.md` and `docs/DeveloperGuide.md` were not edited on this feature branch.
- [x] Committed the final amendment closeout record with the final verification task.
- [x] GATE: Hand off for operator branch review; no push, PR, or merge performed.
