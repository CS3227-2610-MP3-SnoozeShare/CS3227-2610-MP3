# Edge Cases and Non-Happy Paths

IDs are permanent: never reuse or renumber. Every F and N requirement and every API error appears in at least one row. "Integration" means a `PostgresIntegrationTest` with security enabled.

## Permission
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E1 | N1, F1 | A Guest or Host token calls any `/api/agent/**` route | 403 `ROLE_MISMATCH`; no service is called (spy) | Role isolation protects money and governance actions | integration |
| E2 | N1, F2 | No `Authorization` header, or a token the verifier rejects | 401 `INVALID_TOKEN` | Unauthenticated access | integration |
| E3 | N1, F16 | An agent whose status is SUSPENDED (set in the database) uses a valid token | 403 `PROFILE_INACTIVE` on every agent route | Suspension must bite without touching Firebase | integration |
| E4 | F1 | A mutating body also contains `"agentId":"<other uuid>"` | 400 `VALIDATION_ERROR`; no service call; no audit row | Actor spoofing; relies on `fail-on-unknown-properties` | integration |
| E5 | N1 | Application started with `snoozeshare.security.enabled=false` | No agent controller bean exists; `GET /api/agent/disputes` returns 404 | A permit-all chain must not expose actor-less endpoints (SD9) | integration |
| E6 | N1, F16 | A valid Firebase token with no `users` row | 404 `PROFILE_NOT_REGISTERED` | Existing filter contract | integration |
| E62 | N1, F1 | A Guest token calls `/api/%61gent/disputes`, `//api/agent/disputes`, `/api/agent/disputes/`, `/api/agent/disputes;x=1` and `/api/agent/%2e%2e/guest/x`; an unauthenticated caller does the same | Every variant is 403 `ROLE_MISMATCH` (guest) or 401 `INVALID_TOKEN` (anonymous), or is rejected by the firewall with 400; none reaches a controller | The raw-URI prefix check could be bypassed | integration |
| E69 | F1 | A controller method is invoked with the request attribute missing (filter bypassed in a slice test) | The resolver throws `MissingActorException`; 401 `INVALID_TOKEN`; no service call | Fail closed, never a null actor | integration |

## Input
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E7 | F2 | A JSON body of 70,000 bytes; a request with `Transfer-Encoding: chunked` and no `Content-Length`; a bodyless PUT (assign) | 413 `PAYLOAD_TOO_LARGE`; 411 `LENGTH_REQUIRED`; the PUT succeeds; nothing stored for the first two | Memory and storage abuse | integration |
| E8 | F2 | `{ticketId}`, `{userId}` or `{categoryId}` is not a UUID | 400 `VALIDATION_ERROR` | Stable errors | integration |
| E9 | F3, F8 | `assignee` or `channel` is not a defined value | 400 `VALIDATION_ERROR` | Enum input | integration |
| E13 | F6 | Notes of 4,000 and 4,001 characters; notes `null` and `""` | 4,000 accepted; 4,001 is 400 `VALIDATION_ERROR` with notes unchanged; `null` and `""` both store `""` | Boundary | integration |
| E19 | F7 | `mode` MANUAL with no `guestRefund` | 400 `INVALID_REQUEST` `Guest refund is required` | A money action needs an explicit figure | integration |
| E22 | F7 | ACCEPT on a PARTIAL_REFUND or OTHER ticket with refund 0 or none | 400 `INVALID_REQUEST` with the service message | Accept must refund something | integration |
| E23 | F7, N2 | MANUAL refund 100.01 on an escrow of 100.00; refund -1; refund 10.505; refund with 10 integer digits; refund 100.00 | The first is 400 `INVALID_REQUEST` `Guest refund cannot exceed the escrow held`; the next three are 400 `VALIDATION_ERROR`; the last succeeds; a rejected request changes no wallet | Over-refund would create money | integration |
| E28 | F10 | Suspend or reactivate with a blank reason | 400 `VALIDATION_ERROR` | Every governance action needs a recorded reason | integration |
| E30 | F11 | `limit` 0, 201 or `offset` -1 | 400 `VALIDATION_ERROR` | Bounded pages | integration |
| E32 | F11 | `from` after `to`; only `from` given; only `to` given | 400 for the first; the others filter one side only | Ranges | integration |
| E33 | F11 | `actions=NOT_AN_ACTION`; `actions=` empty; `actions=TOP_UP,TOP_UP, WITHDRAWAL`; 41 items; `text` of 101 characters | 400; no filter; two actions; 400; 400 | Input rules | integration |
| E51 | F8 | A message body that is blank; 2,000 characters; 2,001 characters | 400; accepted; 400 | Boundary | integration |
| E65 | F7, F10, F12 | A reason of 1,000 and 1,001 characters; a category label of 60 and 61 characters; a blank label | Accepted; 400; accepted; 400; 400 | Boundary | integration |
| E66 | F4, F6, F7, F8 | Notes, resolve, thread read and thread post on an unknown ticket id | 404 `NOT_FOUND` `Ticket does not exist` each | Typed not-found | integration |

## State and concurrency
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E10 | F5 | Two threads (separate committed transactions, two agents) assign the same OPEN ticket; a spy holds the first thread inside its transaction after the lock until the second has called the service | Exactly one 200; the other 409 `Ticket is not open`; one `TICKET_ASSIGNED` row; the assignee is the winner | A ticket has one owner; needs the row lock (SD10) | integration |
| E11 | F5, F4 | Assign or open an unknown ticket id | 404 `NOT_FOUND` `Ticket does not exist` | Typed not-found | integration |
| E12 | F6 | Agent B saves notes on a ticket assigned to agent A | 409 `Ticket is not assigned to this agent`; notes unchanged | Notes belong to the assignee | integration |
| E14 | F5 | The assignee assigns the same ticket again | 409 `Ticket is not open` | Repeat is not silent | integration |
| E15 | F7 | Agent B resolves a ticket assigned to agent A | 409 `Ticket is not assigned to this agent`; no wallet change | Only the owner settles money | integration |
| E16 | F6 | Notes on an OPEN ticket or on a resolved one | 409 `Notes can only be saved while the ticket is in review` | State machine | integration |
| E17 | F4, F6, F8 | Notes saved on a ticket | Neither thread contains the note text; `agentNotes` appears in `DisputeDetailResponse` only and `TicketResponse` has no such field | Notes stay internal | integration |
| E20 | F7, N2 | Two threads (committed transactions) resolve one ticket (the assignee in two tabs); a spy holds the first thread inside its transaction until the second has entered `resolve`, so the second has not read the ticket before the first commits | Exactly one 200; the other 409 `Ticket is not in review`; the guest and host wallets each change once; one settlement audit set | Escrow is settled once; needs the row lock (SD10) | integration |
| E21 | F7 | Resolve a resolved ticket | 409 `Ticket is not in review` | Repeat | integration |
| E24 | F10 | Suspend or reactivate an Agent user or the System user | 400 `INVALID_REQUEST` `Support agent accounts cannot be suspended or reactivated` | No lock-out of staff | integration |
| E25 | F10 | Suspend a suspended account; reactivate an active one | 409 `Account is already suspended` / `Account is not suspended` | Repeat | integration |
| E26 | F10 | Reactivate a suspended guest whose bookings were cancelled by the cascade | Status ACTIVE; the cancelled bookings and deactivated listings stay as they are; the suspension audited the account and each cancellation | Reactivation is not an undo | integration |
| E27 | F10 | Suspend or reactivate an unknown user | 404 `NOT_FOUND` `User does not exist` | Typed not-found | integration |
| E29 | F10, F16 | A suspended guest's next call to a guest route | 403 `PROFILE_INACTIVE` | Suspension takes effect at once | integration |
| E35 | F12 | Create or rename to an existing label, differing only in case or surrounding spaces | 400 `INVALID_REQUEST` `Category label already exists` | Unique labels | integration |
| E37 | F12 | Delete a category a ticket uses | 409 `Category is in use by tickets; deactivate it instead`; category unchanged | Ticket categories are label text (D8) | integration |
| E38 | F12 | Delete, rename or toggle an unknown category | 404 `NOT_FOUND` | Typed not-found | integration |
| E39 | F8 | Post to a resolved ticket | 409 `Ticket is resolved`; nothing stored | Resolved threads are read-only | integration |
| E52 | F3 | assignee MINE for an agent with no tickets, while 3 OPEN tickets exist | 200 with `items: []` and `unassignedCount` 3 | Empty state; the count is global | integration |
| E53 | F4, F7 | Resolve a ticket whose booking is not CONFIRMED or whose escrow is released | 409 `Booking is not confirmed` or `Escrow is not held for this booking`; detail still returns 200 with `escrowHeld: false` | Do not hide history; UI disables the actions | integration |
| E54 | F12 | Rename a category to its own current label | 200, unchanged label | Idempotent rename | integration |
| E70 | F10 | Suspend a host with two listings and a confirmed future booking on one; a guest whose stay starts today | Both listings INACTIVE; the future booking cancelled and refunded; the stay starting today is not cancelled | Cascade scope | integration |

## Failure and volume
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E40 | F2 | Every message an agent-reachable service can throw | None contains a UUID, an email, `SQL` or `Exception`; the test enumerates the throw sites by calling each failing path | Messages go to the browser | integration |
| E41 | F2 | A spy makes a repository throw `DataAccessException` | 500 `INTERNAL_ERROR` `Something went wrong`; stack trace in the log only | No internal leak | integration |
| E42 | F2 | `PUT` to a `GET`-only route | 405 `METHOD_NOT_ALLOWED` | Stable errors | integration |
| E43 | F2 | A JSON endpoint called with `text/plain` | 415 `UNSUPPORTED_MEDIA_TYPE` | Stable errors | integration |
| E55 | N4 | A request with a known test token, a reason and a note fails with a forced database error carrying the reason as a bound value | The Logback `ListAppender` holds no line with the token, the reason, the note or the bound value | Privacy | integration |
| E56 | N3 | Each mutating endpoint succeeds | The audit rows of its service exist | Audit is service-owned | integration |
| E57 | N5 | 300 tickets with bookings, 300 accounts, 2,000 audit rows | Queue, accounts and an audit page are timed over 20 calls; p95 is logged; the test fails above 5 s | Usable lists | integration |
| E58 | N6, N2, N3 | A fixture controller breaks each ArchUnit rule | `ApiLayerTest` reports the break | Layering | unit |
| E34 | F11 | 450 rows, some sharing one timestamp, read in pages of 200 | No row repeated or missing across the pages when no row is added; order is timestamp descending, then id descending | Stable paging | integration |
| E44 | F9 | The database holds Guests, Hosts, an Agent and the System user | `GET /accounts` returns only the Guests and Hosts | No governance of staff | integration |
| E63 | F2 | `GET /api/agent/nope` by an agent; `GET /actuator/health`; an existing auth call | 404 `NOT_FOUND` `Not found` in the error shape; the health body stays exactly `{"status":"UP"}`; auth responses unchanged | The shared handler must not disturb other routes | integration |
| E67 | F2 | A success response and an error response from any agent route | Both carry `Cache-Control: no-store` | No cached privileged data | integration |
| E68 | F7, F10, N2 | A spy makes `LedgerWriter` fail after the guest credit during resolve, and the guest wallet lookup fail during a suspension cascade | The request returns 500; the ticket, booking, wallets, listings and audit rows are unchanged | All-or-nothing | integration |
| E71 | F11 | `GET /audit/actions` | 200 with every `AuditAction` name, including `BOOKING_FORCE_CANCELLED` | The cascade writes that action | integration |
| E74 | F10, N2 | Two threads (two agents, committed transactions) suspend the same guest holding a confirmed future booking | Exactly one 200; the other 409 `Account is already suspended`; the booking is refunded once; one `ESCROW_REFUND` row for it | Double refund creates money; needs the user row lock (SD19) | integration |
| E75 | F2, N4 | `actions=NOT_AN_ACTION`, a malformed UUID, and a Hibernate or JDK `IllegalStateException` forced by a spy | 400 `VALIDATION_ERROR` `Request is not valid`; 400 `VALIDATION_ERROR`; 500 `INTERNAL_ERROR` `Something went wrong`; no body contains a class name, enum value or input | Framework text must not reach the browser | integration |
| E76 | F5, F7 | A spy holds the ticket lock for longer than the 5 s timeout while another request arrives | The waiting request returns 409 `CONFLICT` `Another change is in progress. Try again.` and changes nothing | A stuck transaction must not hold connections forever | integration |
| E77 | F7 | REJECT with `guestRefund` 50; ACCEPT on a FULL_REFUND ticket with `guestRefund` 5; ACCEPT on a HOST_PAYOUT ticket with no refund; REJECT with `guestRefund` -1 | The first settles with refund 0 (RESOLVED_REJECTED); the second refunds the full amount (RESOLVED_APPROVED); the third pays the host fully (RESOLVED_APPROVED); the fourth is 400 `VALIDATION_ERROR` (constraints run first) | The ignore rule is explicit | integration |
| E78 | F7 | Resolve with a blank or missing reason | 400 `VALIDATION_ERROR`; no wallet change | Every resolution needs a recorded reason | integration |
| E79 | F12 | `PUT /categories/{id}/active` with `{}` or `{"active":null}` | 400 `VALIDATION_ERROR` | Required field | integration |
| E80 | F3, F11 | `GET /disputes?foo=1` and `GET /audit?bar=2` | 200; unknown query parameters are ignored | Documented behaviour | integration |
| E81 | N1, F2 | An anonymous request with a 70,000-byte body; a valid agent with the same body | 401 `INVALID_TOKEN`; 413 `PAYLOAD_TOO_LARGE` | Authentication is decided before size | integration |
| E82 | F11 | `text` containing `%` and `_` | Treated as literal characters of the search text, not as wildcards, and not as an error | Search accuracy | integration |

## Time
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E31 | F11 | An entry at 23:30 UTC on 21 Oct 2026 (07:30 on 22 Oct in Singapore), `from=to=2026-10-22` | Included (the audit zone is `Asia/Singapore`, pending Q1); with `from=to=2026-10-21` it is excluded | Day boundaries | integration |

## UI
| ID | Requirement | Scenario | Expected behaviour (exact) | Why it matters | Test layer |
|---|---|---|---|---|---|
| E36 | F8, F14, F15 | A reason, note, message, label or audit reason containing `<img src=x onerror=alert(1)>` | Shown as literal text on every page; no element is created | Stored XSS from one agent to another | UI |
| E45 | F16 | Any agent call returns 403 `PROFILE_INACTIVE`, or 401 on a mutation with a dialog open | The page body is replaced by the session notice; Sign out works | No stale privileged data | UI |
| E46 | N8 | The tab is hidden for 60 s, then shown; two overlapping requests where the older answers last | No fetch while hidden; one fetch on becoming visible; the older answer is dropped | Free-tier backend; correctness | UI |
| E47 | F7, F14 | Double click on Confirm | One request; the button is disabled while pending | Escrow settled once | UI |
| E48 | F14 | Another agent resolves the ticket while this agent's dialog is open | The dialog shows the 409 message, stays open, and the detail refetches to the resolved view | Stale view | UI |
| E49 | F15 | Load more, then change a filter and Apply | Pages append in order; Apply resets to offset 0 and clears the list first | Correct paging | UI |
| E50 | F15, F3 | Each page with an empty list | The empty-state copy of ui.md | States | UI |
| E59 | N7 | Open a dialog, press Escape, Tab around | `role="dialog"`, `aria-modal`, an accessible name; focus stays inside; Escape closes and returns focus to the opener; a table has `<th scope="col">` and a switch has an `aria-label` | Keyboard use | UI |
| E60 | F13 | Visit `/agent`; click each tab; reload `/agent/disputes/{id}`; look for the Wallet pill | Redirect to `/agent/disputes`; each tab sets `aria-current`; the detail route keeps Disputes current; no Wallet pill for the agent | Navigation | UI |
| E61 | F14 | A poll returns while the notes box holds unsaved text | The note text is kept; other fields update | No lost work | UI |
| E64 | F7, F14 | The resolve request fails with 500 or a network error after the server committed | The dialog shows the error, the detail is refetched, and a resolved ticket shows the resolved view | Unknown outcome must not invite a retry | UI |
| E72 | F14 | A ticket with the escrow released, or an IN_REVIEW ticket assigned to another agent | Accept, Reject and Manual are disabled with the notes under the action row | Prevents known 409s | UI |
| E73 | F14 | Accept dialog for a PARTIAL_REFUND ticket with refund `0`, `-1`, `abc`, `1.234` | Confirm stays disabled with a format message; a larger-than-escrow amount is sent and the server's 400 is shown inline | No client money comparison | UI |

## What could corrupt data
- Double settlement of one escrow: the ticket row is locked as the first read of the mutation (SD10); proven by E10, E20 and E76 with two real threads, and the button is disabled while pending (E47).
- Double refund by a concurrent suspension: the user row is locked first (SD19, E74).
- Acting as another agent: the actor is never read from a request (E4, E62, E69).
- Partial cascade on suspension or partial settlement: each is one service transaction (existing) and E68 forces a failure to prove nothing persists.
- A controller writing audit or wallet rows: forbidden by the layering test (E58).
- Over-refund: rejected by `SettlementCalculator` and by the request constraints (E23).
