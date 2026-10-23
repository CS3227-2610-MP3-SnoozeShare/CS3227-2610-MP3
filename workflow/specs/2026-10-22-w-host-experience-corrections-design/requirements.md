# Requirements

Ordered so that a requirement comes after anything it depends on.

## Functional

| ID | Requirement | Acceptance criterion |
|---|---|---|
| F1 | Each Host listing collection row shall include its total booking count and average review rating. | Given an owned listing with bookings and reviews, when its Host loads the collection, then the row has the matching count and arithmetic average; listings without reviews return `0.0`. |
| F2 | A successful availability toggle shall update only that listing card from the server response without reloading the collection. | Given the collection is loaded, when a toggle succeeds, then its switch and status label update, the rest of the page remains rendered, and no second collection GET occurs. |
| F3 | Listing detail shall display check-in and check-out in the exact saved 24-hour `HH:mm` value. | Given API times `07:00:00` and `03:00:00`, when the detail page renders in any browser timezone, then it displays `07:00` and `03:00`; the edit form still opens with those values. |
| F4 | Listing detail shall display occupancy with exactly two fractional digits. | Given occupancy values `0`, `12.5`, and `100`, when detail renders, then it shows `0.00%`, `12.50%`, and `100.00%`. |
| F5 | Calendar month navigation shall update only the selected-month calendar data while preserving unrelated block-form state. | Given a rendered calendar, when previous/next month is selected, then the page frame and block-date controls remain mounted, the month label changes immediately, only the grid shows loading, the applied date range and reason remain unchanged, and after success the month-specific grid and blocked-date list update. An open date-picker draft follows the existing outside-click behavior: close and discard unapplied draft edits. |
| F6 | The block-date range shall have no selected start or end date on initial display in API and fixture modes. | Given a newly opened calendar page, when the Block dates panel renders, then its field says `Select dates`, no dates are highlighted, and the picker opens to the visible calendar month. |

## Non-functional

| ID | Category | Requirement | Acceptance criterion |
|---|---|---|---|
| N1 | Performance | Listing metrics for a complete collection shall be fetched in batches, not through per-listing HTTP or SQL queries. | Given collections of 1 and 101 owned listings, the frontend makes one collection GET and the backend performs no more than two aggregate metrics queries, independent of collection size. |
| N2 | Security | Listing summaries and their metrics shall remain scoped to the authenticated Host's owned listing IDs. | Given two Hosts with distinct listings, when either requests the collection, then no listing or metric from the other Host appears; existing authorization and ownership tests remain green. |
