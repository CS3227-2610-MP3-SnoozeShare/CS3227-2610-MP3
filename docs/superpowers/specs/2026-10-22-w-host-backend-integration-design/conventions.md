# Conventions

| Rule | Applies to | Source |
|---|---|---|
| REST controllers live under `com.snoozeshare.api.controller` and use `/api/host/**`; they call services, never repositories directly. | Backend boundary | C76, C79, § Architecture 4.9 |
| Resolve Firebase UID to a server-owned domain user; never accept an acting user/host ID from the browser. | Every Host endpoint | C79, C92, SD2 |
| Use separate request/response DTOs; do not serialize JPA entities. | JSON boundary | C73 |
| Enforce role at the security boundary and ownership/state rules in the service layer; controllers map failures to stable codes. | Authorization and errors | C70, C79, § Known Gaps |
| Money changes go through existing `WalletService`/`LedgerWriter` transactions and audit rows; controllers never adjust balances. | Wallet endpoints | § Conventions, C75 |
| Dates are ISO `yyyy-MM-dd`; UI inclusive block ends are converted using the existing half-open persistence convention. | Calendar endpoints | D5, W-DOMAIN `AvailabilityService` |
| Error JSON is `code`, `message`, `requestId`; logs contain request ID/status/safe code/path only. | All failures | C96 |
| Frontend API calls use one authenticated client and retain fixture adapters for deterministic tests. | Frontend Host provider | C78, SD3 |
| All paged collections default to 50, cap at 100, use stable ordering, and return `{items,page,size,hasMore}`. | Host collection endpoints | SD6 |
| Wallet mutations have no request idempotency key or replay store; each accepted request is a distinct mocked operation, and the client never retries automatically. | Wallet endpoints and ledger | SD12 |
| Booking decisions lock the booking row and re-check its state after acquiring the lock; overlapping block check/write is SERIALIZABLE. | Booking and availability service mutations | SD7 |
| Message and wallet event publication happens after the enclosing transaction commits. | Message and wallet services | SD7; C78 |
| Host API exceptions map to the single exact status/code matrix in `api.md`; controllers do not infer codes from exception message text. | REST error boundary | SD4, SD6, C96 |
| Backend controller tests use Spring security principal fixtures and Testcontainers where persistence is exercised; frontend tests assert request paths, payloads, state transitions, and no secret/host-ID spoofing. | Verification | § Conventions, C83 |
