# Branch State — W-HOST

**What this branch has changed, and how to pick it up.** `PROJECT_STATE.md` describes `INT`;
read it for context and write here instead of editing it. `sync-project-state` folds this file
in after the branch merges.

- **Branch:** W-HOST (from `development` at `8702e88`)
- **Base:** `8702e88`
- **Owner:** project owner
- **Status:** In review
- **Approved:** —
- **Last updated:** 2026-10-21 by GPT-5 — message composer sends on Enter and scrolls to the newest message

---

**Scope.** Assess and, if requested, implement host work that can proceed without the W-DOMAIN
model, persistence, and service contracts.

---

## 1. Orientation

No change.

## 2. How to Resume

**Setup**, only if it differs from `PROJECT_STATE.md` § How to Resume:

No change.

**Sessions**

| Session | Started | Agent(s) | Workstream | Status | Doing | Next action | Last touched |
|---|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — | — |

**Remaining**

- [ ] Owner review and approval remain before integration. Keep domain-dependent contracts provisional until W-DOMAIN settles them.

## 3. Workstreams

| ID | Workstream | Status | Spec | Plan | Progress | Guide |
|---|---|---|---|---|---|---|
| W-HOST | Host features — listings, calendar, requests, wallet, messages, Next.js pages | In review | [spec](docs/superpowers/specs/2026-10-21-w-host-ui-design.md) | [plan](docs/superpowers/plans/2026-10-21-w-host-ui.md) | Messages fill viewport; composer sends on Enter and auto-scrolls after send; 134 tests, lint, isolated build pass | guide out of date: W-HOST Host UI pages and Fall Light details |

## 4. Architecture

No boundary changes. Host pages remain fixture-backed and independent of W-DOMAIN.

## 5. Conventions

Host route content uses a full-width page frame; input value text is normal-weight, while field labels remain emphasized.

## 6. Known Gaps

No change: existing W-DOMAIN dependency and accepted gaps remain in force.

## 7. Needs a Human

None.

## 8. Decisions

| ID | Date | Decision | Why / who asked | Source |
|---|---|---|---|---|
| C~1 | 2026-10-21 | W-HOST UI must match the supplied Claude artifact exactly, including its Fall Light tokens, shell layout, page composition and interaction presentation | User explicitly set the artifact as the visual source of truth; generic existing Tailwind styling is insufficient | [Claude artifact](https://claude.ai/artifact/PWBCxbfv9e9FGVvY6RKUwd) |
| C~2 | 2026-10-21 | Host content pages use the full available width; edit values use normal weight and selected status uses title case; host messages do not show a New Ticket action | Project owner, after authenticated comparison, directed these UI corrections and explicitly rejected adding New Ticket to Host messaging | Operator conversation |
| C~3 | 2026-10-21 | Listings page places its create action beside the title; cards omit the status pill and use five centered, width-stable right-side groups, with outlined calendar/edit actions | Project owner requested the final Listings refinements after reviewing the pushed W-HOST implementation | Operator conversation |
| C~4 | 2026-10-21 | Within the Availability group, the switch sits to the right of its ACTIVE/INACTIVE label; reduce horizontal gaps among all five card groups while retaining centered content and stable widths | Project owner requested a follow-up after reviewing the first T9 layout | Operator conversation |
| C~5 | 2026-10-21 | New/Edit listing forms retain full-width page use, with their four form cards arranged to fill the available width rather than constraining the form to the artifact's narrower artboard | Project owner clarified that the forms should add up to use the full width after the visual comparison surfaced the apparent width difference | Operator conversation |
| C~6 | 2026-10-21 | Booking calendar shows as many complete week rows as its month requires; blocking uses one artifact-style date-range picker and a required reason; current blocked dates list bold reasons above ranges with aligned remove actions, using Maintenance as the fixture reason | Project owner specified calendar coverage and the date-blocking presentation; separate start/end fields, optional reasons, and unlabeled override ranges are rejected | Operator conversation |
| C~7 | 2026-10-21 | Booking-calendar month navigation uses fixed arrow columns with the month/year centered in a fixed-width track, so varying title length does not move the arrows | Project owner requested stable arrow placement and a centered month/year label when navigating across month names of different lengths | Operator conversation |
| C~8 | 2026-10-21 | Reopened date picker must calculate its Sunday-first grid from the first of the selected month, not the selected date itself | Project owner reported selected dates shifted to incorrect weekdays after closing and reopening the picker | Operator conversation |
| C~9 | 2026-10-21 | The booking calendar always renders six week rows; the date-range picker stages edits until Apply, Clear resets the draft without closing, outside click closes without applying, and picker/current-blocked ranges include the end year (both years across year boundaries) | Project owner specified the calendar layout, artifact-style picker controls, dismissal behavior, and range-year presentation | Operator conversation |
| C~10 | 2026-10-21 | The current page name in every Host breadcrumb is bolded | Project owner requested consistent visual emphasis for the current page | Operator conversation |

## 9. Deviations

| ID | Date | Deviation | Consequence / action |
|---|---|---|---|
| D~1 | 2026-10-21 | Initial T8 comparison was blocked by missing Host authentication; operator subsequently logged in and enabled a live route comparison | Resolved. Authenticated artifact comparison completed; all eight Host content routes were checked at 1680×945, 1280×800, and 768×1024 with no horizontal overflow |
| D~2 | 2026-10-21 | Next.js production build intermittently failed during page-data collection with `Invariant: no direct app page entry found for /_not-found` after the breadcrumb implementation | Build passed from an isolated frontend copy while the local dev server remained active; shared `.next` state may explain the earlier failure, but cause is not proven. Recheck from a clean isolated build if it recurs |

## 10. Changes

| Date | What changed | Workstream | Ref |
|---|---|---|---|
| 2026-10-21 | Specified the exact Claude-artifact Fall Light Host UI, synthetic fixture contract, local-only presentation behavior, and visual verification criteria | W-HOST | `c608be1` |
| 2026-10-21 | Added shared Fall Light tokens and UI primitives with accessible focus and status presentation | W-HOST | T1 |
| 2026-10-21 | Added deterministic synthetic Host fixture adapter, typed records, and state-replaceable provider | W-HOST | T2 |
| 2026-10-21 | Added Host-only fixture shell, explicit route map, active tabs, wallet badge, and `/host` redirect | W-HOST | T3 |
| 2026-10-21 | Added fixture-backed listing cards, detail metrics, and local-only new/edit listing forms | W-HOST | T4 |
| 2026-10-21 | Added calendar precedence, booking block presentation, and pending/history request screens | W-HOST | T5 |
| 2026-10-21 | Added Host messages, resolved-thread behavior, wallet statement, and mocked wallet controls | W-HOST | T6 |
| 2026-10-21 | Added cross-screen local-only interaction tests and no-network guarantees | W-HOST | T7 |
| 2026-10-21 | Corrected full-width Host framing, listing/detail/calendar/request parity, and edit-form typography; no Host New Ticket action; fixed inactive switch token and modal keyboard focus; all 97 frontend tests, lint, build, and three-viewport route checks pass | W-HOST | T8 |
| 2026-10-21 | Aligned New listing with the page title; removed status pills; centered five fixed-width card groups and added outlined calendar/edit actions; all 99 frontend tests, lint, build, and desktop/tablet browser checks pass | W-HOST | T9 |
| 2026-10-21 | Kept the availability switch to the right of its label and compacted the five desktop card groups; all 99 frontend tests, lint, production build, and desktop browser check pass | W-HOST | T9 follow-up |
| 2026-10-21 | Matched New/Edit listing forms to artifact card order, paired fields, six amenity rows, section styling, breadcrumbs, currency/time display, and action order while retaining full-width cards; 104 tests, lint, build, and browser checks pass | W-HOST | T4 follow-up |
| 2026-10-21 | Added visible Property type/Status labels; independently sized cards; tightened form gaps; positioned select menus directly below fields; increased breadcrumb spacing; 105 tests, lint, build, and browser check pass | W-HOST | T4 follow-up |
| 2026-10-21 | Split New/Edit listing cards into top-aligned desktop stacks, kept the narrow-screen card sequence, tightened vertical field/card gaps, and normalized select trigger weight; 105 tests and lint pass | W-HOST | T4 follow-up |
| 2026-10-21 | Applied shared chevron breadcrumbs to New, Edit, listing detail, and booking calendar routes; expanded route regression coverage; 106 tests and lint pass | W-HOST | T4 follow-up |
| 2026-10-21 | Added entry-example placeholders to New/Edit title, description, address, location, capacity, and rate inputs; 107 tests, lint, and production build pass | W-HOST | T4 follow-up |
| 2026-10-21 | Added pointer cursors for enabled buttons and hover feedback for outlined buttons and links | W-HOST | T1 follow-up |
| 2026-10-21 | Refined listing action link colors and listing-detail actions; restored page-title tracking style | W-HOST | Operator follow-up |
| 2026-10-21 | Made each listing card surface navigate to its detail page while preserving independent availability, calendar, and edit controls; 109 tests, lint, and build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Matched listing detail to the artifact with a gradient hero, six metrics, compact inline reviews/performance, and breadcrumb-row actions; 111 tests, lint, and isolated production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Increased detail/address typography; added review spacing; aligned Performance beside title/metrics at ~40% content width; top-aligned breadcrumb with shared Host placement; 111 tests, lint, isolated production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Simplified outlined listing-card action-link styles while retaining the shared hover background; 111 tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-21 | Added an intermediate three-column card-info layout to prevent the Edit action being stranded at narrow viewport widths; 111 tests, lint, production build, and 628px browser check pass | W-HOST | Operator follow-up |
| 2026-10-21 | Matched the calendar split panel, month/legend row, day colors and sizing, sample bookings, block form, and removable override; 114 tests, lint, production build, and browser check pass | W-HOST | Operator follow-up |
| 2026-10-21 | Added month-sized calendar week rows; artifact-style single range picker; required reason; and reason-first blocked-date entries with Maintenance fixture. 117 tests, lint, production build, and browser checks pass | W-HOST | Operator follow-up |
| 2026-10-21 | Fixed calendar month navigation to fixed arrow columns around the centered label; verified May and September alignment, 118 tests, lint, and production build | W-HOST | Operator follow-up |
| 2026-10-21 | Fixed date-picker weekday drift by anchoring reopened grids to the selected month’s first day; Sep 16 stays Wednesday after reopening. 119 tests, lint, and production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Booking calendar now always renders six weeks; date picker stages edits for Apply, Clear leaves it open, outside click dismisses without applying, and picker/blocked-date ranges share year-aware formatting. 124 tests, lint, production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Bolded the current page in shared Host breadcrumbs, marked it with aria-current="page", and added a regression test; 125 frontend tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-21 | Matched booking requests hierarchy, card tables, emphasis, statuses, action buttons, and contextual approval/rejection dialogs; preserved header and full-width layout; 127 frontend tests, lint, and production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Matched approval/rejection dialogs to artifact sizing, summary colors, copy order, input and action styling; outside clicks now dismiss modals without dismissing on inside clicks; 128 frontend tests, lint, and production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Guarded backdrop dismissal by pointer origin so dragging the rejection textarea resize handle outside the modal no longer closes it; verified 128 tests, lint, production build | W-HOST | Operator follow-up |
| 2026-10-21 | Grouped the booking-requests pending pill directly beside its title while preserving the New listing title/action row; 128 tests and lint pass | W-HOST | Operator follow-up |
| 2026-10-21 | Built direct-message threads from the three current pending booking requests, added listing/date/guest context and sender-aligned bubbles, retained the existing read-only support ticket without a New Ticket action; 130 tests, lint, and production build pass | W-HOST | Operator follow-up |
| 2026-10-21 | Messages route now fills the viewport below the navbar; conversation and transcript columns stay within it and scroll internally; 132 tests, lint, isolated production build, and live viewport overflow check pass | W-HOST | Operator follow-up |
| 2026-10-21 | Message history scrolls to the newest message when a message is sent, and Enter sends the composer draft; 134 tests, lint, and isolated production build pass | W-HOST | Operator follow-up |
