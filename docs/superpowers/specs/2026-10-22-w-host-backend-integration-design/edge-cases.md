# Edge Cases and Non-Happy Paths

IDs are permanent: never reuse or renumber. Every F/N requirement and API error appears below.

## Permission and identity

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E1 | F1,N1 | Missing or invalid bearer token | Return 401 with safe error envelope; no service call. | Prevent anonymous access. | controller |
| E2 | F1,N1 | Active GUEST or AGENT calls `/api/host/**` | Return 403 `ROLE_MISMATCH`; no Host data. | Prevent role bypass. | controller |
| E3 | F1,N1 | Host sends another host's property/listing/block/booking ID | Return 404 `NOT_FOUND`; no mutation. | Prevent cross-tenant access and resource enumeration. | integration |
| E4 | F7,N1 | Browser sends a forged `hostId` field | Ignore/reject unknown input; derive identity from token. | Prevent identity spoofing. | controller/frontend |

## Input and state

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E5 | F1,N3 | Blank listing field, zero rate, invalid enum, or unknown JSON field | Return 400 `VALIDATION_ERROR`; no persistence. | Preserve DTO validation. | controller |
| E6 | F2 | Past, reversed, overlapping, or malformed block range | Malformed date/body returns 400 `VALIDATION_ERROR`; validly shaped but past/reversed/overlapping range returns 422 `DOMAIN_RULE_VIOLATION`; existing blocks remain unchanged. | Preserve date policy and D5. | service/integration |
| E7 | F3,N2 | Approve/reject request after another decision | Return 409 `STATE_CONFLICT`; no second ledger/audit/event. | Prevent double decisions and payout/refund. | integration |
| E8 | F4 | Post to resolved ticket or inaccessible/private thread | Return 404 `NOT_FOUND` for inaccessible thread; return 409 `STATE_CONFLICT` for a visible resolved ticket; no message created. | Preserve channel privacy and read-only resolved state. | service/integration |
| E9 | F5,N2 | Zero, negative, excessive-precision, or over-limit wallet amount | Return 400 `VALIDATION_ERROR`; no balance change. | Protect money invariants. | controller |
| E10 | F5,N2 | Withdraw more than available balance | Return 422 `DOMAIN_RULE_VIOLATION`; no ledger row. | Prevent negative wallet. | service/integration |

## Failure, concurrency, and UI recovery

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E11 | F6,N5 | API timeout or Render sleep | Show existing error/retry state; do not show success or fabricate fixture replacement. | Matches accepted dev sleep gap. | frontend |
| E12 | F5,F6,N2 | Wallet mutation response is ambiguous because of a timeout or connection loss | Do not resend the mutation automatically; refresh balance and statement. Keep wallet actions disabled until refresh succeeds. A later explicit confirmation is a new operation and may create a new ledger row. | Make the accepted no-deduplication behavior visible and prevent accidental client retries. | frontend/integration |
| E13 | F7,N4 | Expired Firebase token | API client refreshes once or routes to auth recovery; token is never logged. | Preserve session security. | frontend/controller |
| E15 | N3 | Unexpected backend exception | Return 500 `INTERNAL_ERROR` with request ID; logs contain safe correlation only. | Prevent information leakage and enable support. | controller |
| E16 | N4 | Message contains sensitive text | Response may display it only to authorized participants; logs never contain body. | Protect personal data. | integration/review |
| E17 | F1,F3,F4,F5,N5 | Collection has more rows than the requested page | Execute an owner-scoped database page query reading at most `size + 1`; return only `size` rows (default 50, max 100), stable ordering, and correct `hasMore`; calendar returns no more than 42 cells. | Bound both response and server memory/query work. | integration |
| E18 | F2,N2 | Two hosts concurrently create overlapping blocks for the same listing | On SQLSTATE `40001`, retry once in a fresh transaction; the retried overlap returns 422 `DOMAIN_RULE_VIOLATION`. If serialization fails again, return 409 `STATE_CONFLICT`; no overlapping rows remain. | Prevent availability race and avoid leaking serialization failures as 500s. | integration |
| E19 | F3,N2 | Two different decisions race for one pending booking | Row lock serializes decisions; one succeeds, the other returns 409 `STATE_CONFLICT`; one legal transition/event/audit outcome exists. | Prevent conflicting booking outcomes. | integration |
| E20 | F4,N2 | Message insert succeeds but event dispatch or request fails | Message commits once; event is dispatched after commit; a rolled-back insert publishes no event. | Keep notifications consistent with stored messages. | integration |
| E21 | F1,F4,N4 | Listing text or message contains HTML/script-like content | Store/display as text; React renders escaped text; no raw HTML execution. | Prevent stored script injection. | frontend/integration |
| E22 | N1,N4 | Auth profile exists but matching domain user is absent or role/status/UID differs | Return 409 `PROFILE_INCONSISTENT`, log request ID and safe code only, no domain operation. | Detect split-profile drift without trusting either client data or mismatched records. | controller |
| E23 | F5,F6,F7 | A wallet, listing, block, decision, or message mutation returns an API error | Keep prior server state, show the safe mapped message, and do not automatically retry wallet mutations; after an ambiguous wallet failure refresh wallet data before another explicit operation. | Prevent fixture-era optimism and accidental duplicate wallet operations. | frontend |

## What could corrupt data (High tier)

- Host or guest attempts cross-tenant reads/writes: identity is resolved from the verified principal; host ownership is checked by domain services; E2-E4 and E22 cover rejection.
- A client or network repeats a wallet request: each accepted request is a distinct operation by design; the UI sends no automatic retry and refreshes after ambiguous outcomes (E12).
- Competing booking decisions: PESSIMISTIC_WRITE row lock plus pending-state check allows one transition; E19 verifies loser behavior.
- Concurrent overlapping availability blocks: SERIALIZABLE transaction wraps overlap-check and write; E18 verifies serialization.
- Service failure midway through booking/wallet mutation: one transaction contains state, ledger, and audit records; failure rolls all of them back; E7/E10/E19 cover this.
- Message delivery event fires for an uncommitted row: event dispatch is AFTER_COMMIT; E20 verifies rollback behavior.
- Malicious user text executes in the browser: no raw HTML rendering; E21 verifies escaping.
- Large collection reads consume memory or bandwidth: owner-scoped database queries fetch at most 101 rows, page size is capped at 100, calendar at 42 cells; E17 verifies the bound.

## Threats

| Actor | Abuse | Mitigation | Test |
|---|---|---|---|
| Authenticated non-Host | Call Host namespace | RoleAuthorizationFilter returns 403 before controller; E2 | Controller security test |
| Host | Guess another tenant's resource IDs | Resolve owner from Firebase UID and return indistinguishable 404; E3 | Cross-host integration test |
| Host or network retry | Duplicate a mocked wallet mutation after an ambiguous response | No automatic retry; refresh wallet data before another explicit confirmation; duplicate explicit operations are distinct by design; E12 | UI/API retry behavior test |
| Two concurrent Hosts/requests | Race booking decision or date block | Row lock or SERIALIZABLE transaction; E18/E19 | Concurrent integration tests |
| Host or guest | Submit HTML/script text in profile content/chat | React escaped rendering; never use raw HTML; E21 | Frontend rendering test |
| Authenticated user | Request oversized pages or flood response size | Maximum page size 100 and month capped to 42 dates; E17 | Controller paging test |
| Operator/log reader | Read tokens, message content, descriptions from logs | Safe error/log allowlist; E15/E16 | Log capture test |
