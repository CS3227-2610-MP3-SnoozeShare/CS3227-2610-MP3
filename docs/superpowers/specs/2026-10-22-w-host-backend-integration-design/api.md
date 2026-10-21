# API

All operations require a verified Firebase bearer token and an active database profile with role `HOST`. The controller resolves the domain `User` UUID from the token UID and passes that UUID to W-DOMAIN services. The client never supplies an acting host ID.

## GET /api/host/listings

| Field | Definition |
|---|---|
| Caller and principal | Authenticated Firebase principal; HOST role. |
| Authorization rule | Active profile and HOST role in the security boundary; result is `ListingService.findByHostId(domainHostId)`. |
| Tenancy scope | Only properties whose `hostId` equals the resolved domain host UUID. |
| Inputs | None. |
| Outputs | Array of `PropertyResponse`; host ID is the resolved owner, never client input. |
| Errors | Missing/invalid token → 401; inactive/wrong role → 403; domain lookup failure → 404; unexpected failure → 500 `INTERNAL_ERROR`. |
| Idempotency | Read-only. |
| Transaction boundary | Read transaction. |
| Events / audit | None. |
| Logging | Request ID, status, safe code, path; never token or entity internals. |
| Compatibility | New endpoint; fixture adapter remains test-only. |

## POST /api/host/listings; PUT /api/host/listings/{listingId}; PATCH /api/host/listings/{listingId}/status

| Field | Definition |
|---|---|
| Caller and principal | Authenticated Firebase principal; HOST role. |
| Authorization rule | Resolve host UUID; load/validate ownership; call `ListingService.create`, `update`, or `updateStatus`. |
| Tenancy scope | Create uses the resolved host; update/status may affect only an owned listing. |
| Inputs | Existing `CreateListingRequest`, `UpdateListingRequest`, and `UpdateListingStatusRequest`; Bean Validation plus domain validation; UUID path for existing listing. |
| Outputs | `PropertyResponse`; no credentials or unrelated users. |
| Errors | Invalid JSON/validation → 400 `VALIDATION_ERROR`; missing listing → 404 `NOT_FOUND`; non-owner → 403 `FORBIDDEN`; illegal status/rate/date/domain rule → 422 `DOMAIN_RULE_VIOLATION`; unexpected → 500. |
| Idempotency | Create is not idempotent; repeated update/status with the same representation is a no-op at the domain state level and returns current state. |
| Transaction boundary | Existing service transaction covers property mutation and required audit rows. |
| Events / audit | Existing service audit/events only; controller does not write ledger or audit rows directly. |
| Logging | Safe request metadata only; never full descriptions, tokens, or monetary secrets. |
| Compatibility | Uses W-DOMAIN DTOs and `ListingService`; no schema change. |

## GET /api/host/listings/{listingId}/calendar; POST /api/host/listings/{listingId}/blocks; DELETE /api/host/blocks/{blockId}

| Field | Definition |
|---|---|
| Caller and principal | Authenticated active HOST. |
| Authorization rule | Verify listing/block ownership before calling `AvailabilityService.blocksFor`, `createHostBlock`, or `removeHostBlock`; booked dates come from the existing booking query used by the calendar response. |
| Tenancy scope | Only the selected Host's listing and blocks. |
| Inputs | UUID path values; `CreateHostBlockRequest` with non-null dates, inclusive UI range converted per D5, optional reason limited to 240 UTF-8 characters. |
| Outputs | Calendar response containing listing ID, booked ranges, blocked ranges, and `AvailabilityBlockResponse`; deletion returns 204. |
| Errors | Invalid UUID/body → 400; unknown/non-owned listing or block → 404/403; past, reversed, overlapping, or illegal range → 422 `DOMAIN_RULE_VIOLATION`; unexpected → 500. |
| Idempotency | Repeating a delete after removal returns 404; creating an identical block twice is rejected by the domain rule rather than silently duplicated. |
| Transaction boundary | Existing availability service transaction and audit rules. |
| Events / audit | Existing domain events/audit only. |
| Logging | Request ID and safe range metadata; never message bodies or tokens. |
| Compatibility | Calendar remains visually six rows; API supplies real date state. |

## GET /api/host/requests?view=pending|history; POST /api/host/requests/{bookingId}/decision

| Field | Definition |
|---|---|
| Caller and principal | Authenticated active HOST. |
| Authorization rule | Use `BookingService.pendingRequestRowsFor` or `historyRowsFor`; decision passes resolved host UUID to `BookingService.decide`. |
| Tenancy scope | Only bookings for properties owned by the resolved Host. |
| Inputs | `view` is exactly `pending` or `history`; decision uses `BookingDecisionRequest` with non-null `approve` and optional message limited to 500 UTF-8 characters. |
| Outputs | Arrays of `HostBookingRowResponse`; decision returns `BookingResponse`. |
| Errors | Invalid view/body → 400; unknown/non-owned booking → 404/403; stale/non-pending/legal-state failure → 409 `STATE_CONFLICT`; validation/domain failure → 422; unexpected → 500. |
| Idempotency | A decision is accepted once; repeat after transition returns 409 and does not notify or move money twice. |
| Transaction boundary | Existing booking decision transaction includes state, escrow/ledger effects, audit, and event publication. |
| Events / audit | Existing booking, wallet, and audit events only. |
| Logging | Request ID, booking ID, safe status; never guest message content or tokens. |
| Compatibility | Preserves the existing pending/history table and four-row viewport behavior. |

## GET /api/host/messages; GET /api/host/messages/{threadId}; POST /api/host/messages/{threadId}; POST /api/host/messages/{threadId}/read

| Field | Definition |
|---|---|
| Caller and principal | Authenticated active HOST. |
| Authorization rule | Resolve role HOST and pass resolved user UUID/role to `BookingConversationService` or `MessageService`; service enforces thread visibility. |
| Tenancy scope | Booking conversations only for bookings involving the Host; ticket messages only in the Host channel for tickets visible to that Host. |
| Inputs | Thread UUID and `PostMessageRequest.body`, non-blank and max 2,000 UTF-8 characters. |
| Outputs | Conversation summaries, `MessageResponse`/`BookingConversationSummaryResponse`, or 204 for read marking. |
| Errors | Invalid body/path → 400; inaccessible thread → 404/403; resolved/read-only thread or invalid booking state → 409; unexpected → 500. |
| Idempotency | Posting is not idempotent; read marking is idempotent. |
| Transaction boundary | Existing message insert/read transaction; publication after commit. |
| Events / audit | Existing message events; no ticket creation from Host messaging. |
| Logging | Request ID, thread ID, status; never message body, token, or personal content. |
| Compatibility | Retains the UI decision that Host messaging has no New Ticket action. |

## GET /api/host/wallet; POST /api/host/wallet/top-up; POST /api/host/wallet/withdraw

| Field | Definition |
|---|---|
| Caller and principal | Authenticated active HOST. |
| Authorization rule | Resolve user UUID; call `WalletService` for that user only. |
| Tenancy scope | Only the resolved Host wallet and its statement. |
| Inputs | `WalletAmountRequest.amount`, positive `BigDecimal`, maximum 2 decimal places, maximum 100,000.00 SGD per operation. |
| Outputs | `WalletResponse`, `TransactionResponse`, and statement rows; response says `MOCKED` where the UI needs to distinguish simulation from payment. |
| Errors | Invalid amount → 400; wallet missing → 404; insufficient funds/invalid operation → 422; unexpected → 500. |
| Idempotency | Operations are mocked and not idempotent; the UI must require explicit confirmation and never retry automatically. |
| Transaction boundary | Existing wallet/ledger transaction writes balance, transaction/audit row, and post-commit event together. |
| Events / audit | Existing ledger and audit events; no payment provider call. |
| Logging | Request ID, operation type, amount band/status; never token, credentials, or full wallet response in logs. |
| Compatibility | Preserves the accepted mocked top-up/withdraw limitation (C75). |
