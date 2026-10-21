# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | Add Host identity resolver and stable error boundary | Firebase-to-domain identity check, typed service/API failures, shared error envelope | F1-F7,N1,N3,N4 |
| T2 | Add listing API and detail projection | Owned listing collection/detail/create/update/status operations and metrics composition | F1,N1,N3 |
| T3 | Make calendar mutations atomic and expose calendar API | Month projection, inclusive date conversion, SERIALIZABLE block writes/removals with one fresh-transaction retry, audit/event behavior | F2,N1-N3 |
| T4 | Serialize Host booking decisions | Booking row lock, post-lock state validation, deterministic conflict result | F3,N1,N2 |
| T5 | Add distinct Host conversation APIs | Booking/ticket routes, transactional message/read behavior, after-commit events | F4,N1,N3,N4 |
| T6 | Add idempotent Host wallet API | Wallet statement/balance projection, idempotency migration/repository/service, mocked operation response | F5,N1-N4 |
| T7 | Add authenticated frontend Host API adapter | Typed requests, Firebase token attachment, paging, safe errors, shared provider contract | F6,F7,N3-N5 |
| T8 | Wire listing, calendar, and request screens | Replace fixtures for first three Host domains with real API data and mutations | F6,F7,N3,N5 |
| T9 | Wire messages and wallet screens | Replace remaining fixtures and local-only results with API behavior | F6,F7,N2-N5 |
| T10 | Verify integration and close the phase | Backend/controller/frontend coverage, builds, visual regression, state and guide handover | F1-F7,N1-N5 |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 Add Host identity resolver and stable error boundary | - | - | 1 |
| T2 Add listing API and detail projection | T1 | T3 | 2 |
| T3 Make calendar mutations atomic and expose calendar API | T1 | T2 | 2 |
| T4 Serialize Host booking decisions | T1 | T5,T6 | 2 |
| T5 Add distinct Host conversation APIs | T1 | T4,T6 | 2 |
| T6 Add idempotent Host wallet API | T1 | T4,T5 | 2 |
| T7 Add authenticated frontend Host API adapter | T1,T2,T3,T4,T5,T6 | - | 3 |
| T8 Wire listing, calendar, and request screens | T2,T3,T4,T7 | T9 | 4 |
| T9 Wire messages and wallet screens | T5,T6,T7,T8 | - | 5 |
| T10 Verify integration and close the phase | T9 | - | 6 |

Shared-file hazards: T2/T3/T4/T5/T6 use disjoint backend controller/service/repository files; the error mapper and identity resolver from T1 are frozen before them. T8 and T9 both change provider wiring and therefore run serially; T9 consumes the stable interface produced by T8. T10 alone edits close-out state.
