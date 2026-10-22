# Data Model

No property, booking, message, wallet, or user columns or tables change. Wallet operations use the existing wallet, audit, and ledger records. Each accepted request produces its own mocked operation; this workstream does not persist request keys or deduplicate retries (SD12).

## API response value shapes

- `HostListingDetailResponse`: `listing: PropertyResponse`, `metrics: ListingMetricsResponse`.
- `HostCalendarResponse`: `listingId`, `year`, `month`, ordered `days[]` (`date`, `booked`, `blocked`, `blockReason`), and ordered `blockedRanges[]` (`id`, `reason`, `startDate`, `endDate`). Wire dates are ISO local dates and inclusive.
- `HostWalletResponse`: `wallet: WalletResponse`, `statement: TransactionResponse[]`; the statement contains every transaction for the Host wallet in `timestamp DESC, id DESC` order.
- `HostWalletMutationResponse`: `mocked: true`, `wallet: WalletResponse`, `transaction: TransactionResponse`.
- Host collection and history response bodies are `T[]` arrays; `HostWalletResponse.statement` is likewise an array. These responses carry no page metadata or row-count cap.
- Message summaries carry `kind: BOOKING | TICKET`; booking and ticket messages retain distinct IDs and routes. Summary and history reads return every matching owner-scoped row from their respective stores in the ordering defined by `api.md`.

Collection responses may grow without a defined maximum; this is the accepted API/runtime limitation recorded by C~19. Host ownership and conversation scope remain mandatory.
