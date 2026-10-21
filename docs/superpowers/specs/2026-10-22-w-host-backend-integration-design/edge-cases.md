# Edge Cases and Non-Happy Paths

IDs are permanent: never reuse or renumber. Every F/N requirement and API error appears below.

## Permission and identity

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E1 | F1,N1 | Missing or invalid bearer token | Return 401 with safe error envelope; no service call. | Prevent anonymous access. | controller |
| E2 | F1,N1 | Active GUEST or AGENT calls `/api/host/**` | Return 403 `ROLE_MISMATCH`; no Host data. | Prevent role bypass. | controller |
| E3 | F1,N1 | Host sends another host's property/listing/block/booking ID | Return 404 or 403 per endpoint contract; no mutation. | Prevent cross-tenant access. | integration |
| E4 | F7,N1 | Browser sends a forged `hostId` field | Ignore/reject unknown input; derive identity from token. | Prevent identity spoofing. | controller/frontend |

## Input and state

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E5 | F1,N3 | Blank listing field, zero rate, invalid enum, or unknown JSON field | Return 400 `VALIDATION_ERROR`; no persistence. | Preserve DTO validation. | controller |
| E6 | F2 | Past, reversed, overlapping, or malformed block range | Return 400 for malformed input or 422 `DOMAIN_RULE_VIOLATION` for domain rejection; existing blocks remain unchanged. | Preserve date policy and D5. | service/integration |
| E7 | F3,N2 | Approve/reject request after another decision | Return 409 `STATE_CONFLICT`; no second ledger/audit/event. | Prevent double decisions and payout/refund. | integration |
| E8 | F4 | Post to resolved ticket or inaccessible/private thread | Return 403/404 or 409 per contract; no message created. | Preserve channel privacy and read-only resolved state. | service/integration |
| E9 | F5,N2 | Zero, negative, excessive-precision, or over-limit wallet amount | Return 400 `VALIDATION_ERROR`; no balance change. | Protect money invariants. | controller |
| E10 | F5,N2 | Withdraw more than available balance | Return 422 `DOMAIN_RULE_VIOLATION`; no ledger row. | Prevent negative wallet. | service/integration |

## Failure, concurrency, and UI recovery

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E11 | F6,N5 | API timeout or Render sleep | Show existing error/retry state; do not show success or fabricate fixture replacement. | Matches accepted dev sleep gap. | frontend |
| E12 | F6 | Mutation returns error after user confirmation | Keep prior state, show safe error, permit retry; never append a local success row. | Prevent false UI state. | frontend |
| E13 | F7,N4 | Expired Firebase token | API client refreshes once or routes to auth recovery; token is never logged. | Preserve session security. | frontend/controller |
| E14 | N2 | Two decisions or wallet mutations race | Existing transaction/isolation/locking yields one legal result; loser receives 409/422 and no duplicate ledger. | Protect concurrent money/state changes. | integration |
| E15 | N3 | Unexpected backend exception | Return 500 `INTERNAL_ERROR` with request ID; logs contain safe correlation only. | Prevent information leakage and enable support. | controller |
| E16 | N4 | Message contains sensitive text | Response may display it only to authorized participants; logs never contain body. | Protect personal data. | integration/review |

## What could corrupt data (High tier)

- A controller using a client-supplied host ID could mutate another host's property; SD2 and E3/E4 prevent it.
- A controller bypassing `BookingService` or `WalletService` could skip state, audit, or ledger invariants; controller tests and conventions forbid direct repository/ledger calls.
- Retrying a non-idempotent wallet or decision mutation could duplicate money/state; the UI does not auto-retry and the service transaction/state checks reject repeats.
- A partial failure after a state change could leave missing ledger/audit rows; existing service transaction boundaries remain the sole mutation path and are covered by integration tests.
