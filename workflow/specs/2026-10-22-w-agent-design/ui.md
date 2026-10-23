# UI

Visual source: the design artifact boards named in the index. Behaviour source: the desktop screens (SD2-SD5). Tokens are the Fall Light variables already in `frontend/src/app/globals.css`; no new colour is introduced. Every page renders inside the existing `RouteGuard role="AGENT"` and `RoleShell`. Money reads `SGD 480.00`. Dates read `d MMM yyyy, h:mm a` in the browser's time zone. User-written text (reasons, notes, messages, labels, titles) is rendered only as React text nodes.

Visible to roles: AGENT only for every screen below.

## Navigation and shell
| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Populated | The workspace navbar with four tab links; the tab for the current route has `aria-current="page"` and the accent underline | Tabs link to `/agent/disputes`, `/agent/accounts`, `/agent/audit`, `/agent/categories`; the wordmark links to `/agent`, which replaces itself with `/agent/disputes`; Log out as today; the Wallet pill is not rendered for agents | Disputes, Accounts, Audit Log, Categories | AGENT |
| Session failure | A centred notice replaces the page body; no stale table | Sign out button (calls `signOut`) | 401: "Your session has ended. Sign in again." `PROFILE_INACTIVE`: "Your account is suspended." `PROFILE_NOT_REGISTERED`: "Your profile is not set up." `ROLE_MISMATCH`: "This area is for support agents." | AGENT |

Navigation: `RoleNavbar` gives only the AGENT entries their own `href`s and active state (`usePathname`); the GUEST and HOST entries are unchanged. `/agent/disputes/{ticketId}` keeps the Disputes tab current.

## Disputes queue (`/agent/disputes`)
| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading | Title and filter chips; table body shows one row of text | chips disabled | "Loading disputes..." | AGENT |
| Empty | Table header and a single empty row | filter chips | "No disputes match this filter" | AGENT |
| Error | Banner above the table | Retry button | the error message from the API, or "Could not load disputes" | AGENT |
| Populated | Title "Dispute queue", a red pill "N unassigned" from `unassignedCount` (always the global count) when it is above 0, chips All / Unassigned / Mine (the current one filled), the table lists the oldest dispute first and has columns Ticket (`#label`), Subject (title), Booking (listing title), Guest / Host (`guestName / hostName`), Assigned (`assignedAgentName` or an em dash), Status pill (OPEN "Unassigned", IN_REVIEW "In review", RESOLVED_APPROVED "Resolved - approved", RESOLVED_REJECTED "Resolved - rejected") | Each row is a link to the dispute; chips set `assignee` and reload | as listed | AGENT |

Polling: 30 s while visible (N8). Default chip All.

## Dispute detail (`/agent/disputes/{ticketId}`)
| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading | Breadcrumb and empty panels | none | "Loading dispute..." | AGENT |
| Not found / error | Banner | Back to Disputes link, Retry | "Ticket does not exist" for 404, else the API message | AGENT |
| Unassigned (OPEN) | Breadcrumb `Disputes > #label title`, status pill, summary card, both thread panels (read-only reading allowed), notes box disabled | Assign to me (primary); Send, Save, Accept, Reject, Manual adjustment disabled | "Assign this ticket to reply or resolve." shown under the action row | AGENT |
| In review, mine | Same, everything enabled while the escrow is held; when `escrowHeld` is false the three resolution buttons are disabled with the note "Escrow already released." | Assign to me hidden; Send per thread; Save notes; Accept - remedy guest, Reject dispute, Manual adjustment... | placeholders "Reply to guest...", "Reply to host...", "Add investigation notes..." | AGENT |
| In review, other agent | Everything readable; Send is enabled (any agent may reply while open); Save and the three resolution buttons disabled | none extra | "Assigned to <name>." under the action row | AGENT |
| Resolved | Summary card, threads read-only, notes read-only, a resolution banner | no Send, no Save, no actions | "Ticket is resolved" over each composer; banner shows status and `resolutionReason` | AGENT |

"Mine" means `ticket.assignedAgentId` equals the `id` of the signed-in profile (`AuthProfile.id`, added in T7). Resolution buttons need all of: status IN_REVIEW, assigned to me, escrow held.

Summary card: listing title, `d MMM - d MMM` stay dates, Guest name, Host name, "Amount held in escrow" (`escrowAmount` with `escrowHeld`, else "Released"), "Booking status" showing `phaseLabel`. Guest panel header "Guest messages - <guestName>" with an "Opened ticket" badge; its first item is the opening card (title, description, requested remedy label, supporting text, `createdAt`), then the thread. Host panel header "Host messages - <hostName>". Messages from AGENT are right-aligned and tinted; others left-aligned. Notes box label "Internal notes (invisible to guest or host)". Threads and detail poll every 10 s while visible and the tab is not editing the notes box (a poll never overwrites unsaved note text).

### Dialogs (focus-trapped, Escape and Cancel close, reason required)
| Dialog | Fields | Confirm button and request |
|---|---|---|
| Accept - remedy guest | Requested remedy summary. Shown only when the remedy is PARTIAL_REFUND or OTHER: "Guest refund (SGD)", required, a positive number with at most 2 decimals (format check only; the server enforces the escrow limit and its message shows inline). FULL_REFUND shows "Full refund SGD <escrow>"; HOST_PAYOUT shows "Full payout to host". Reason | "Confirm accept" sends `{mode: ACCEPT, guestRefund (only when asked), reason}` |
| Reject dispute | Reason | "Confirm reject" sends `{mode: REJECT, guestRefund: null, reason}` |
| Manual wallet adjustment | Override type: Full refund to guest (refund = `escrowAmount`, locked), Full payout to host (refund 0.00, locked), Custom (editable, a number of 0 or more with at most 2 decimals; the server enforces the escrow limit); "Guest refund (SGD)"; Reason | "Confirm" sends `{mode: MANUAL, guestRefund, reason}` |

The dialog shows the API error inline and stays open on failure; on success it closes and the detail is replaced by the response. Confirm is disabled while a request is pending. After any failed resolve (409, 500 or network error) the page refetches the detail, because the outcome may be unknown; if the refetched ticket is resolved the dialog closes and the resolved view shows.

## Accounts (`/agent/accounts`)
| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading / Error | as Disputes queue | Retry | "Loading accounts..." / "Could not load accounts" | AGENT |
| Empty | Header and one row | search | "No accounts match your search" | AGENT |
| Populated | Title "Account governance", subtitle "Suspend or reactivate Guest and Host accounts", search box (placeholder "Search...", case-insensitive substring of display name or email, in the browser), table Display name, Email, Joined (`MMM yyyy`), Role (Guest, Host), Status pill (Active green, Suspended red) with "Reason: <suspensionReason>" under a suspended one | Suspend (red outline) on ACTIVE rows, Reactivate on SUSPENDED rows; each opens a reason dialog "Suspend account" / "Reactivate account" naming the user, buttons Cancel and Confirm | Suspend warns "Pending and upcoming bookings will be cancelled and refunded." | AGENT |

## Audit Log (`/agent/audit`)
| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading / Error | as above | Retry | "Loading audit log..." / "Could not load the audit log" | AGENT |
| Empty | filter card and one row | Clear | "No audit entries match" | AGENT |
| Populated | Title "Audit log"; filter card with Search (placeholder "User name, or booking / ticket / user id"), Action type (All action types plus the names from `GET /audit/actions`, so the list always matches the backend enum), From, To, Apply filters, Clear; table Timestamp, Actor, Action type (pill), Target, Status (`beforeState -> afterState`, or the single value), Reason, Amount (`walletAdjustment` signed, SGD) | Apply reloads from offset 0; Clear resets and reloads; "Load more" appears only when the last page held 200 rows and appends the next page | Target: `subjectName`, else `entityType` and the first 8 characters of `entityId` | AGENT |

No polling. An invalid range (From after To) disables Apply and shows "From must not be after To".

## Categories (`/agent/categories`)
| State | What the user sees | Controls | Copy | Visible to roles |
|---|---|---|---|---|
| Loading / Error | as above | Retry | "Loading categories..." / "Could not load categories" | AGENT |
| Empty | header and one row | + Add category | "No categories yet" | AGENT |
| Populated | Title "Ticket categories", subtitle "Shown to guests when filing a dispute ticket", table Label, Active (switch with `aria-label` "Toggle active: <label>"), Action (Edit) | + Add category opens the dialog "Add category"; Edit opens "Edit category" with Label, Save, Cancel, Delete (red) | Delete confirms inline "Delete <label>? This cannot be undone." and on a 409 shows the API message | AGENT |

A toggle applies at once, shows the new state on success and reverts with an inline error on failure.
