# Edge Cases and Non-Happy Paths

## Data scope and collection metrics

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E1 | F1, N1, N2 | A Host has a listing with no reviews, a listing with bookings/reviews, and another Host has unrelated data. | The first Host sees only owned rows; empty rating is `0.0`; other metrics equal persisted counts/average; no foreign row or metric appears. | Prevents fabricated zeros and cross-Host disclosure. | integration |
| E2 | N1 | Collection contains 1 listing and then 101 listings. | Exactly one frontend collection GET; at most two aggregate metrics SQL queries in either case. | Prevents per-listing network/database work while preserving complete collections. | integration / UI |
| E9 | F1, N1 | A Host owns no listings. | The endpoint returns `[]` and skips both aggregate metrics queries. | Avoids unnecessary database work and preserves the complete empty collection contract. | integration |

## Mutation and display

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E3 | F2 | Status update succeeds or fails. | Success patches only the target card from the returned status and does not refetch listings; failure preserves the old status and shows the safe inline error without replacing the page. | Avoids screen-wide refresh and stale/optimistic status. | UI |
| E4 | F3 | API returns `07:00:00` and `03:00:00`; run the regression under UTC+8 and UTC-8 timezone settings. | In both timezone settings, detail shows `07:00` and `03:00`; edit inputs show the same strings. | Prevents time-only values from shifting as if they were instants in either direction. | unit / UI |
| E5 | F4 | Occupancy is zero, fractional, or 100. | Display is exactly `0.00%`, `12.50%`, or `100.00%`. | Stable readable precision. | UI |

## Calendar state and time

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E6 | F5 | Navigate to a month not in the provider cache while calendar GET is pending, with an applied date range and reason; optionally leave un-applied picker edits open. | Breadcrumb/page frame, applied range, and reason stay unchanged; requested month title updates immediately; grid alone shows loading; on response the grid and month-specific blocked-date list update. If picker edits are open, the outside click closes the picker and discards only those unapplied edits under the existing Host date-picker behavior. | Avoids a route-wide refresh or silently resetting form state while keeping month data authoritative. | UI |
| E7 | F5 | A month GET returns 404 or another error. | Existing generic route/load error with a Back to Listings link appears; no stale calendar is presented as the requested month. | Keeps failure behavior consistent with the Host route/load error contract. | UI |
| E8 | F6 | Open a new calendar in fixture mode and API mode. | Range values are empty, trigger says `Date range: Select dates`, no date is selected, and picker opens to the visible month. | Prevents synthetic dates from being mistaken for a real block selection. | UI |
