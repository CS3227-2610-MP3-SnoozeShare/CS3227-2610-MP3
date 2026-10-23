# Requirements

Ordered so that a requirement comes after anything it depends on.

## Functional

| ID | Requirement | Acceptance criterion |
|---|---|---|
| F1 | The system shall let a guest search active listings by city, date range, guest count and maximum nightly budget; all filters are optional | Given a guest on the search page, when they submit any combination of filters (including none), then the results show all active listings matching those filters; each result shows availability for the selected dates |
| F2 | The system shall let a guest view a listing's detail: property info, amenities, host profile, and reviews | Given a guest viewing search results, when they select a listing, then a detail panel shows title, type, address, description, capacity, bedrooms, bathrooms, check-in/out times, amenities, host name and "hosting since" year, and reviews with average rating |
| F3 | The system shall let a guest estimate the total cost for a listing and date range | Given a guest viewing a listing detail, when they select valid check-in and check-out dates, then the panel shows the nightly rate, number of nights and total amount |
| F4 | The system shall let a guest submit a booking request for a listing and date range | Given a guest viewing a listing detail with valid dates, when they click "Book", then the system creates a PENDING booking, holds escrow from the guest's wallet, and shows a success confirmation |
| F5 | The system shall let a guest view their trips grouped into Upcoming (pending or future confirmed), Active (checked-in confirmed) and Past (completed, cancelled, rejected, or confirmed past end-date) sections | Given a guest on the trips page, then each booking appears in exactly one group, with the listing title, host name, dates, status, total cost and available actions |
| F6 | The system shall let a guest cancel a pending or pre-check-in confirmed booking, showing a refund preview before confirmation | Given a guest with a cancellable booking, when they initiate cancellation, then a dialog shows the refund percentage and amount; on confirmation the booking is cancelled and the refund is applied |
| F7 | The system shall let a guest file a dispute ticket for a booking within the dispute window (endDate + 7 days), selecting a category, title, description, remedy type and optional supporting text | Given a guest with a dispute-eligible booking and no existing ticket, when they complete the filing form, then the system creates an OPEN ticket linked to that booking |
| F8 | The system shall let a guest view and send messages in ticket threads (GUEST channel) and booking conversations, and see conversation summaries with unread counts | Given a guest on the messages page, then they see a list of ticket and booking conversations sorted by last activity; selecting one shows the message thread; they can send messages in open threads |
| F9 | The system shall let a guest submit a review (1-5 star rating and comment) for a completed booking that has no existing review | Given a guest with a completed, un-reviewed booking, when they submit a rating and comment, then the review is persisted and the "Leave review" action is no longer available for that booking |
| F10 | The system shall let a guest view wallet balance, transaction history, top up and withdraw | Given a guest on the wallet page, then they see their current balance, a list of transactions (newest first), and can top up or withdraw any positive amount not exceeding the balance (for withdrawals) |

## Non-functional

| ID | Category | Requirement | Acceptance criterion |
|---|---|---|---|
| N1 | Security | Every `/api/guest/**` endpoint enforces the GUEST role via `RoleAuthorizationFilter`; a guest can only read and mutate their own data (bookings, wallet, tickets, messages, reviews) | An authenticated HOST or AGENT calling any guest endpoint receives 403; a guest calling an endpoint with another user's resource ID receives 403 or 404 (no information leakage about other users' data) |
| N2 | Performance | Search, trip listing and conversation listing endpoints respond within 2 seconds under normal single-instance load | Measured in backend integration tests with representative data |
| N3 | Usability | All date inputs in the frontend reject past dates; form validation shows inline errors before the form is submitted to the backend | Date pickers disable dates before today; required fields show a validation message when the user attempts to submit an incomplete form |
| N4 | Audit | Booking creation, cancellation, ticket filing, review submission, wallet top-up and withdrawal produce audit rows via the existing service-layer audit logic | No new audit-writing code is needed; the controller delegates to services that already call `AuditService.record`; an integration test confirms the audit row exists after each mutation |
