# API — Wallet, Review and DTOs

See [api-search-booking.md](api-search-booking.md) for common error responses, auth rules, and the resolved-user-ID mechanism (SD7).

## GuestWalletController

### GET /api/guest/wallet

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Returns only the authenticated guest's wallet |
| Inputs | None |
| Outputs | `200 OK` — `WalletResponse`: `id`, `userId`, `balance`, `currency`, `updatedAt` |
| Errors | Wallet not found → 404 `WALLET_NOT_FOUND` (should not occur; created at registration) |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log user ID at DEBUG; never log balance |
| Compatibility | New endpoint |

### GET /api/guest/wallet/transactions

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Returns only transactions for the authenticated guest's wallet |
| Inputs | None |
| Outputs | `200 OK` — `TransactionResponse[]` ordered by timestamp descending |
| Errors | None specific |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log user ID at DEBUG |
| Compatibility | New endpoint |

### POST /api/guest/wallet/topup

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Credits only the authenticated guest's wallet |
| Inputs | Body — `WalletAmountRequest`: `amount` BigDecimal > 0 (required, `@NotNull @Positive`) |
| Outputs | `200 OK` — `TransactionResponse` for the top-up |
| Errors | Amount ≤ 0 → 400 `VALIDATION_ERROR`; amount null → 400 `VALIDATION_ERROR` |
| Idempotency | Not idempotent; each call adds to the balance |
| Transaction boundary | `WalletService.topUp` in a transaction |
| Events / audit | Audit row `WALLET_TOPUP`; wallet event after commit |
| Logging | Log user ID at INFO; never log the amount |
| Compatibility | New endpoint |

### POST /api/guest/wallet/withdraw

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Debits only the authenticated guest's wallet |
| Inputs | Body — `WalletAmountRequest`: `amount` BigDecimal > 0 (required, `@NotNull @Positive`) |
| Outputs | `200 OK` — `TransactionResponse` for the withdrawal |
| Errors | Amount ≤ 0 → 400 `VALIDATION_ERROR`; amount > balance → 400 `INSUFFICIENT_BALANCE` "Insufficient wallet balance" |
| Idempotency | Not idempotent |
| Transaction boundary | `WalletService.withdraw` in a transaction |
| Events / audit | Audit row `WALLET_WITHDRAWAL`; wallet event after commit |
| Logging | Log user ID at INFO; never log the amount |
| Compatibility | New endpoint |

---

## GuestReviewController

### POST /api/guest/bookings/{id}/review

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; booking must belong to the authenticated guest |
| Tenancy scope | Creates a review authored by the authenticated guest for their booking |
| Inputs | Path — `id`: UUID (booking ID). Body — `SubmitReviewRequest`: `rating` int 1-5 (`@Min(1) @Max(5)`), `comment` String (required, not blank, max 2000 chars) |
| Outputs | `201 Created` — `ReviewResponse` |
| Errors | Booking not found → 404 `BOOKING_NOT_FOUND`; booking belongs to another guest → 403 `FORBIDDEN`; booking not COMPLETED → 400 `NOT_REVIEWABLE` "Only completed bookings can be reviewed"; review already exists → 409 `DUPLICATE_REVIEW` "A review has already been submitted for this booking" |
| Idempotency | Not idempotent; duplicate blocked by service |
| Transaction boundary | `ReviewService.submit` in a transaction |
| Events / audit | Audit row for review; no event published |
| Logging | Log booking ID at INFO |
| Compatibility | New endpoint |

---

## New DTOs

### GuestTripResponse (response)

```
record GuestTripResponse(
    BookingResponse booking,
    String listingTitle,
    String hostDisplayName,
    boolean canCancel,
    boolean canFileDispute,
    boolean hasReview
)
```

Computed by `GuestBookingController` from `BookingService.tripsFor`, `ListingService.getDetail`, `UserService.findById`, `BookingService.previewCancellationRefund` (for cancellability), `DisputeWindowPolicy` + `TicketService.myTickets` (for dispute eligibility), and `ReviewService.hasReview`.

### PropertyDetailResponse (response)

```
record PropertyDetailResponse(
    PropertyResponse property,
    String hostDisplayName,
    int hostingSinceYear,
    List<ListingReviewResponse> reviews,
    double averageRating,
    int reviewCount
)
```

Uses the existing `ListingMetricsResponse.ListingReviewResponse` (which has `guestName`, `rating`, `comment`) instead of `ReviewResponse` (which has `id`, `bookingId`, `guestId` but no guest name). Computed by `GuestSearchController` from `ListingService.getDetail`, `UserService.findById`, and `ListingMetricsService.metricsFor`.

### GuestConversationListResponse (response)

```
record GuestConversationListResponse(
    List<ConversationSummaryResponse> ticketConversations,
    List<BookingConversationSummaryResponse> bookingConversations
)
```

Returned by the conversations endpoint; the frontend merges and sorts them by last-message timestamp.

## Security summary (High-tier)

**Threat model:**

| Actor | Abuse | Mitigation |
|---|---|---|
| Authenticated guest tries to access another guest's booking/wallet/ticket | Calls endpoint with another user's resource UUID | Every mutation and read resolves the guest ID from the bearer token, not from the request; the controller verifies `booking.guestId == authenticatedUserId` (or equivalent) before proceeding; mismatch returns 403 or 404 |
| Authenticated HOST or AGENT calls `/api/guest/**` | Role escalation / cross-role access | `RoleAuthorizationFilter` rejects non-GUEST roles with 403 before the controller runs |
| Unauthenticated caller | Missing or forged token | `FirebaseAuthFilter` rejects with 401 |
| Guest submits booking to exhaust another user's escrow or block dates | Rapid repeated booking requests | The SERIALIZABLE transaction prevents double-booking; escrow debits only the guest's own wallet; rate limiting is process-local (Known Gap) |
| Guest manipulates `guestId` in the request body | Tries to create a booking or ticket on behalf of another user | Controllers never read a user ID from the request body; the guest ID always comes from the authenticated principal |
| Guest enumerates listings or bookings by ID | UUID guessing | Listings are public data for authenticated guests; bookings/tickets/messages return 403 if the guest ID does not match |
