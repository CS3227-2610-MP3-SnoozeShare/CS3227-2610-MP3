# Data Model

No property, booking, message, wallet, or user columns or tables change. Wallet operations use the existing wallet, audit, and ledger records. Each accepted request produces its own mocked operation; this workstream does not persist request keys or deduplicate retries (SD12).

## API response value shapes

- `HostListingDetailResponse`: `listing: PropertyResponse`, `metrics: ListingMetricsResponse`.
- `HostCalendarResponse`: `listingId`, `year`, `month`, ordered `days[]` (`date`, `booked`, `blocked`, `blockReason`), and ordered `blockedRanges[]` (`id`, `reason`, `startDate`, `endDate`). Wire dates are ISO local dates and inclusive.
- `HostWalletResponse`: `wallet: WalletResponse`, `statement: TransactionResponse[]`, `statementPage`, `statementSize`, `statementHasMore`; statement rows are selected directly from the ledger with a bounded database query.
- `HostWalletMutationResponse`: `mocked: true`, `wallet: WalletResponse`, `transaction: TransactionResponse`.
- `PageResponse<T>`: `items: T[]`, zero-based `page`, requested `size`, and `hasMore`; database reads fetch at most `size + 1` rows and never materialize the full collection.
- Message summaries carry `kind: BOOKING | TICKET`; booking and ticket messages retain distinct IDs and routes. Summary projections are queried page-wise from their respective stores; message history is queried page-wise by conversation/ticket ID and owner.
