# Data Model

No property, booking, message, wallet, or user columns change. One table is added to make wallet retries safe across lost responses and process restarts.

## Wallet operation idempotency

| Attribute | Type | Null/default/range | Rule |
|---|---|---|---|
| id | UUID | not null | Primary key. |
| user_id | UUID | not null | FK to `users.id`; owner is derived from the verified principal. |
| operation | VARCHAR(16) | not null | `TOP_UP` or `WITHDRAWAL`. |
| idempotency_key | VARCHAR(128) | not null | Opaque client-generated key; unique per user across all wallet operations. |
| request_hash | CHAR(64) | not null | SHA-256 of canonical operation + amount representation. |
| transaction_id | UUID | not null | FK to `audit_log.id`; the ledger row produced by the first successful operation. |
| created_at | TIMESTAMPTZ | not null | UTC operation time. |

Table name: `wallet_operation_idempotency`. Unique key: `(user_id, idempotency_key)`. Lookup is by this pair before applying any wallet operation; compare the stored hash, which includes operation and canonical two-decimal amount. A repeat with the same hash returns the original ledger transaction. Reusing the key with a different operation or amount returns 409 `IDEMPOTENCY_KEY_REUSED`. Concurrent first-use requests are resolved by the unique constraint: the loser reloads the committed row and returns its original result or the mismatch error. The idempotency row and ledger mutation commit in one database transaction. Validation or insufficient-funds failures do not reserve the key. Retain rows for the lifetime of the associated ledger entry; no expiry cleanup is permitted in this phase.

Migration: add the next Flyway migration after the current W-DOMAIN migration. Existing rows need no backfill. Rollback is to remove the new table after the application version using it is rolled back; no wallet or audit rows are deleted. If migration fails, Flyway transaction rollback leaves the table absent. The committed mock DB is backend Postgres only; no SQLite reference DB rebuild applies.

## API response value shapes

- `HostListingDetailResponse`: `listing: PropertyResponse`, `metrics: ListingMetricsResponse`.
- `HostCalendarResponse`: `listingId`, `year`, `month`, ordered `days[]` (`date`, `booked`, `blocked`, `blockReason`), and ordered `blockedRanges[]` (`id`, `reason`, `startDate`, `endDate`). Wire dates are ISO local dates and inclusive.
- `HostWalletResponse`: `wallet: WalletResponse`, `statement: TransactionResponse[]`, `statementPage`, `statementSize`, `statementHasMore`; statement rows are selected directly from the ledger with a bounded database query.
- `HostWalletMutationResponse`: `mocked: true`, `wallet: WalletResponse`, `transaction: TransactionResponse`, `replayed: boolean`.
- `PageResponse<T>`: `items: T[]`, zero-based `page`, requested `size`, and `hasMore`; database reads fetch at most `size + 1` rows and never materialize the full collection.
- Message summaries carry `kind: BOOKING | TICKET`; booking and ticket messages retain distinct IDs and routes. Summary projections are queried page-wise from their respective stores; message history is queried page-wise by conversation/ticket ID and owner.
