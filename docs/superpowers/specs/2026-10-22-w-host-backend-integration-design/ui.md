# UI

The visual contract remains the approved W-HOST Fall Light UI. `HostFixtureProvider` remains available to unit and visual tests; authenticated production routes use an API-backed provider with the same state shapes. Each Host collection/history data set loads in one request with all matching rows; clients send no `page` or `size` parameters and render no pagination or load-more controls. Existing internal scroll regions remain. The navbar wallet badge and Wallet screen share the `/api/host/wallet` response, which always includes the complete statement; opening Wallet adds no second wallet request. Other Host routes that load the wallet badge also receive the complete statement, an accepted consequence of C~19.

## Listings, detail, and new/edit listing

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Existing empty listings card | New listing remains available | Existing empty-state copy | HOST |
| Loading | Existing skeleton/card loading treatment | No mutation controls until data loads | Existing loading semantics | HOST |
| Error | Existing error card with retry | Retry reloads the API only | Existing error copy | HOST |
| Populated | API data mapped to existing listing cards/detail/form | Create, update, status, calendar, edit | Existing copy | HOST |

Navigation: `/host/listings` → `/host/listings/{id}`, `/new`, `/edit`; the listings route renders all returned rows in its existing scroll region. Unknown IDs retain the not-found state.

## Calendar and requests

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Existing empty calendar/request state | Block or decision controls only where allowed | Existing copy | HOST |
| Loading | Existing page loading treatment | Disabled until loaded | Existing loading semantics | HOST |
| Error | Existing retryable error card | Retry | Existing error copy | HOST |
| Populated | API booked/blocked ranges and every pending/history row | Apply/clear block; approve/reject decision | Existing confirmation copy | HOST |

Navigation: listing detail calendar and Host Requests tab. Successful mutations refetch the affected complete collection; failed mutations leave prior state unchanged. Existing table scroll regions remain. Calendar requests the selected month and constructs its six-week client grid from the inclusive API date records.

## Messages and wallet

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Existing empty thread/statement state | No unsupported action | Existing copy | HOST |
| Loading | Existing loading treatment | Disabled composer/actions | Existing loading semantics | HOST |
| Error | Existing retryable error card | Retry | Existing error copy | HOST |
| Populated | All API booking conversations and ticket threads, selected thread's complete message history, wallet balance, and full statement | Send/read, mocked top-up/withdraw confirmation | Existing resolved/read-only and mocked-wallet copy | HOST |

Navigation: Host Messages and Wallet tabs. Booking messages and ticket messages are separate API resources; selecting a thread loads its entire history and there is no load-older control. The UI preserves current thread context and Host cannot start a support ticket. Resolved threads remain read-only; wallet controls disable while a request is in flight and never display real payment-success language. Wallet statements render all returned rows in the existing scroll region. Wallet mutations are not automatically retried or deduplicated. After an ambiguous failure, refresh balance and full statement before enabling another explicit operation; a new confirmation is a new wallet operation.

## Authentication and API errors

An expired token invokes the existing auth recovery path. `401`, `403`, `404`, `409`, `422`, and `500` map to stable user-safe messages without exposing request internals. No UI control accepts or displays a client-selected host UUID.
