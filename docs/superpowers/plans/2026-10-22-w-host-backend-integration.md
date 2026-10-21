# W-HOST Backend Integration Implementation Plan

- **Status:** Approved (teleifo, 2026-10-22)
- **Amended 2026-10-22:** Added prerequisite T0 with operator approval C~17; all service-dependent API tasks now depend on T0.

> **For agents:** tick `- [ ]` to `- [x]` as each step is completed, in the same commit as the work. Keep the W-HOST branch state's Progress cell current. Spec: [W-HOST Backend Integration Design](../specs/2026-10-22-w-host-backend-integration-design.md).

## Scope

**Goal:** Replace Host production fixtures with an authenticated `/api/host/**` API and API-backed pages, preserving the approved UI.

**In scope:** F1-F7; N1-N6. This includes deterministic activation of existing persistence-backed services, active Host identity/ownership, bounded reads, listing/calendar/booking/message/wallet endpoints, and authenticated frontend state/mutations.

**Non-goals:** Payment-provider integration (C75); AI features (Q6); polling/WebSockets (C78); visual redesign; wallet request idempotency (C~16 / SD12).

**Risk tier:** High - authorization, tenancy, booking state, messaging, and wallet ledger behavior require security, concurrency, transaction, and failure-path tests.

## Task order

| Task | Depends on | Can run concurrently with | Wave | Status |
|---|---|---|---|---|
| T0 Activate W-DOMAIN service beans deterministically | - | T1 | 1 | Done |
| T1 Add Host identity resolver and stable error boundary | - | T0 | 1 | Done |
| T2 Add listing API and detail projection | T0,T1 | T3 | 2 | Done |
| T3 Make calendar mutations atomic and expose calendar API | T0,T1 | T2 | 2 | Done |
| T4 Serialize Host booking decisions | T0,T1 | T5,T6 | 2 | Not started |
| T5 Add distinct Host conversation APIs | T0,T1 | T4,T6 | 2 | Not started |
| T6 Add Host wallet API | T0,T1 | T4,T5 | 2 | Not started |
| T7 Add authenticated frontend Host API adapter | T0,T1,T2,T3,T4,T5,T6 | - | 3 | Not started |
| T8 Wire listing, calendar, and request screens | T2,T3,T4,T7 | - | 4 | Not started |
| T9 Wire messages and wallet screens | T5,T6,T7,T8 | - | 5 | Not started |
| T10 Verify integration and close the phase | T9 | - | 6 | Not started |

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
| N5 | T2-T10 | bounded-query tests and browser network verification |
| N6 | T0,T2 | `DomainServiceActivationTest`, `HostListingsControllerTest`, `ServerPortDefaultTest` |
| E1,E2,E4,E5,E15,E22 | T1,T2 | `HostSecurityIntegrationTest`, `HostListingsControllerTest` |
| E3 | T2-T6 | Host listing, calendar, booking, message, and wallet integration tests |
| E6,E18 | T3 | `HostCalendarIntegrationTest` |
| E7,E19 | T4 | `HostRequestsIntegrationTest` |
| E8,E16,E20 | T5 | booking/ticket message integration tests |
| E9,E10,E12 | T6,T9 | `HostWalletIntegrationTest`, wallet page tests |
| E11,E13 | T7-T10 | API client tests and auth recovery route tests |
| E17 | T2-T6 | paged repository/controller integration tests |
| E21 | T8,T9 | Host text-rendering tests |
| E23 | T7-T9 | Host mutation failure/recovery tests |
| E24 | T0,T2 | `DomainServiceActivationTest`, `HostListingsControllerTest` |
| E25 | T0 | `ServerPortDefaultTest`, `ServerPortOverrideTest` |

## Verification gates

- Backend task tests: from `backend/`, run `./gradlew test --tests '<named test class>'`; run `./gradlew checkstyleMain checkstyleTest` after Java changes.
- Frontend task tests: from `frontend/`, run `npm test -- <test-file>`; also run `npm run lint` after TS/TSX changes.
- Before finishing: `cd backend && ./gradlew build`, then `cd frontend && npm test && npm run lint && npm run build` (with the configured `NEXT_PUBLIC_API_BASE_URL`); every F/N acceptance criterion has a passing test or recorded manual check.

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
- [x] 3. Run tests; assert one overlapping block at most, expected 422 after retry, 409 after a second serialization failure, and audit only for committed writes.
- [x] 4. Run backend Checkstyle (`./gradlew checkstyleMain checkstyleTest`) and build.
- [ ] 5. Update branch state; commit `feat: expose atomic Host calendar API`.

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

- [ ] 1. Write failing request/query and concurrency tests for E3, E7, E17, and E19.
- [ ] 2. Add Host-page repository methods and row locking; keep state-machine, escrow, ledger, audit, and event work in the existing service transaction.
- [ ] 3. Run the tests; verify one decision changes state and writes ledger/audit/event once, while the concurrent loser receives 409 `STATE_CONFLICT`.
- [ ] 4. Run backend Checkstyle (`./gradlew checkstyleMain checkstyleTest`) and build.
- [ ] 5. Update branch state; commit `feat: serialize Host booking decisions`.

### T5: Add distinct Host conversation APIs

**Delivers:** Separate booking/ticket routes, transactional message/read behavior, after-commit events | **Satisfies:** F4,N1,N3,N4 | **Depends on:** T0,T1 | **Wave:** 2

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/api/controller/HostBookingMessagesController.java` | Host participant-scoped booking conversation summary/history/post/read routes. |
| Create | `backend/src/main/java/com/snoozeshare/api/controller/HostTicketMessagesController.java` | Host-channel ticket summary/history/post/read routes; no ticket creation action. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/BookingMessageRepository.java` and `backend/src/main/java/com/snoozeshare/repository/MessageRepository.java` | Add participant/channel-scoped pageable history reads with stable newest-first order. |
| Modify | `backend/src/main/java/com/snoozeshare/service/BookingConversationService.java`, `backend/src/main/java/com/snoozeshare/service/MessageService.java`, and corresponding `*Impl.java` files | Add bounded summary/history queries; ensure message events publish only after commit. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostBookingMessagesIntegrationTest.java` | Booking privacy, paging, post/read behavior, event-after-commit and rollback (E3,E16,E20). |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostTicketMessagesIntegrationTest.java` | HOST channel, private/resolved thread behavior, no ticket creation. |

**Checklist**

- [ ] 1. Write failing tests for E3, E8, E16, E17, and E20 for each message channel.
- [ ] 2. Implement distinct routes and scoped page queries; keep conversation content out of logs and ensure event listeners run only after transaction commit.
- [ ] 3. Run tests; verify cross-tenant 404, resolved-thread 409, authorized read/post, bounded newest-first history, and no event on rollback.
- [ ] 4. Run backend Checkstyle (`./gradlew checkstyleMain checkstyleTest`) and build.
- [ ] 5. Update branch state; commit `feat: add Host booking and ticket messaging APIs`.

### T6: Add Host wallet API

**Delivers:** Wallet statement/balance projection and mocked operations through existing transaction, row-lock, ledger, and audit behavior; no schema migration | **Satisfies:** F5,N1-N4 | **Depends on:** T0,T1 | **Wave:** 2

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/api/controller/HostWalletController.java` | Wallet read, bounded statement, top-up and withdrawal endpoints; no idempotency header or replay logic. |
| Create | `backend/src/main/java/com/snoozeshare/api/response/HostWalletResponse.java` | Wallet plus bounded statement page response. |
| Create | `backend/src/main/java/com/snoozeshare/api/response/HostWalletMutationResponse.java` | `mocked`, resulting wallet, and transaction only; no `replayed` field. |
| Modify | `backend/src/main/java/com/snoozeshare/repository/LedgerRepository.java` and `backend/src/main/java/com/snoozeshare/repository/JdbcLedgerRepository.java` | Add direct database paging ordered by `timestamp DESC, id DESC`; do not load and slice a complete ledger list. |
| Modify | `backend/src/main/java/com/snoozeshare/service/WalletService.java` and `backend/src/main/java/com/snoozeshare/service/impl/WalletServiceImpl.java` (`statementFor`, `topUp`, `withdraw`) | Expose bounded statement query and preserve existing wallet-row lock, transaction, ledger, audit, and after-commit event behavior. Each accepted request is a separate operation. |
| Test | `backend/src/test/java/com/snoozeshare/api/host/HostWalletIntegrationTest.java` | Amount validation, owner scope, paged statement, insufficient balance, and atomic balance/ledger/audit behavior (E3,E9,E10,E12,E15,E17). |

**Checklist**

- [ ] 1. Write failing wallet tests for E9, E10, E12, and E17; prove failed mutations do not change balance or ledger and the statement query is database-bounded.
- [ ] 2. Implement routes and response DTOs with no schema migration, request key, payload hash, or replay response; use existing wallet row locking and ledger transaction.
- [ ] 3. Run tests; verify every received mutation is a separate operation, concurrent balance changes serialize, invalid/insufficient requests roll back, and no wallet payload is logged.
- [ ] 4. Run backend Checkstyle (`./gradlew checkstyleMain checkstyleTest`) and build.
- [ ] 5. Update branch state; commit `feat: expose Host wallet API`.

### T7: Add authenticated frontend Host API adapter

**Delivers:** Typed requests, Firebase token attachment, paging, safe errors, shared provider contract | **Satisfies:** F6,F7,N3-N5 | **Depends on:** T0,T1,T2,T3,T4,T5,T6 | **Wave:** 3

**Files**

| Action | Path | What changes |
|---|---|---|
| Create | `frontend/src/lib/hostApi.ts` | Typed endpoint functions, page query builder, response parsing, mutation/error behavior; use `authenticatedFetch` and never accept host UUID input. |
| Create | `frontend/src/components/host/HostApiProvider.tsx` | Async API-backed state and mutations with loading/error/empty/not-found and refresh operations; cache the wallet summary in shared provider state. |
| Modify | `frontend/src/components/host/HostFixtureProvider.tsx` | Preserve fixture provider as the deterministic test adapter behind the same consumer interface. |
| Modify | `frontend/src/components/host/hostTypes.ts` | Add API DTO/page types and map backend values into existing visual types. |
| Modify | `frontend/src/app/host/layout.tsx` | Use API-backed provider for production Host routes while tests can continue mounting fixture provider. |
| Modify | `frontend/src/components/host/HostRoleShell.tsx` | Read navbar wallet balance from the same in-memory provider query used by Wallet; do not request the statement here. |
| Test | `frontend/src/lib/hostApi.test.ts` | Authenticated paths, request payloads, stable errors, paging and no host ID (E4,E13,E17). |
| Test | `frontend/src/components/host/HostApiProvider.test.tsx` | Async states, refetch, no local success on errors, wallet no-auto-retry behavior (E11-E13,E23). |

**Checklist**

- [ ] 1. Write failing adapter/provider tests for E4, E11, E13, E17, and E23; assert token use, bounded paging, and no client-selected host identifier.
- [ ] 2. Implement typed API calls and provider state; preserve fixture adapter test usage and stable user-safe error mapping. Keep wallet summary shared in provider memory so the navbar and Wallet page do not fetch balance twice.
- [ ] 3. Add wallet mutation in-flight lock and ambiguous-failure refresh; do not automatically resend top-up/withdraw requests.
- [ ] 4. Run focused frontend tests and `npm run lint`.
- [ ] 5. Update branch state; commit `feat: add authenticated Host API adapter`.

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

- [ ] 1. Write failing route tests for E3-E7, E11, E17, E21, and E23 against the API provider.
- [ ] 2. Wire list/detail/forms, month calendar, and pending/history screens to T7 provider calls; send no host ID and keep mutation results server-authoritative.
- [ ] 3. Run affected route/component tests; verify existing visual checks and keyboard flows remain intact.
- [ ] 4. Run frontend lint and production build.
- [ ] 5. Update branch state; commit `feat: connect Host listings calendar and requests`.

### T9: Wire messages and wallet screens

**Delivers:** Remaining Host pages use separate API message resources and real wallet behavior | **Satisfies:** F4-F7,N2-N5 | **Depends on:** T5,T6,T7,T8 | **Wave:** 5

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `frontend/src/app/host/messages/page.tsx`, `frontend/src/app/host/wallet/page.tsx` | Load booking/ticket conversations separately and wallet/statement from API; preserve page-level states. |
| Modify | `frontend/src/components/host/HostMessagesPanel.tsx`, `frontend/src/components/host/HostWalletPanel.tsx` | Connect send/read and mocked top-up/withdraw; no new-ticket action, no automatic wallet retries, refresh after ambiguous failure. The Wallet page requests only its statement page in addition to the summary already cached for the navbar. |
| Test | `frontend/src/app/host/messages/page.test.tsx`, `frontend/src/app/host/wallet/page.test.tsx`, `frontend/src/components/host/HostMessagesWallet.test.tsx` | Verify channel separation, resolved state, wallet success/error and no automatic retry (E8-E13,E16,E20,E21,E23). |

**Checklist**

- [ ] 1. Write failing message/wallet route tests for E8-E13, E16, E20, E21, and E23.
- [ ] 2. Wire message channels, history paging, read/post behavior, wallet statement and mocked operations through T7; disable controls while mutations are pending.
- [ ] 3. On ambiguous wallet outcome, refresh balance and statement; keep actions disabled if refresh fails and do not resend the mutation.
- [ ] 4. Run affected frontend tests, lint, and build; verify no New Ticket action or real-payment-success copy appears.
- [ ] 5. Update branch state; commit `feat: connect Host messages and wallet`.

### T10: Verify integration and close the phase

**Delivers:** Backend/frontend acceptance verification | **Satisfies:** F1-F7,N1-N5 | **Depends on:** T9 | **Wave:** 6

**Files**

| Action | Path | What changes |
|---|---|---|
| Modify | `docs/superpowers/plans/2026-10-22-w-host-backend-integration.md` | Tick verified checklists and task statuses; keep plan status aligned with approval and execution. |
| Verify | Host backend/frontend routes in local environment | Check all eight Host routes at desktop/tablet/mobile widths, auth recovery, API requests, loading/error/empty states, and wallet ambiguous failure handling. |

**Checklist**

- [ ] 1. Run `cd backend && ./gradlew build` and `cd frontend && npm test && npm run lint && npm run build` with the configured API base URL.
- [ ] 2. Verify every Traceability row; record any unverified behavior or build divergence in branch-state Deviations instead of claiming completion.
- [ ] 3. Confirm the API logs omit tokens, message bodies, listing descriptions, and full wallet payloads; confirm frontend requests contain no client-supplied host ID.
- [ ] 4. Confirm the completed test and build output is recorded in the plan; do not mark completion without evidence.

### Close-out

- [ ] Every Traceability row is verified; all backend and frontend builds are green.
- [ ] Spec status and plan task statuses reflect the delivered behavior; record any deviation in the branch state.
- [ ] Update `docs/project-state/branch-states/W-HOST.md`: W-HOST-API In review, concise Progress, Changes/Deviations complete, guide out-of-date note, and no completed session rows. Do not edit `PROJECT_STATE.md` or `docs/DeveloperGuide.md` on this feature branch.
- [ ] Commit the close-out record with the final implementation task's documentation changes.
- [ ] GATE: Ask the operator for branch review; do not push, open a PR, or merge until the operator approves.
