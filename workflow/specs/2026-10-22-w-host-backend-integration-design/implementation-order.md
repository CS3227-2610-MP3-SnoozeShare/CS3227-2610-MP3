# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T0 | Activate W-DOMAIN services deterministically | Set `snoozeshare.persistence.enabled: true` in `application.yml`; activate service implementations and JDBC adapters from that property; test database-backed and database-free contexts | N6,E24,E25 |
| T1 | Add Host identity resolver and stable error boundary | Firebase-to-domain identity check, typed service/API failures, shared error envelope | F1-F7,N1,N3,N4 |
| T2 | Add listing API and detail projection | Owned listing collection/detail/create/update/status operations and metrics composition; verifies Host controller wiring against activated services | F1,N1,N3,N6,E24 |
| T3 | Make calendar mutations atomic and expose calendar API | Month projection, inclusive date conversion, SERIALIZABLE block writes/removals with one fresh-transaction retry, audit/event behavior | F2,N1-N3 |
| T4 | Serialize Host booking decisions | Booking row lock, post-lock state validation, deterministic conflict result | F3,N1,N2 |
| T5 | Add distinct Host conversation APIs | Booking/ticket routes, transactional message/read behavior, after-commit events | F4,N1,N3,N4 |
| T6 | Add Host wallet API | Wallet statement/balance projection and mocked operations through existing wallet transaction, row-lock, ledger, and audit behavior; no schema migration | F5,N1-N4 |
| T7 | Add authenticated frontend Host API adapter | Typed requests, Firebase token attachment, safe errors, shared provider contract (its original paging behavior is superseded by T14) | F6,F7,N3-N5 |
| T8 | Wire listing, calendar, and request screens | Replace fixtures for first three Host domains with real API data and mutations | F6,F7,N3,N5 |
| T9 | Wire messages and wallet screens | Replace remaining fixtures and local-only results with API behavior | F6,F7,N2-N5 |
| T10 | Verify integration and close the phase | Backend/controller/frontend coverage, builds, visual regression, state and guide handover | F1-F7,N1-N5 |
| T11 | Return complete listing and booking-request collections | Owner-scoped stable-order arrays; integration fixtures independently contain 101 listings, 101 pending bookings, and 101 history bookings | F1,F3,F8,N1,N3,N5,E26-E28 |
| T12 | Return complete booking and ticket conversation collections | Owner-scoped complete summary/history arrays; fixtures independently contain 101 booking summaries, booking messages, ticket summaries, and ticket messages; preserve privacy and transactional writes | F4,F8,N1,N3,N4,N5,E3,E8,E16,E26-E28 |
| T13 | Return the complete wallet statement | Complete owner-scoped statement array with 101 ledger rows; preserve wallet row locking, ledger/audit transaction behavior, and mocked operations | F5,F8,N1-N5,E9,E10,E12,E26-E28 |
| T14 | Replace paged frontend API and provider types | Typed complete-array responses and requests without `page`/`size`; one request per collection dataset | F6-F8,N3-N5,E11,E13,E26-E27 |
| T15 | Remove listing and request pagination UI | Render complete results in existing scroll regions and remove page state/controls | F6,F8,N5,E26-E28 |
| T16 | Remove message-history and wallet load-more UI | Load selected full thread and full statement; remove older/load-more state and controls while retaining existing scroll regions | F5,F6,F8,N2-N5,E12,E16,E26-E28 |
| T17 | Verify the pagination reversal | Backend/frontend tests, lint, builds, contract checks and authenticated read-only browser/network acceptance | F1-F8,N1-N6,E1-E28 |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T0 Activate W-DOMAIN services deterministically | - | T1 | 1 |
| T1 Add Host identity resolver and stable error boundary | - | T0 | 1 |
| T2 Add listing API and detail projection | T0,T1 | T3 | 2 |
| T3 Make calendar mutations atomic and expose calendar API | T0,T1 | T2 | 2 |
| T4 Serialize Host booking decisions | T0,T1 | T5,T6 | 2 |
| T5 Add distinct Host conversation APIs | T0,T1 | T4,T6 | 2 |
| T6 Add Host wallet API | T0,T1 | T4,T5 | 2 |
| T7 Add authenticated frontend Host API adapter | T0,T1,T2,T3,T4,T5,T6 | - | 3 |
| T8 Wire listing, calendar, and request screens | T2,T3,T4,T7 | - | 4 |
| T9 Wire messages and wallet screens | T5,T6,T7,T8 | - | 5 |
| T10 Verify integration and close the phase | T9 | - | 6 |
| T11 Return complete listing and booking-request collections | T2,T4 | T13 | 7 |
| T12 Return complete booking and ticket conversation collections | T5,T11 | T13 | 8 |
| T13 Return the complete wallet statement | T6 | T11,T12 | 7 |
| T14 Replace paged frontend API and provider types | T11,T12,T13 | - | 9 |
| T15 Remove listing and request pagination UI | T14 | T16 | 10 |
| T16 Remove message-history and wallet load-more UI | T14 | T15 | 10 |
| T17 Verify the pagination reversal | T15,T16 | - | 11 |

Shared-file hazards: T2/T3/T4/T5/T6 use disjoint backend controller/service/repository files; the error mapper and identity resolver from T1 are frozen before them. T11 and T13 use disjoint endpoint/service/repository and integration-test files and may run concurrently. T12 also changes `BookingRepository`, so it depends on T11 and runs in wave 8; it may run concurrently with T13. T14 freezes shared response types before the UI tasks. T15 and T16 use disjoint screen/test files and may run concurrently. T17 alone edits close-out state.
