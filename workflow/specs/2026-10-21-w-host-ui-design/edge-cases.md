# Edge Cases and Non-Happy Paths

IDs are permanent: never reuse or renumber. Every F requirement appears in at least one row.

## State and input

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E1 | F1, F2 | Host opens each supported route directly | The matching page renders inside the Host shell; an unknown listing id renders `Listing not found`; unsupported route paths do not expose fixture content | Direct links are normal in a web app | UI |
| E2 | F3, F10 | Fixture contains no listings | Listings page keeps `My listings` and `+ New listing`, then shows `You don't have any listings yet.` without a broken table | Empty ownership is valid | UI |
| E3 | F4, F5 | Listing fixture is missing or incomplete | Detail and edit both render only the exact `Listing not found` card with a `Back to Listings` control; the edit form is not rendered | Prevents false listing data and contradictory error states | UI |
| E4 | F6 | Calendar has no blocks or bookings | Grid renders all dates as Available/Other month and Current overrides says `No current overrides` | Empty calendar is common | UI |
| E5 | F6 | Calendar has overlapping visual categories or a date belongs to another month | A date displays one deterministic label with precedence Booked, then Blocked, then Available, then Other month; the accessible name includes the final state | Prevents ambiguous status presentation | unit/UI |
| E6 | F7 | Requests fixture has zero pending but has history | Pending section shows `No pending requests`; past table remains visible | Hosts may have history without new work | UI |
| E7 | F8, F10 | Messages or wallet have no records | The corresponding empty copy appears and no blank controls imply a successful operation | Empty user data must be legible | UI |
| E8 | F5, F6, F7, F8, F9 | User activates Save, Block, Approve, Reject, Send, Top up, or Withdraw | Save validates using the exact messages in `ui.md`; Block/Approve/Reject/Top up/Withdraw open a local modal with Cancel and confirmation controls, and either close without changing fixtures; Send appends only to the direct local thread and is disabled for the resolved ticket; no backend request, success toast, or transaction completion copy appears | Domain actions are outside W-HOST's independent slice | UI/network mock |

## Permission and auth

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E9 | N2 | Signed-out user opens a Host route | Existing `RouteGuard` redirects to `/login?returnTo=<encoded-path>` | Host data must not be exposed by frontend routing | UI |
| E10 | N2 | Authenticated Guest or Agent opens a Host route | Existing `RouteGuard` redirects to that profile's role route; Host fixture content is not rendered | Client routing must preserve role separation | UI |
| E11 | N2, N3 | Profile loading fails or is inactive | Existing W-AUTH retry/sign-out or inactive response renders without Host shell content | Avoids a shell around an unauthorized profile | UI |

## Visual and viewport

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E12 | N1 | A page uses a generic blue/gray utility class or an unapproved hard-coded color | Visual review fails; the page uses the Fall Light token instead | Exact styling is the acceptance criterion | visual review |
| E13 | N3 | Status is conveyed only by a color swatch | The state also has visible text and an accessible name | Supports color-blind users | UI/accessibility |
| E14 | N4 | Viewport is 768px wide | Cards stack or compress without horizontal page overflow; primary action remains reachable | Tablet layout is required | browser |
| E15 | N4 | Text is long or a listing has no image | Text wraps within its card and the thumbnail uses the defined fallback treatment without shifting controls outside the card | Fixture variation must not break layout | UI/browser |

## Isolation and failure

| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E16 | N5 | Backend is unavailable or W-DOMAIN changes its persistence approach | Host fixture pages still render from the adapter; no frontend source imports backend or desktop classes; fixture records remain explicitly synthetic | This is the independent work boundary | source/UI |
| E17 | F10 | Fixture adapter returns loading then error | Skeletons render first, then the exact page error copy; stale populated data is not silently shown as current | Makes failure states deterministic | UI |

| E18 | F5, F6, F9 | User enters malformed text, blank required fields, negative numeric values, a zero rate, a sub-cent rate, or an invalid date range | The exact validation message in `ui.md` appears beside the field or form summary; no local navigation or presentation mutation occurs | Prevents ambiguous fixture-only form behaviour | UI |
| E19 | F1, F4, F5, F6 | User opens an encoded, malformed, or unknown `:listingId` | The router resolves no fixture and renders only `Listing not found` with `Back to Listings`; no record is selected by partial matching | Prevents accidental fixture disclosure and unstable route behaviour | UI |

## Security boundary

The frontend route guard protects access to the Host UI but is not the authorization boundary. Because every fixture record is synthetic and unowned, exposing a bundled listing ID or wallet value does not expose production data. W-DOMAIN integration must replace the fixture adapter with authenticated, ownership-scoped API responses before real Host data is displayed.

## Security boundary

The frontend route guard protects access to the Host UI but is not the authorization boundary. Because every fixture record is synthetic and unowned, exposing a bundled listing ID or wallet value does not expose production data. W-DOMAIN integration must replace the fixture adapter with authenticated, ownership-scoped API responses before real Host data is displayed.
