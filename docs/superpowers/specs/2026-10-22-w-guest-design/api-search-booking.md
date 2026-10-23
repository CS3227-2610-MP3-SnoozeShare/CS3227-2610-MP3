# API — Search and Booking

5 controllers in `com.snoozeshare.controller.guest`. All require a valid Firebase bearer token and the GUEST role (enforced by `FirebaseAuthFilter` + `RoleAuthorizationFilter`). The authenticated guest's database UUID is propagated by the filter as request attribute `snoozeshare.userId` (SD7); controllers read it via a shared helper and pass it to service calls.

Common error responses (apply to every endpoint unless noted). Shape: `{ "code": "<CODE>", "message": "<human-readable>", "requestId": "<uuid>" }` matching the existing `AuthErrorResponse` pattern (SD9, C96). A `@RestControllerAdvice` exception handler maps service exceptions to this shape.

| Condition | Status | Code |
|---|---|---|
| Missing or invalid bearer token | 401 | `INVALID_TOKEN` |
| No profile registered for Firebase UID | 404 | `PROFILE_NOT_REGISTERED` |
| User account suspended | 403 | `PROFILE_INACTIVE` |
| Role mismatch (not GUEST) | 403 | `ROLE_MISMATCH` |
| Request body fails Jakarta validation | 400 | `VALIDATION_ERROR` with field details |

---

## GuestSearchController

### GET /api/guest/listings

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; RoleAuthorizationFilter on `/api/guest/**` |
| Tenancy scope | Public data: returns all ACTIVE listings regardless of owner; does not expose host contact or financial data |
| Inputs | Query params — `city`: String (optional), `startDate`: LocalDate ISO (optional), `endDate`: LocalDate ISO (optional), `guests`: Integer ≥ 1 (optional), `maxNightlyRate`: BigDecimal > 0 (optional). All optional; omitted = no filter |
| Outputs | `200 OK` — `SearchResultResponse[]`: each contains `PropertyResponse` + `boolean available` (true if available for the date range, or true if no dates given) |
| Errors | `startDate` in the past or `endDate` ≤ `startDate` → 400 `INVALID_DATE_RANGE` "Check-out must be after check-in and dates must not be in the past"; `guests` < 1 → 400 `VALIDATION_ERROR`; `maxNightlyRate` ≤ 0 → 400 `VALIDATION_ERROR` |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only, no transaction needed |
| Events / audit | None |
| Logging | Log search criteria at DEBUG; never log the bearer token |
| Compatibility | New endpoint; no existing callers |

### GET /api/guest/listings/{id}

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Public listing data; host identity limited to display name and created-at year |
| Inputs | Path — `id`: UUID (property ID) |
| Outputs | `200 OK` — `PropertyDetailResponse` (new): `PropertyResponse` fields + `hostDisplayName`: String + `hostingSinceYear`: int + `reviews`: `ListingReviewResponse[]` (guestName, rating, comment) + `averageRating`: double + `reviewCount`: int |
| Errors | Property not found or status not ACTIVE → 404 `LISTING_NOT_FOUND` "Listing not found" (the controller calls `ListingService.getDetail` then checks active status — SD8: visibility filtering is a controller concern; do not reveal existence of inactive listings to guests) |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log property ID at DEBUG |
| Compatibility | New endpoint |

### GET /api/guest/listings/{id}/estimate

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Public pricing data |
| Inputs | Path — `id`: UUID. Query — `startDate`: LocalDate ISO (required), `endDate`: LocalDate ISO (required) |
| Outputs | `200 OK` — `PriceBreakdownResponse`: `nightlyRate`, `nights`, `totalAmount` |
| Errors | Property not found or inactive → 404 `LISTING_NOT_FOUND` (active-status check in controller, SD8); missing dates → 400 `VALIDATION_ERROR`; `endDate` ≤ `startDate` → 400 `INVALID_DATE_RANGE`; `startDate` in the past → 400 `INVALID_DATE_RANGE` (controller validates dates before calling the service, since `ListingServiceImpl.estimateCost` does not call `StayDatePolicy`) |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log property ID and dates at DEBUG |
| Compatibility | New endpoint |

---

## GuestBookingController

### POST /api/guest/bookings

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; the guest ID is taken from the authenticated principal, not from the request body |
| Tenancy scope | Creates a booking owned by the authenticated guest; the escrow hold debits the guest's wallet only |
| Inputs | Body — `CreateBookingRequest`: `propertyId` UUID (required), `startDate` LocalDate (required), `endDate` LocalDate (required) |
| Outputs | `201 Created` — `BookingResponse` |
| Errors | Property not found → 404 `LISTING_NOT_FOUND`; property inactive → 400 `LISTING_INACTIVE` "Listing is not active"; dates unavailable → 409 `DATES_UNAVAILABLE` "The selected dates are not available"; `endDate` ≤ `startDate` → 400 `INVALID_DATE_RANGE`; `startDate` in the past → 400 `INVALID_DATE_RANGE`; insufficient wallet balance for escrow → 400 `INSUFFICIENT_BALANCE` "Insufficient wallet balance"; guest is the property host → 400 `SELF_BOOKING` "Cannot book your own listing" (SD10: enforced in `BookingServiceImpl`) |
| Idempotency | Not idempotent; each call creates a new booking |
| Transaction boundary | `BookingService.submitRequest` runs in a SERIALIZABLE transaction (overlap check + escrow hold) |
| Events / audit | Audit row for `BOOKING_CREATED` + `ESCROW_HOLD` via services |
| Logging | Log booking ID and property ID at INFO; never log financial amounts |
| Compatibility | New endpoint |

### GET /api/guest/bookings

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Returns only bookings where `guestId` matches the authenticated user; no access to other guests' bookings |
| Inputs | Query — `filter`: String enum `UPCOMING`, `ACTIVE`, `PAST` (optional; omitted returns all) |
| Outputs | `200 OK` — `GuestTripResponse[]` (new DTO): `booking` (`BookingResponse`), `listingTitle` (String), `hostDisplayName` (String), `canCancel` (boolean), `canFileDispute` (boolean), `hasReview` (boolean). The controller calls `BookingService.tripsFor(guestId, null)` and groups by date (SD8): UPCOMING = PENDING or CONFIRMED with `startDate` > today; ACTIVE = CONFIRMED with `startDate` ≤ today ≤ `endDate`; PAST = COMPLETED, FORCE_COMPLETED, FORCE_CANCELLED, REJECTED, CANCELLED_BY_GUEST, CANCELLED_BY_HOST, or CONFIRMED with `endDate` < today. Sorted: UPCOMING by `startDate` asc, ACTIVE by `startDate` asc, PAST by `endDate` desc |
| Errors | Invalid filter value → 400 `VALIDATION_ERROR` |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log guest ID at DEBUG |
| Compatibility | New endpoint |

### GET /api/guest/bookings/{id}/cancel-preview

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; booking must belong to the authenticated guest |
| Tenancy scope | Returns refund data for the authenticated guest's booking only |
| Inputs | Path — `id`: UUID (booking ID) |
| Outputs | `200 OK` — `CancellationRefundResponse`: `amount` (BigDecimal), `percent` (int: 100 or 50). The controller fetches the booking, verifies `booking.guestId == authenticatedUserId` (SD8: ownership is a controller authorization check), then calls `previewCancellationRefund` |
| Errors | Booking not found → 404 `BOOKING_NOT_FOUND`; booking belongs to another guest → 403 `FORBIDDEN`; booking not in a cancellable state (not PENDING or CONFIRMED-before-check-in) → 400 `NOT_CANCELLABLE` "This booking cannot be cancelled" |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log booking ID at DEBUG |
| Compatibility | New endpoint |

### POST /api/guest/bookings/{id}/cancel

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; booking must belong to the authenticated guest |
| Tenancy scope | Cancels and refunds only the authenticated guest's booking |
| Inputs | Path — `id`: UUID (booking ID). No body |
| Outputs | `200 OK` — `BookingResponse` (with updated status CANCELLED_BY_GUEST) |
| Errors | Booking not found → 404 `BOOKING_NOT_FOUND`; booking belongs to another guest → 403 `FORBIDDEN`; booking not cancellable → 400 `NOT_CANCELLABLE` |
| Idempotency | Calling cancel on an already-cancelled booking returns 400 `NOT_CANCELLABLE` (not silently successful) |
| Transaction boundary | `BookingService.cancel` runs in a transaction (state change + escrow refund) |
| Events / audit | Audit row for `BOOKING_CANCELLED` + `ESCROW_REFUND`; `BookingCancelledEvent` after commit |
| Logging | Log booking ID at INFO |
| Compatibility | New endpoint |
