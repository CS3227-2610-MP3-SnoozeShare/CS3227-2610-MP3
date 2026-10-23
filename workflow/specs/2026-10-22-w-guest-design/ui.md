# UI

Four pages under `/guest/` plus four dialogs. All pages are wrapped by the existing `RouteGuard` (GUEST role) and `RoleShell` (guest navbar) from `frontend/src/app/guest/layout.tsx`. API calls use the existing `authenticatedFetch` from `frontend/src/lib/api.ts`.

---

## Guest landing (`/guest`)

Redirects to `/guest/search` immediately (client-side `redirect()` in Next.js). No UI states.

Navigation: entry from login/registration → AuthProvider → RouteGuard redirect.

---

## Search page (`/guest/search`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Initial | Filter bar (collapsed or inline) with empty results area | City text input, Check-in date picker, Check-out date picker, Guests dropdown (1-8, default blank), Max budget text input, "Search" button | Heading: "Explore listings" | GUEST |
| Loading | Spinner or skeleton grid below the filter bar | Filters remain interactive | — | GUEST |
| Empty results | Filter bar + message | Same filters + "Search" button | "No listings match your filters." | GUEST |
| Error | Filter bar + error banner | "Retry" button | "Something went wrong. Please try again." | GUEST |
| Populated | Filter bar + results grid (responsive: 1-3 columns) | Each result card is clickable; filters remain editable | — | GUEST |

**Result card:** listing title, property type, city, beds (or "Studio"), base nightly rate ("SGD X.XX / night"), availability badge ("Available" green or "Unavailable" muted). Unavailable cards shown at reduced opacity after available ones.

**Date pickers:** disable dates before today (N3, C53). Check-out picker disables dates on or before the selected check-in. Budget field accepts positive numbers only; non-numeric input shows inline validation "Enter a number or leave empty."

**Search triggers:** clicking "Search" or pressing Enter in any filter field. The initial page load performs a search with no filters.

Navigation: clicking a result card opens the Listing detail sheet.

---

## Listing detail sheet (Shadcn Sheet, right side panel)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading | Sheet open with skeleton content | Close button (X) | — | GUEST |
| Error | Sheet with error message | Close button, "Retry" button | "Could not load listing details." | GUEST |
| Populated | Full listing detail | Close, date pickers, "Book" button, review toggle | — | GUEST |

**Content (populated):**
- Header: title, property type, full address (street, city, region, postal code)
- Description paragraph
- Stats row: max guests, bedrooms, bathrooms
- Check-in / check-out times (formatted "h:mm a", or "—" if null)
- Amenities as pills (enum label with underscores → spaces, title case)
- Host section: display name, "Hosting since YYYY"
- Reviews section: average rating (stars + number), review count. Initially collapsed showing up to 2 reviews; "Show all N reviews" / "Show fewer" toggle. Each review (from `ListingReviewResponse`): guest name, star rating (1-5 filled/empty stars), comment (or "No comment provided.")
- Price estimator: check-in and check-out date pickers (pre-filled from search if set), nights count, nightly rate, total ("SGD X.XX"). Updates on date change
- "Book" button (disabled when: dates not set, check-out ≤ check-in, or price estimate errored)

**Booking flow:** clicking "Book" shows inline confirmation ("Book N nights for SGD X.XX?") with "Confirm" and "Cancel". On confirm → POST to create booking → success toast "Booking submitted!" + close sheet. On error → inline error message (e.g. "Insufficient wallet balance", "Dates are no longer available").

Navigation: entry from search card click. Exit: close button or click outside sheet. On successful booking, the sheet closes.

---

## Trips page (`/guest/trips`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading | Skeleton cards in 3 groups | — | — | GUEST |
| Empty | Message in center | — | "You have no trips yet. Start exploring!" with link to `/guest/search` | GUEST |
| Error | Error banner | "Retry" button | "Could not load your trips." | GUEST |
| Populated | 3 collapsible sections with trip cards | Section toggles, action buttons per card | Section headings: "Upcoming", "Active", "Past" | GUEST |

**Trip grouping** (mirrors desktop `TripDashboardController`):
- **Upcoming:** PENDING, or CONFIRMED with `startDate` > today. Sorted by `startDate` ascending
- **Active:** CONFIRMED with `startDate` ≤ today ≤ `endDate`. Sorted by `startDate` ascending
- **Past:** COMPLETED, FORCE_COMPLETED, FORCE_CANCELLED, REJECTED, CANCELLED_BY_GUEST, CANCELLED_BY_HOST, or CONFIRMED with `endDate` < today. Sorted by `endDate` descending

Empty sections show "No upcoming/active/past trips." and are collapsible.

**Trip card:** listing title, date range ("d MMM – d MMM yyyy"), host name, total amount ("SGD X.XX"), status pill (color-coded: Pending=amber, Checked in=blue, Confirmed=green, Completed=gray, Cancelled/Rejected=red). Cancelled/rejected cards at reduced opacity. Host decision message shown below the card if present.

**Action buttons per card** (shown only when applicable):
- "Message host" — visible on all CONFIRMED bookings → navigates to `/guest/messages` with that booking conversation selected (query param `?booking={id}`)
- "Cancel" — visible when `canCancel` is true (PENDING, or CONFIRMED before check-in) → opens Cancel dialog
- "File dispute" — visible when `canFileDispute` is true → opens Ticket filing dialog
- "Leave review" — visible when booking is COMPLETED (not FORCE_COMPLETED) and `hasReview` is false → opens Review dialog

Navigation: entry from guest nav. Action buttons open dialogs or navigate to messages.

---

## Cancel dialog (Shadcn Dialog)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading | Dialog with spinner | — | "Loading refund details..." | GUEST |
| Ready | Booking summary + refund preview | "Cancel booking" (destructive), "Keep booking" | "Title · dates · Booking #ref", refund line: "Refund: 100% (SGD X.XX)" or "Refund: 50% (SGD X.XX)" | GUEST |
| Processing | Buttons disabled, spinner on "Cancel booking" | — | — | GUEST |
| Error | Error message below buttons | "Try again", "Keep booking" | "Cancellation failed: {reason}" | GUEST |

**Refund copy:** 100% → "Full refund — pending booking or more than 48 hours before check-in"; 50% → "Partial refund — within 48 hours of check-in".

Navigation: entry from trip card "Cancel" button. Exit: "Keep booking" or successful cancellation (dialog closes, trips page refreshes).

---

## Ticket filing dialog (Shadcn Dialog)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading | Dialog with spinner for categories | — | — | GUEST |
| Ready | Filing form | Category dropdown, Title input, Description textarea, Remedy dropdown, Supporting info textarea (optional), "Submit", "Cancel" | Heading: "File a dispute" | GUEST |
| Processing | Buttons disabled, spinner on "Submit" | — | — | GUEST |
| Error | Inline error below form | "Try again" | "{reason}" | GUEST |

**Fields:**
- Category: dropdown populated from `GET /api/guest/tickets/categories` (alphabetical, "Other" last)
- Title: text input, required, max 200 characters
- Description: textarea, required
- Remedy: dropdown with 3 options: "Full Refund", "Partial Refund", "Other"
- Supporting information: textarea, optional

**Validation:** all required fields validated before submission. Empty required fields show "This field is required." inline.

Navigation: entry from trip card "File dispute" or messages page. Exit: "Cancel" or successful filing (dialog closes, trips page or messages page refreshes).

---

## Review dialog (Shadcn Dialog)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Ready | Stay summary + rating + comment | 5 clickable stars, Comment textarea, "Submit review", "Cancel" | Heading: "Leave a review", listing title, dates, "Host: {name}" | GUEST |
| Processing | Buttons disabled, spinner on "Submit" | — | — | GUEST |
| Error | Inline error below form | "Try again" | "{reason}" | GUEST |

**Star rating:** 5 interactive stars; clicking star N selects rating N. Unselected = empty stars. Required; submitting with 0 stars shows "Please select a rating."

**Comment:** textarea, required (not blank). Submitting blank shows "Please enter a comment."

Navigation: entry from trip card "Leave review". Exit: "Cancel" or successful submission (dialog closes, trips page refreshes, "Leave review" button removed for that booking).

---

## Messages page (`/guest/messages`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading | Skeleton sidebar + empty thread area | — | — | GUEST |
| Empty | Message in center | — | "No conversations yet." | GUEST |
| Error | Error banner | "Retry" | "Could not load conversations." | GUEST |
| Populated | Sidebar conversation list + selected thread | Conversation list items, message input, "Send" button, "File dispute" (booking conversations only) | — | GUEST |

**Sidebar conversation list:** merged ticket and booking conversations, sorted by last-message timestamp descending. Each item shows:
- Icon indicating type (ticket or booking)
- Title: ticket label + ticket title (for tickets), or listing title (for bookings)
- Subtitle: counterpart name
- Unread count badge (if > 0)
- Status pill for tickets (Open, Resolved)

**Thread area (selected conversation):**
- Header: conversation title + status
- Message bubbles: sender name ("You" for the guest, counterpart name for others), message body, timestamp. Guest messages aligned right, others left
- Message input at bottom: text input + "Send" button. Disabled with placeholder "This conversation is closed" when the thread is not open
- For booking conversations: a "File dispute" button in the header (if `canFileDispute` for that booking) → opens Ticket filing dialog with the booking ID

**Mark-read:** selecting a conversation calls the mark-read endpoint (PUT). The unread badge updates.

**Deep link:** `/guest/messages?booking={id}` auto-selects the booking conversation on load (used by the "Message host" action on the trips page).

Navigation: entry from guest nav or trip card "Message host". "File dispute" opens the ticket filing dialog.

---

## Wallet page (`/guest/wallet`)

| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading | Skeleton balance card + skeleton transaction list | — | — | GUEST |
| Error | Error banner | "Retry" | "Could not load wallet." | GUEST |
| Populated | Balance card + transaction history + action buttons | "Top up" button, "Withdraw" button | Balance: "SGD X.XX", heading: "Transaction History" | GUEST |
| Empty transactions | Balance card + empty list message | "Top up", "Withdraw" | "No transactions yet." | GUEST |

**Balance card:** prominent display of current balance ("SGD X.XX"), currency label.

**Transaction history:** table of transactions, newest first. Each row: date/time, action type (human-readable label mapped from the `actionType` enum: "Top Up", "Withdrawal", "Escrow Hold", "Escrow Refund", "Booking Payout", "Platform Fee", "Dispute Refund", "Dispute Payout"), amount (green for credits, red for debits), balance after, reason (if present).

**Top-up flow:** clicking "Top up" opens an inline form or Shadcn Popover: amount input (positive number, required), "Confirm" button. On success → toast "Topped up SGD X.XX", list refreshes. On error → inline error.

**Withdraw flow:** clicking "Withdraw" opens an inline form: amount input (positive number, required, ≤ current balance), "Confirm" button. Validation: if amount > balance, show "Amount exceeds your balance." On success → toast "Withdrew SGD X.XX", list refreshes. On error → inline error.

Navigation: entry from guest nav.
