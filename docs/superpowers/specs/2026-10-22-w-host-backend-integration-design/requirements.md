# Requirements

Ordered so identity and read contracts precede mutations.

## Functional

| ID | Requirement | Acceptance criterion |
|---|---|---|
| F1 | The backend shall expose authenticated Host listing reads and writes using W-DOMAIN services and DTOs. | Given an active Host, listing list/detail/create/update/status requests return or mutate only that Host's properties; another Host's property returns 404 `NOT_FOUND` without mutation. Detail includes listing metrics/reviews. |
| F2 | The backend shall expose Host calendar and manual blocking operations. | Given an owned listing and requested year/month, the calendar returns a date-ordered month grid with inclusive blocked ranges; a valid block can be created or removed; invalid/past/overlapping ranges return 422 `DOMAIN_RULE_VIOLATION`. |
| F3 | The backend shall expose pending and history booking requests and Host approve/reject decisions. | Given a pending request, the Host sees it in the pending response and a decision changes it once with the service state machine and audit rows. |
| F4 | The backend shall expose distinct Host booking-conversation and ticket-thread operations. | Given an authorized Host thread, paged summaries/history, read marking, and posting work; inaccessible threads return 404 `NOT_FOUND`; Host cannot read another user's private conversation. |
| F5 | The backend shall expose the Host wallet balance, statement, top-up, and withdrawal operations with durable idempotency. | Given an active Host and idempotency key, balance/statement load and a positive mocked operation returns the original ledger transaction on an exact retry, without duplicate ledger rows. |
| F6 | The frontend shall replace production fixture reads and local-only mutations with the Host API while preserving populated, empty, loading, error, not-found, and disabled/resolved states. | Given a valid authenticated API response, each existing Host route renders equivalent content; given a request failure, the existing error state appears and no stale success is shown. |
| F7 | The frontend shall attach the current Firebase ID token through the existing authenticated request path and map stable backend errors. | Given an expired or absent token, the API request is rejected and the UI offers the existing auth recovery path without sending a client-selected host ID. |

## Non-functional

| ID | Category | Requirement | Acceptance criterion |
|---|---|---|---|
| N1 | Security | Every Host operation shall enforce active Firebase identity, HOST role, and host ownership at the backend. | Controller security tests cover unauthenticated, GUEST, AGENT, inactive, unknown-property, and cross-host cases; all return 401/403/404 without a service mutation. |
| N2 | Integrity | Booking decisions and wallet mutations shall use row locking/idempotency with service transaction, state-machine, ledger, and audit invariants. | Integration tests verify exactly one legal transition and ledger result under concurrent requests, and no state or balance change on failure. |
| N3 | Compatibility | API responses shall use the existing request/response DTO layer and stable error envelope. | Contract tests validate JSON names/types, `requestId`, and error codes; no domain entity is serialized directly. |
| N4 | Observability | Host failures shall log request ID, status, safe code, and path only. | Log assertions or review checks show no token, registration code, wallet secret, SQL, or message body leakage. |
| N5 | Performance | Normal Host page reads shall complete with one API request per page data set and no client polling. | Browser/network tests show no duplicate request loop and each route's initial data load uses the documented request set. |
