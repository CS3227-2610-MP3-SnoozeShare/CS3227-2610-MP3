# UI

The visual contract remains the approved W-HOST Fall Light UI. `HostFixtureProvider` remains available to unit and visual tests; authenticated production routes use an API-backed provider with the same state shapes.

## Listings, detail, and new/edit listing

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Existing empty listings card | New listing remains available | Existing empty-state copy | HOST |
| Loading | Existing skeleton/card loading treatment | No mutation controls until data loads | Existing loading semantics | HOST |
| Error | Existing error card with retry | Retry reloads the API only | Existing error copy | HOST |
| Populated | API data mapped to existing listing cards/detail/form | Create, update, status, calendar, edit | Existing copy | HOST |

Navigation: `/host/listings` → `/host/listings/{id}`, `/new`, `/edit`; unknown IDs retain the not-found state.

## Calendar and requests

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Existing empty calendar/request state | Block or decision controls only where allowed | Existing copy | HOST |
| Loading | Existing page loading treatment | Disabled until loaded | Existing loading semantics | HOST |
| Error | Existing retryable error card | Retry | Existing error copy | HOST |
| Populated | API booked/blocked ranges and pending/history rows | Apply/clear block; approve/reject decision | Existing confirmation copy | HOST |

Navigation: listing detail calendar and Host Requests tab. Successful mutations refetch the affected page data; failed mutations leave the prior state unchanged.

## Messages and wallet

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Existing empty thread/statement state | No unsupported action | Existing copy | HOST |
| Loading | Existing loading treatment | Disabled composer/actions | Existing loading semantics | HOST |
| Error | Existing retryable error card | Retry | Existing error copy | HOST |
| Populated | API conversations, messages, balance, and statement | Send/read, mocked top-up/withdraw confirmation | Existing resolved/read-only and mocked-wallet copy | HOST |

Navigation: Host Messages and Wallet tabs. Resolved threads remain read-only; wallet controls never display real payment-success language.

## Authentication and API errors

An expired token invokes the existing auth recovery path. `401`, `403`, `404`, `409`, `422`, and `500` map to stable user-safe messages without exposing request internals. No UI control accepts or displays a client-selected host UUID.
