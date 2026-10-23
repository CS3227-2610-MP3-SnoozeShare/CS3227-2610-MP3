# UI

## Host listings (`/host/listings`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Existing empty-list message. | New listing link. | Unchanged. | Host |
| Loading | Existing collection loading placeholders only on initial load. | None on placeholder rows. | Unchanged. | Host |
| Error | Existing generic listing-load error page and retry. | Back to Listings; Try again. | Unchanged. | Host |
| Populated | Each card shows server booking count and average rating. During a status mutation, only its switch is disabled; after success only that card's status changes. | Other listing cards remain usable; status switch behavior unchanged. | Existing safe inline mutation error on failure. | Host |

Navigation: existing listing detail, calendar, edit, and new-listing navigation remains unchanged.

## Host listing detail (`/host/listings/{listingId}`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Populated | Check-in/out exactly as `HH:mm`; occupancy as `0.00%`-style percentage. | Existing Edit listing and Open Booking Calendar links. | Existing labels. | Host |

Navigation: unchanged.

## Host booking calendar (`/host/listings/{listingId}/calendar`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Initial loading | Existing route-level loading state until the first listing and month are available. | None in the loading placeholder. | Unchanged. | Host |
| Month loading | Breadcrumbs, Block dates controls, applied date range, reason, and page frame remain visible; only the 42-cell calendar grid area shows a loading indicator. The month-specific blocked-date list updates after the response arrives. | Previous/Next month remain enabled only when no month request is in flight. An open date-picker closes and discards only unapplied draft edits on outside click, preserving the existing Host date-picker behavior. | Month label names the requested month. | Host |
| Month error | Existing generic route/load error and return-to-listings link. | Existing return link. | Unchanged. | Host |
| Populated | Selected month grid; date-range field is empty on first display. | Existing month navigation, date-range picker, reason, block and remove actions. | Empty range says `Select dates`. Picker opens on the visible month. | Host |

Navigation: month changes stay on the same route; listing and return navigation remain unchanged.
