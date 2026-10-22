# API

## GET `/api/host/listings`

| Field | Definition |
|---|---|
| Caller and principal | Authenticated Firebase principal with active `HOST` role. |
| Authorization rule | Existing role filter and `HostIdentityResolver` resolve the host; `ListingService.findByHostId` selects owned properties. |
| Tenancy scope | Only the authenticated Host's properties and metrics computed for those returned property IDs. No caller-supplied owner ID is accepted. |
| Inputs | None. The endpoint continues to ignore legacy `page` and `size` query parameters under the approved complete-collection contract. |
| Outputs | Complete ordered array. Each row contains all existing property fields plus integer `bookingCount` and numeric `averageRating`; no reviews, guest identities, or other Host data are added. No-review average is `0.0`. |
| Errors | Existing stable Host API errors remain unchanged; there are no new client-validation errors. |
| Idempotency | Read-only; repeated requests return the current persisted listing and metric snapshot. |
| Transaction boundary | Read-only service/JDBC operations; no writes. |
| Events / audit | None. |
| Logging | Existing request-boundary logging only; do not log listing contents or guest data. |
| Compatibility | Add summary fields to each array element; keep the complete collection ordering and all existing property fields. The Host frontend consumes the two new fields. Detail/create/update/status response shapes do not change. |

Metrics are computed for a non-empty collection in no more than two aggregate SQL queries: bookings grouped by `listing_id`, and reviews joined to bookings and grouped by `listing_id`. Both queries join `properties` and filter by the `hostId` derived from the authenticated principal; they do not accept an array of listing IDs or caller-supplied ownership values. They do not issue an HTTP request or a SQL metrics query per property. For an empty collection, both aggregate queries are skipped and the endpoint returns `[]`.

## PUT `/api/host/listings/{listingId}/status`

No API contract change. The authenticated Host receives the existing `PropertyResponse`, including the authoritative new status but no summary metrics. Ownership is checked before mutation. The frontend patches only the returned `status` onto the existing collection row, preserving its `bookingCount` and `averageRating`; it must not remap the mutation response as a complete collection row.
