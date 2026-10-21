# UI

Reference: the supplied [Claude artifact](https://claude.ai/artifact/PWBCxbfv9e9FGVvY6RKUwd), specifically its Fall Light Design System and Host artboards 13–17, plus the shared Messages and Wallet artboards.

## Shared Host shell

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading | Existing W-AUTH loading response from `RouteGuard` | None | `Loading…` | Host route while auth is unresolved |
| Error | Existing W-AUTH profile error response | Retry, Sign out | `We couldn’t load your profile` | Host route when profile loading fails |
| Authenticated | Cream page canvas; compact top row with `SnoozeShare`, `Host workspace`, right-aligned `Wallet: <amount>`, `Log out`; second row with tabs Listings, Requests, Messages, Wallet | Brand link, four tab links, logout | Exact labels above | HOST only |

Navigation: `/host` redirects to `/host/listings`; tab links enter `/host/listings`, `/host/requests`, `/host/messages`, and `/host/wallet`. The active tab has rust underline and emphasized text.

All Host content pages use the full available viewport width with consistent page-edge padding; do not cap the main page frame at a centered maximum width.

`/host` redirects to `/host/listings`. The shell wallet badge reads `Wallet: $3,920.50` in the populated fixture, `Wallet: $0.00` in the empty fixture, `Wallet: —` in loading, and `Wallet: unavailable` in error. The `RoleNavbar` route map is shared, but only the Host route values change in this slice; Guest and Agent route values remain unchanged.

## Fixture contract

The fixture adapter defaults to `populated`. Tests replace its state provider with one of four exact states: `populated`, `empty`, `loading`, or `error`. The adapter contract is `createHostFixtureAdapter(state = 'populated')`, returning `getState()`, `getWalletSummary()`, `getListings()`, `getListing(id)`, `getCalendar(id)`, `getRequests()`, `getMessages()`, and `getStatement()`. `getListing(id)` accepts only `lakeside-cabin`, `downtown-loft`, `garden-studio`, or `hilltop-house`; every other value returns not-found. Tests inject the adapter through the Host fixture provider and create a fresh adapter per test, so state resets between tests. Fixture records are synthetic, are not keyed to the authenticated user's ID, and may be bundled in the frontend because they are not production user data.

Populated listing records:

| Title | Type | Location | Rate | Bookings | Rating | Status |
|---|---|---|---:|---:|---:|---|
| Lakeside Cabin | House | Lakeview, WI | $120/night | 18 | 4.8 | ACTIVE |
| Downtown Loft | Apartment | Lakeview, WI | $95/night | 11 | 4.6 | ACTIVE |
| Garden Studio | Private room | Lakeview, WI | $58/night | 4 | 4.9 | INACTIVE |
| Hilltop House | House | Millbrook, WI | $210/night | 7 | 4.7 | ACTIVE |

Lakeside Cabin detail records: `ACTIVE`; `142 Shoreline Dr, Lakeview, WI 53211`; 4 max guests; 2 bedrooms; 1 bathroom; `$120` per night; check-in `3:00 PM`; check-out `11:00 AM`; description `A cozy cabin steps from the water, with a private dock and wood-burning stove. Perfect for a quiet long weekend away from the city.`; amenities Wifi, Parking, Kitchen, Washer; reviews `4.8 ★ (23)`; performance Total bookings `18`, Occupancy (30d) `62%`, Earnings (30d) `$1,860.00`.

Populated request records:

| Guest | Listing | Dates | Nights | Gross | Net earning | Rating | Status |
|---|---|---|---:|---:|---:|---|---|
| Jane G. | Lakeside Cabin | Oct 3–6 | 3 | $360.00 | $349.20 | 4.9 ★ | PENDING |
| Marcus T. | Downtown Loft | Oct 10–12 | 2 | $190.00 | $184.30 | No ratings yet | PENDING |
| Priya S. | Lakeside Cabin | Oct 18–22 | 4 | $480.00 | $465.60 | 4.7 ★ | PENDING |
| Wei L. | Hilltop House | Sep 1–4 | 3 | $630.00 | — | — | CONFIRMED |
| Ana R. | Lakeside Cabin | Aug 20–22 | 2 | $240.00 | — | — | REJECTED |
| Dana K. | Downtown Loft | Aug 12–15 | 3 | $285.00 | — | — | CONFIRMED |

The populated calendar is September 2026 with `Sep 15` through `Sep 17` as the block form example and `Oct 3 – Oct 5` as the current override.

The populated wallet balance is `$3,920.50` and its exact synthetic statement rows are:

| Date | Type | Related to | Amount | Balance after |
|---|---|---|---:|---:|
| Sep 24, 2026 | BOOKING_PAYOUT | Booking #B-2291 | +$349.20 | $3,920.50 |
| Sep 20, 2026 | TOP_UP | — | +$500.00 | $3,571.30 |
| Sep 7, 2026 | BOOKING_PAYOUT | Booking #B-2255 | +$203.70 | $3,071.30 |
| Aug 30, 2026 | ESCROW_HOLD | Booking #B-2255 | -$210.00 | $2,867.60 |
| Aug 20, 2026 | WITHDRAWAL | — | -$300.00 | $3,077.60 |

The populated Messages fixture contains exactly two list rows: `Lakeside Cabin · Oct 3–6` / `Direct message · Jane G. (Guest)` / `Question about check-in` / `OPEN`, and `Ticket #1042 · Lakeside Cabin · Support Agent` / `Refund not received` / `RESOLVED`. The selected first thread contains, oldest first, `Jane G.: Hi, what time can we check in?` and `You: Check-in starts at 3:00 PM.`; the composer placeholder is `Write a message…`. Tests assert this exact list order, selected thread, and local message append rather than a domain identity.

## Host — Listings (`/host/listings`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Heading `My listings`, no rows, centered empty card | `+ New listing` | `You don't have any listings yet.` | HOST |
| Loading | Shell plus heading and four listing-row skeletons | None | No fixture values | HOST |
| Error | Shell plus page-level error card | Retry | `Unable to load your listings.` | HOST |
| Populated | Heading, `+ New listing`, four horizontal listing cards. Each card has warm thumbnail tile, title, type/location/rate, bookings, rating, active toggle/status, `Open Booking Calendar`, and `Edit` | New listing, toggle presentation, calendar link, edit link | `My listings`, `+ New listing`, `BOOKINGS`, `RATING`, `ACTIVE`, `INACTIVE` | HOST |

Rows use the artifact fixture content: Lakeside Cabin, Downtown Loft, Garden Studio, and Hilltop House. The row layout is a single bordered card with compact metrics aligned to the right.

## Host — Listing detail (`/host/listings/:listingId`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Shell and not-found card | Back to Listings | `Listing not found` | HOST |
| Loading | Shell, breadcrumb skeleton, hero/card skeletons | None | No fixture values | HOST |
| Error | Shell and error card | Back, Retry | `Unable to load this listing.` | HOST |
| Populated | Breadcrumbs; title/type/status/address; metric strip; description; amenities; rating and review excerpts; Performance card with Total bookings, Occupancy (30d), Earnings (30d) | Listings breadcrumb, `Open Booking Calendar`, `Edit listing` | `Host workspace`, `AMENITIES`, `REVIEWS`, `Performance` | HOST |

## Host — New/Edit Listing (`/host/listings/new`, `/host/listings/:listingId/edit`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | New-listing form with blank values | Inputs, dropdowns, checkboxes, Save, Cancel | `New listing` | HOST |
| Loading | Shell, breadcrumbs, two-column card skeletons | None | No fixture values | HOST |
| Error | Form remains visible with inline error summary | Retry, Cancel | `Unable to load this listing.` | HOST |
| Populated | Two-column layout: left Basic Details + Location; right Capacity & Pricing + Amenities. Breadcrumbs above; Save/Cancel below | Text inputs, text area, Property type dropdown, Status dropdown, numeric/time fields, amenity checkboxes, `Save listing`, `Cancel` | Section labels exactly `BASIC DETAILS`, `LOCATION`, `CAPACITY & PRICING`, `AMENITIES` | HOST |

Until W-DOMAIN exists, Save and status controls are presentation-only. Edit form amenities reflect the fixture's saved selection; status choices display `Active` / `Inactive` (not lowercase values), and editable values use normal font weight while field labels remain emphasized. Required fields are Title, Description, Property type, Status, Street address, City, Region, Postal code, Max guests, Bedrooms, Bathrooms, Rate / night, Check-in time, and Check-out time. Numeric validation is: Max guests ≥ 1 integer; Bedrooms ≥ 0 integer; Bathrooms ≥ 0 integer; Rate / night > 0 with at most two decimal places. Exact validation messages are `Title is required`, `Description is required`, `Street address is required`, `City is required`, `Region is required`, `Postal code is required`, `Max guests must be at least 1`, `Bedrooms cannot be negative`, `Bathrooms cannot be negative`, and `Rate / night must be positive with at most 2 decimal places`. Save validates the local form and returns to `/host/listings/lakeside-cabin` for the supplied edit fixture or `/host/listings` for the new-listing form; status changes reset on reload. A missing edit fixture renders only the not-found card and does not render the form.

## Host — Booking Calendar (`/host/listings/:listingId/calendar`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Month grid with no booked/blocked dates and empty Current overrides | Month navigation, date fields, reason field, block action | `No current overrides` | HOST |
| Loading | Shell, breadcrumb, legend, grid, and form skeletons | None | No fixture values | HOST |
| Error | Shell and calendar error card | Retry, Listings | `Unable to load the booking calendar.` | HOST |
| Populated | Breadcrumbs; complete five-week September 2026 grid with weekday headings; legend Available/Booked/Blocked/Other month; Block dates form; Current overrides with `Oct 3 – Oct 5` | Previous/next month, From, To, Reason, `Block these dates`, remove presentation control | `Block dates`, `Current overrides`, `FROM`, `TO`, `Reason (optional)` | HOST |

Calendar day states are represented by color plus text/accessible state. The artifact's compact seven-column grid and right-side block panel are preserved. The month arrows navigate local calendar presentation only; they do not mutate availability.

## Host — Requests (`/host/requests`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | `Booking requests` heading, zero-pending indicator, empty pending and past sections | None | `No pending requests` and `No past requests` | HOST |
| Loading | Heading, pending count skeleton, pending and past table skeletons | None | No fixture values | HOST |
| Error | Heading plus page error card | Retry | `Unable to load booking requests.` | HOST |
| Populated | `Booking requests`, `3 pending`, pending table, explanatory net-earning note, past requests table | Approve/Reject presentation controls | Pending columns: `GUEST`, `LISTING`, `DATES`, `NIGHTS`, `GROSS`, `NET EARNING`, `GUEST RATING`, `ACTION`; past columns: `GUEST`, `LISTING`, `DATES`, `NIGHTS`, `TOTAL`, `STATUS`; note `Net earning = gross − 3% platform fee.` | HOST |

## Shared Host — Messages (`/host/messages`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Shell, `Messages` heading, empty list and empty conversation panel | None | `No messages yet.` | HOST |
| Loading | Conversation-list and thread skeletons | None | No fixture values | HOST |
| Error | List area error with thread area preserved | Retry | `Unable to load messages.` | HOST |
| Populated | Left conversation list; right selected thread. Rows distinguish Direct message and Support Agent ticket; thread shows sender, body, composer | Conversation selection, message input, Send presentation control; no New Ticket action for Hosts | `Messages`, `Direct message`, `Ticket`, `Send` | HOST |

## Shared Host — Wallet (`/host/wallet`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Empty | Balance card at zero and empty statement | Top up, Withdraw presentation controls | `AVAILABLE BALANCE`, `Transaction statement`, `No transactions yet.` | HOST |
| Loading | Balance and statement skeletons | None | No fixture values | HOST |
| Error | Balance error and statement error states | Retry | `Unable to load wallet.` | HOST |
| Populated | Balance card with `Wallet: $3,920.50`, available balance, escrow note, transaction statement table | Top up, Withdraw presentation controls | `AVAILABLE BALANCE`, `Includes $0.00 currently held in escrow for pending bookings`, columns `DATE`, `TYPE`, `RELATED TO`, `AMOUNT`, `BALANCE AFTER` | HOST |

Until W-DOMAIN exists, wallet controls open local confirmation presentation only; they have no financial effect and never claim a completed transaction. The local confirmation presentations use `Cancel` and a visually emphasized confirmation control; closing either leaves the fixture unchanged. Top-up copy is `Mocked top-up — no real payment gateway is charged.` and withdrawal copy is `Mocked withdrawal — no real payout rail.`

Calendar validation is local: From and To are required; To must be on or after From; invalid ranges show `End date must be on or after start date`; a blank reason is allowed. Block confirmation uses `Block these dates?`, the selected listing name, From/To values, and `Guests won't be able to request or book these dates. This won't affect any existing confirmed booking.` Closing the modal leaves the calendar fixture unchanged.

Approve confirmation uses `Approve booking request?`, listing, guest, date range, nights, earning, and `The booking moves to CONFIRMED and the guest is notified. Funds stay held in escrow until check-in.` Reject confirmation uses `Reject booking request?`, listing, guest, date range, refund amount, optional `Message to guest`, and `The guest will be notified and the held funds fully refunded (ESCROW_REFUND) — no fee is charged for a host rejection.` Both close without changing fixtures. Selecting the resolved Support Agent thread disables the composer and Send control; the direct thread keeps the composer.
