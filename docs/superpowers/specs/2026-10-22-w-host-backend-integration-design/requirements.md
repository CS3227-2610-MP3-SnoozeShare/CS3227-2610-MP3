# Requirements

Ordered so identity and read contracts precede mutations.

## Functional

| ID | Requirement | Acceptance criterion |
|---|---|---|
| F1 | The backend shall expose authenticated Host listing reads and writes using W-DOMAIN services and DTOs. | Given an active Host, listing list/detail/create/update/status requests return or mutate only that Host's properties; the listing GET returns every owned property in one ordered array. Another Host's property returns 404 `NOT_FOUND` without mutation. Detail includes listing metrics/reviews. |
| F2 | The backend shall expose Host calendar and manual blocking operations. | Given an owned listing and requested year/month, the calendar returns a date-ordered month grid with inclusive blocked ranges; a valid block can be created or removed; invalid/past/overlapping ranges return 422 `DOMAIN_RULE_VIOLATION`. |
| F3 | The backend shall expose complete pending and history booking request lists and Host approve/reject decisions. | Given an active Host, pending and history GETs return every matching row in deterministic order; a decision changes a pending request once with the service state machine and audit rows. |
| F4 | The backend shall expose distinct Host booking-conversation and ticket-thread operations. | Given an authorized Host thread, complete summary/history arrays, read marking, and posting work; inaccessible threads return 404 `NOT_FOUND`; Host cannot read another user's private conversation. |
| F5 | The backend shall expose the Host wallet balance, complete statement, top-up, and withdrawal operations through existing wallet services. | Given an active Host, balance and the entire newest-first statement load together; a valid mocked operation returns the resulting wallet and ledger transaction; each accepted request is a separate operation, and no request key or replay record is stored. |
| F6 | The frontend shall replace production fixture reads and local-only mutations with the Host API while preserving populated, empty, loading, error, not-found, and disabled/resolved states. | Given a valid authenticated API response, each existing Host route renders equivalent content; given a request failure, the existing error state appears and no stale success is shown. |
| F7 | The frontend shall attach the current Firebase ID token through the existing authenticated request path and map stable backend errors. | Given an expired or absent token, the API request is rejected and the UI offers the existing auth recovery path without sending a client-selected host ID. |
| F8 | Host collection and history reads shall return complete results without pagination. | Given a collection with more than 100 matching rows, one request without pagination parameters returns every owner-scoped row in deterministic order; the response contains no `page`, `size`, or `hasMore` fields. |

## Non-functional

| ID | Category | Requirement | Acceptance criterion |
|---|---|---|---|
| N1 | Security | Every Host operation shall enforce active Firebase identity, HOST role, and host ownership at the backend. | Controller security tests cover unauthenticated, GUEST, AGENT, inactive, unknown-property, and cross-host cases; all return 401/403/404 without a service mutation. |
| N2 | Integrity | Booking decisions and wallet mutations shall preserve service transaction, state-machine, wallet row-lock, ledger, and audit invariants. | Integration tests verify exactly one legal booking transition, serialized wallet balance updates, and no state or balance change on failed operations. |
| N3 | Compatibility | API responses shall use the existing request/response DTO layer and stable error envelope. | Contract tests validate JSON names/types, `requestId`, and error codes; no domain entity is serialized directly. |
| N4 | Observability | Host failures shall log request ID, status, safe code, and path only. | Log assertions or review checks show no token, registration code, wallet secret, SQL, or message body leakage. |
| N5 | Performance | Host collection reads use one API request per data set and no client polling; there is no response-size or row-count bound. | Browser/network tests show no duplicate request loop, each initial collection load uses one request, and the accepted unbounded response risk is recorded and tested with more than 100 rows. |
| N6 | Runtime wiring | Persistence-backed service and JDBC adapter beans shall activate deterministically without relying on JPA repository registration order, and remain disabled in explicitly database-free contexts. | With the property omitted, `application.yml` supplies `true`; a database-backed context contains all 15 service contracts and three JDBC adapters, and the Host listing controller test in T2 starts. With the property explicitly `false`, a database-free context contains none of those service contracts or adapters. |
