# W-HOST Experience Corrections Design

- **Status:** Approved (Trevor, 2026-10-22)
- **Risk tier:** Medium - changes an authenticated owner-scoped listing response and corrects existing Host UI state transitions.
- **Workstream:** W-HOST | **Plan:** pending Gate 2
- **Constraints consulted:** W-HOST branch state § Architecture, § Decisions, § Known Gaps; `PROJECT_STATE.md` § Architecture, § Conventions, § Known Gaps
- **Reviewed by lenses:** Implementer, Tester, Attacker — two rounds complete; no blocking findings

## 1. Problem

The authenticated listing collection currently returns only `PropertyResponse`; `mapHostListing` hardcodes booking count and rating to zero, even though the detail endpoint computes those values. Toggling availability reloads the entire collection and replaces the page with skeletons. Listing detail formats a time-only value by constructing a local `Date` and formatting it in UTC, shifting the displayed time by the browser offset (for example, 07:00 becomes 23:00 the prior day in Singapore). Occupancy is rendered without fixed precision. Calendar month navigation marks the whole route loading, replacing the calendar and block-date panel, and the date-range control starts with sample dates.

The Host experience must show real owner-scoped card metrics; update a single card after a successful status mutation; display saved times unchanged as `HH:mm`; show occupancy with two decimal places; keep the calendar page and date-blocking panel mounted while only the calendar grid loads the selected month; and begin with no selected block range.

## 2. Goals

See [requirements](2026-10-22-w-host-experience-corrections-design/requirements.md) for testable functional and non-functional criteria.

## 3. Non-goals

- Change authentication, Host identity resolution, authorization rules, or other-role APIs; existing boundaries remain authoritative.
- Reintroduce pagination or cap complete collection responses; retain the approved complete-collection contract.
- Change the month-bounded calendar API, block-write semantics, or existing generic route/load error behavior.
- Change listing form time-entry controls or the underlying `LocalTime` model; correct display must preserve the submitted value.
- Remove fixtures; fixture mode remains supported for presentation tests.

## 4. Components

- [Requirements](2026-10-22-w-host-experience-corrections-design/requirements.md) - six observable Host behavior requirements and two non-functional constraints.
- [API](2026-10-22-w-host-experience-corrections-design/api.md) - owner-scoped listing summary response and unchanged route security.
- [UI](2026-10-22-w-host-experience-corrections-design/ui.md) - listing, detail, and calendar states and interactions.
- [Conventions](2026-10-22-w-host-experience-corrections-design/conventions.md) - exact time, percentage, and resource-update rules.
- [Edge cases](2026-10-22-w-host-experience-corrections-design/edge-cases.md) - owner isolation, failures, timezone, empty state, and month transitions.
- [Implementation order](2026-10-22-w-host-experience-corrections-design/implementation-order.md) - testable task sequence and dependencies.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Extend each listing-collection row with booking count and average rating, computed in batched owner-scoped metrics queries. | Uses the existing listing collection and metrics source; avoids one detail request per listing and avoids per-listing database queries. Rejected client-side detail fetches and N+1 summary queries. | Project owner approved correcting the all-zero listing metrics. |
| SD2 | Use the status-mutation response to update only the corresponding listing resource; do not refetch the whole collection. | Keeps the server response authoritative without replacing the page or adding a request. On failure, preserve the prior status and show the existing safe inline error. | Project owner approved avoiding a full-screen refresh on status changes. |
| SD3 | Render API check-in/out strings as their exact `HH:mm` value, without constructing a date or converting time zones. | A time-only value is not an instant; local parsing followed by UTC formatting caused the observed offset shift. | Project owner clarified the 07:00→23:00 and 03:00→19:00 examples and approved preserving the exact time. |
| SD4 | Render occupancy percentages with exactly two fractional digits. | Provides stable precision, including `0.00%`. | Project owner requested two decimal places. |
| SD5 | During a month change, keep the route frame and block-date panel mounted, update the selected month label immediately, and show loading only in the calendar grid until that month's response arrives. Start the date range empty in both API and fixture modes. | Keeps unrelated page content stable while still avoiding display of stale month cells; leaves the existing generic route error for failed loads. Rejected route-wide skeletons and preselected sample ranges. | Project owner approved month-only refresh and an empty default date range. |
