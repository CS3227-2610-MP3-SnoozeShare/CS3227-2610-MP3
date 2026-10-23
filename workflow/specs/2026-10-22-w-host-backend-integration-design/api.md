# API

## Shared identity, collection reads, and errors

Every route requires a verified Firebase bearer token. `RoleAuthorizationFilter` resolves an active HOST profile. `HostIdentityResolver` uses that token UID with `AuthUserRepository.findByFirebaseUid` and confirms the matching domain `User` has the same UUID, `firebaseUid`, HOST role, and ACTIVE status. The browser never sends an acting host ID.

Host collection and history GETs return the complete matching result in one response. They define no `page` or `size` input; if either query parameter is supplied by an older caller, it is ignored and the complete result is still returned. List responses are JSON arrays of response DTOs. The wallet response remains an object containing `wallet` and a complete `statement` array. No collection row-count or response-size cap applies. Queries retain server-side ownership/conversation scope and deterministic database ordering; they do not slice results in application memory. Ordering is: listings `createdAt DESC, id ASC`; booking rows `createdAt DESC, id ASC`; conversation summaries `lastMessageAt DESC, conversationId ASC`; message history `sentAt DESC, id DESC`; wallet statement `timestamp DESC, id DESC`. Calendar is one month (at most 42 day cells) and is not a collection-history endpoint.

Request validation errors use 400 `VALIDATION_ERROR`; domain rules use 422 `DOMAIN_RULE_VIOLATION`; stale state uses 409 `STATE_CONFLICT`; inaccessible or absent owned resources use 404 `NOT_FOUND`; wrong role/inactive profile use 403 `ROLE_MISMATCH`/`PROFILE_INACTIVE`; unexpected errors use 500 `INTERNAL_ERROR`. Every error body is `AuthErrorResponse(code,message,requestId)` with safe fixed text.

For every endpoint, logs include request ID, status, safe code, and route only. Never log bearer tokens, message bodies, listing descriptions, registration codes, or full wallet payloads.

## GET /api/host/listings

| Field | Definition |
|---|---|
| Caller and principal | Verified Firebase principal; active HOST. |
| Authorization / tenancy | Resolve domain host UUID server-side; `ListingService.findByHostId(hostId)` scopes results. |
| Inputs | No pagination inputs. |
| Output | Complete `PropertyResponse[]`, ordered by `createdAt DESC, id ASC`. |
| Errors | Shared error matrix; no per-item ownership errors. |
| Idempotency / transaction | Read-only, read-only transaction. |
| Events / audit | None. |
| Compatibility | Replaces fixture list; stable fixture IDs become backend UUIDs. |

## GET /api/host/listings/{listingId}; POST /api/host/listings; PUT /api/host/listings/{listingId}; PUT /api/host/listings/{listingId}/status

| Field | Definition |
|---|---|
| Caller and principal | Verified active HOST. |
| Authorization / tenancy | Resolve host UUID; detail/update/status require owned property; create assigns resolved UUID. No client `hostId` accepted. |
| Inputs | `CreateListingRequest`, `UpdateListingRequest`, `UpdateListingStatusRequest`; listing UUID; create/update validate existing DTO constraints plus description <= 5,000 chars and title <= 120 chars. |
| Output | Detail returns `HostListingDetailResponse` combining `PropertyResponse` and `ListingMetricsResponse`; writes return `PropertyResponse`. |
| Errors | Invalid fields/unknown JSON → 400 `VALIDATION_ERROR`; absent/non-owned UUID → 404 `NOT_FOUND`; valid payload rejected by domain rule → 422 `DOMAIN_RULE_VIOLATION`; unexpected → 500 `INTERNAL_ERROR`. |
| Idempotency / transaction | Create is non-idempotent. Update/status are replace operations; identical state returns current representation. Each mutation uses its existing service transaction and audit/event behavior. |
| Events / audit | Existing listing events and audit rows; controller does not write repositories or ledger directly. |
| Compatibility | Detail data includes booking count, average rating, reviews, occupancy, and earnings already shown by the UI. |

## GET /api/host/listings/{listingId}/calendar?year={year}&month={month}; POST /api/host/listings/{listingId}/blocks; DELETE /api/host/blocks/{blockId}

| Field | Definition |
|---|---|
| Caller and principal | Verified active HOST. |
| Authorization / tenancy | Listing and block must belong to resolved host; inaccessible resources return 404 `NOT_FOUND`. |
| Inputs | Calendar year 2000–2100 and month 1–12. Create uses `CreateHostBlockRequest`; API dates are inclusive. Reason is optional, trimmed, max 240 Unicode code points. |
| Output | `HostCalendarResponse`; month days are date-ascending, `booked` and `blocked` booleans, BOOKING state takes visual precedence, manual blocked reason is returned only for HOST_BLOCK; ranges are date/id ascending and inclusive. Delete returns 204. |
| Errors | Invalid month/date/UUID/body → 400 `VALIDATION_ERROR`; past/reversed range → 422 `DOMAIN_RULE_VIOLATION`; overlap with a booking (including a booking-sourced calendar block) → 409 `BOOKING_OVERLAP`; overlap with a prior host block → 409 `HOST_BLOCK_OVERLAP`; if both sources overlap, return `BOOKING_OVERLAP`. Both overlap errors use the shared safe body message `Request could not be completed`; the frontend selects explanatory copy from the code. Never expose conflicting IDs or dates. Absent/non-owned listing/block → 404 `NOT_FOUND`; unexpected → 500 `INTERNAL_ERROR`. |
| Idempotency / transaction | Create is non-idempotent; delete repeat returns 404. Create/remove use one SERIALIZABLE transaction; overlap check and write/delete are atomic. On SQLSTATE `40001`, retry the complete operation once in a fresh transaction; if the retry also serializes unsuccessfully, return 409 `STATE_CONFLICT`. A manual block mutation writes the required audit record. |
| Events / audit | Availability event publishes only after commit; audit row records create/remove with actor and inclusive range. |
| Compatibility | Persisted ranges are half-open; response and request convert to/from inclusive dates exactly once. |

## GET /api/host/requests?view=pending|history; POST /api/host/requests/{bookingId}/decision

| Field | Definition |
|---|---|
| Caller and principal | Verified active HOST. |
| Authorization / tenancy | Existing `BookingService` host-scoped queries; decision supplies resolved host UUID to `decide`. |
| Inputs | `view` exactly `pending` or `history`; no pagination inputs. A legacy `page` or `size` parameter is ignored. Decision uses `BookingDecisionRequest`; message <= 500 Unicode code points. |
| Output | Complete `HostBookingRowResponse[]`, ordered `createdAt DESC, id ASC`; decision returns `BookingResponse`. |
| Errors | Invalid view/body → 400 `VALIDATION_ERROR`; absent/non-owned booking → 404 `NOT_FOUND`; no longer pending/concurrent loser → 409 `STATE_CONFLICT`; domain rejection → 422 `DOMAIN_RULE_VIOLATION`; unexpected → 500. |
| Idempotency / transaction | Decision is one-shot. Service locks the booking row `PESSIMISTIC_WRITE`, checks PENDING after lock, and commits state, escrow/ledger, audit, and event intent atomically. Repeat returns 409. |
| Events / audit | Existing booking/wallet/audit behavior; events publish after successful commit. |
| Compatibility | Preserves pending/history presentation and deterministic newest-first ordering. |

## GET /api/host/messages/bookings; GET /api/host/messages/bookings/{bookingId}; POST /api/host/messages/bookings/{bookingId}; POST /api/host/messages/bookings/{bookingId}/read

| Field | Definition |
|---|---|
| Caller and principal | Verified active HOST. |
| Authorization / tenancy | `BookingConversationService` receives resolved host UUID and `Role.HOST`; it verifies booking participation. |
| Inputs | Summary and history routes have no pagination inputs; legacy `page` or `size` parameters are ignored. Booking UUID; `PostMessageRequest.body` 1–2,000 Unicode code points. |
| Output | Complete `BookingConversationSummaryResponse[]` ordered `lastMessageAt DESC, conversationId ASC`; history returns complete `MessageResponse[]` ordered `sentAt DESC, id DESC`; post returns `MessageResponse`; read returns 204. |
| Errors | Invalid body/path → 400; inaccessible booking → 404; closed posting window → 409 `STATE_CONFLICT`; unexpected → 500. |
| Idempotency / transaction | Posting is non-idempotent; read marking is idempotent. Insert/read is transactional; message event publishes after commit. |
| Events / audit | Existing booking-message event, after commit. |
| Compatibility | Booking direct messages remain distinct from agent-managed support tickets. |

## GET /api/host/messages/tickets; GET /api/host/messages/tickets/{ticketId}?channel=HOST; POST /api/host/messages/tickets/{ticketId}?channel=HOST; POST /api/host/messages/tickets/{ticketId}/read?channel=HOST

| Field | Definition |
|---|---|
| Caller and principal | Verified active HOST. |
| Authorization / tenancy | `MessageService` receives resolved host UUID, `Role.HOST`, and required `ThreadChannel.HOST`; service enforces visibility. |
| Inputs | Summary and history routes have no pagination inputs; legacy `page` or `size` parameters are ignored. `channel` exactly `HOST`; ticket UUID; message body 1–2,000 Unicode code points. |
| Output | Complete `ConversationSummaryResponse[]` ordered `lastMessageAt DESC, conversationId ASC`; history returns complete `MessageResponse[]` ordered `sentAt DESC, id DESC`; post returns `MessageResponse`; read returns 204. |
| Errors | Invalid channel/body/path → 400; inaccessible ticket → 404; resolved ticket read-only → 409 `STATE_CONFLICT`; unexpected → 500. |
| Idempotency / transaction | Posting is non-idempotent; read marking is idempotent. Insert/read is transactional; event publishes after commit. |
| Events / audit | Existing ticket-message event, after commit. Host cannot create tickets. |
| Compatibility | Distinct route/type prevents booking IDs from being interpreted as ticket IDs. |

## GET /api/host/wallet; POST /api/host/wallet/top-up; POST /api/host/wallet/withdraw

| Field | Definition |
|---|---|
| Caller and principal | Verified active HOST. |
| Authorization / tenancy | Resolve host UUID; wallet service receives only that UUID. |
| Inputs | Read has no pagination inputs; legacy `page` or `size` parameters are ignored. Mutations accept `WalletAmountRequest.amount` from 0.01 to 100000.00 with at most two decimals; no idempotency header is accepted or required. |
| Output | Read returns `HostWalletResponse` with current balance and complete newest-first `TransactionResponse[]` statement. Mutation returns `HostWalletMutationResponse` with `mocked=true`, resulting wallet, and transaction. |
| Errors | Invalid amount → 400 `VALIDATION_ERROR`; insufficient funds → 422 `DOMAIN_RULE_VIOLATION`; missing wallet → 404 `NOT_FOUND`; unexpected → 500. |
| Idempotency / transaction | There is no request deduplication: each accepted HTTP mutation request creates a separate mocked wallet operation. Each request uses the existing wallet-service transaction, wallet row lock, ledger write, and audit write atomically. The client does not automatically retry; after an ambiguous failure it refreshes balance and statement before enabling a new explicit operation. |
| Events / audit | Existing ledger and audit write; event is dispatched only after commit. No payment provider call. |
| Compatibility | Preserves C75 mocked operations and never represents them as real payments. |
